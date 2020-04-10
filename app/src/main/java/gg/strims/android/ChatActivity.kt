package gg.strims.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.webkit.CookieManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.beust.klaxon.Klaxon
import com.xwray.groupie.Item
import gg.strims.android.items.PrivateChatMessage
import gg.strims.android.models.ChatUser
import gg.strims.android.models.Message
import gg.strims.android.models.Options
import gg.strims.android.models.Stream
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.android.synthetic.main.autofill_item.view.*
import kotlinx.android.synthetic.main.chat_message_item.view.*
import java.io.*
import java.lang.StringBuilder
import java.net.URL
import java.util.*

@KtorExperimentalAPI
@SuppressLint("SetTextI18n", "SimpleDateFormat", "WrongViewCast")
class ChatActivity : AppCompatActivity() {

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOT_USER_KEY = "NOT_USER_KEY"
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    val adapter = GroupAdapter<GroupieViewHolder>()

    private val autofillAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        GlobalScope.launch {
            ChatClient().onConnect()
        }

        GlobalScope.launch {
            StrimsClient().onConnect()
        }

        recyclerViewAutofill.adapter = autofillAdapter
        recyclerViewAutofill.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        sendMessageText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
                autofillAdapter.clear()
                if (sendMessageText.text.isNotEmpty()) {
                    recyclerViewAutofill.visibility = View.VISIBLE
                    val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                    CurrentUser.users!!.sortByDescending {
                        it.nick
                    }
                    CurrentUser.users!!.forEach {
                        if (it.nick.contains(currentWord, true)) {
                            autofillAdapter.add(AutofillItem(it))
                        }
                    }
                } else if (sendMessageText.text.isEmpty() || sendMessageText.text.last() == ' ') {
                    recyclerViewAutofill.visibility = View.GONE
                }
            }
        })

        sendMessageText.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (sendMessageText.text.isNotEmpty()) {
                    sendMessageButton.performClick()
                    return@OnKeyListener true
                }
            }
            false
        })

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = adapter

        recyclerViewChat.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
            val lastItem = layoutTest.findLastVisibleItemPosition()
            if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
                goToBottom.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottom.visibility = View.GONE
                goToBottom.isEnabled = false
            }
        }

        goToBottom.setOnClickListener {
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        optionsButton.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.user_list_fragment)
            if (!fragment!!.isHidden) {
                showHideFragment(this, fragment)
            }
            showHideFragment(this, supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
        }

        userListButton.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.options_fragment)
            if (!fragment!!.isHidden) {
                showHideFragment(this, fragment)
            }
            showHideFragment(this, supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!)
        }
    }

    inner class AutofillItem(private val user: ChatUser) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.autofill_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameAutofill.text = user.nick

            viewHolder.itemView.usernameAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage = sendMessageText.text.toString().substringBefore(currentWord)
                sendMessageText.setText("${currentMessage}${user.nick} ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        if (CurrentUser.user != null) {
            menu!!.findItem(R.id.chatLogin).isVisible = false
            menu.findItem(R.id.chatProfile).isVisible = true
            menu.findItem(R.id.chatSignOut).isVisible = true
        } else {
            menu!!.findItem(R.id.chatLogin).isVisible = true
            menu.findItem(R.id.chatProfile).isVisible = false
            menu.findItem(R.id.chatSignOut).isVisible = false
        }
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chatLogin -> {
//                showHideFragment(this, supportFragmentManager.findFragmentById(R.id.login_fragment)!!)
                startActivity(Intent(this, LoginActivity::class.java))
            }
            R.id.chatProfile -> {
                showHideFragment(this, supportFragmentManager.findFragmentById(R.id.profile_fragment)!!)
            }
            R.id.chatHome -> {
                showHideFragment(this, supportFragmentManager.findFragmentById(R.id.home_fragment)!!)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun retrieveOptions() {
        val file = baseContext.getFileStreamPath("filename.txt")
        if (file.exists()) {
            val fileInputStream = openFileInput("filename.txt")
            val inputStreamReader = InputStreamReader(fileInputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder = StringBuilder()
            var text: String? = null
            while ({ text = bufferedReader.readLine(); text }() != null) {
                stringBuilder.append(text)
            }
            CurrentUser.options = Klaxon().parse(stringBuilder.toString())
        } else {
            CurrentUser.options = Options()
        }
        Log.d("TAG", "${CurrentUser.options!!.greentext}, ${CurrentUser.options!!.emotes}")
    }

    private fun displayNotification(message: Message) {
        val pendingIntent = TaskStackBuilder.create(this)
            .addNextIntent(Intent(this, ChatActivity::class.java))
            .getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.nick)
            .setContentText(message.data)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val remoteInput = RemoteInput.Builder(NOTIFICATION_REPLY_KEY).setLabel("Reply").build()

        val replyIntent = Intent(this, ChatActivity::class.java)
            .putExtra(NOT_USER_KEY, message.nick)

        val replyPendingIntent = PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(R.drawable.ic_launcher_foreground, "Reply", replyPendingIntent)
            .addRemoteInput(remoteInput).build()

        notificationBuilder.addAction(action)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class ChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.chat_message_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            CurrentUser.options!!.ignoreList.forEach {
                if (it == messageData.nick) {
                    return
                }

                if (CurrentUser.options!!.harshIgnore) {
                    if (messageData.data.contains(it)) {
                        return
                    }
                }
            }

            if (CurrentUser.options!!.hideNsfw) {
                if ((messageData.data.contains("nsfw") || messageData.data.contains("nsfl")
                            && messageData.data.contains("a link"))) {
                    return
                }
            }

            if (CurrentUser.options!!.greentext) {
                if (messageData.data.first() == '>') {
                    viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#789922"))
                } else {
                    viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }

            if (CurrentUser.options!!.showTime) {
                val date = Date(messageData.timestamp)
                val time = if (date.minutes < 10) {
                    "${date.hours}:0${date.minutes}"
                } else {
                    "${date.hours}:${date.minutes}"
                }
                viewHolder.itemView.timestampChatMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampChatMessage.text = time
            }

            if (CurrentUser.user != null) {
                if (messageData.data.contains(CurrentUser.user!!.username)) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else if (CurrentUser.user!!.username == messageData.nick) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#151515"))
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                        CurrentUser.user!!.username)) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
                }
            } else if (CurrentUser.user == null) {
                if (messageData.data.contains("anonymous")) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
                }
            }

            if (CurrentUser.options!!.customHighlights.isNotEmpty()) {
                CurrentUser.options!!.customHighlights.forEach {
                    if (messageData.nick == it) {
                        viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                    }
                }
            }

            if (messageData.features.contains("bot") || messageData.nick == "Info") {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FF2196F3"))
                viewHolder.itemView.botFlairChatMessage.visibility = View.VISIBLE
            } else {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
                viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
            }

            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(messageData.nick)) {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFF44336"))
            }

            viewHolder.itemView.usernameChatMessage.text = "${messageData.nick}:"
            viewHolder.itemView.messageChatMessage.text = messageData.data

            viewHolder.itemView.usernameChatMessage.setOnClickListener {
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.messageData.nick == messageData.nick) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            adapterItem?.itemView?.usernameChatMessage?.setTextColor(Color.parseColor("#FFF44336"))
                        }
                    }
                }
                if (CurrentUser.tempHighlightNick == null) {
                    CurrentUser.tempHighlightNick = mutableListOf()
                }
                CurrentUser.tempHighlightNick!!.add(messageData.nick)
            }

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.messageData.features.isEmpty() && item.messageData.nick != "Info") {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            adapterItem?.itemView?.usernameChatMessage?.setTextColor(Color.parseColor("#FFFFFF"))
                        }
                    }
                }
            }
        }
    }

    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

        private fun retrieveHistory() {
            val messageHistory = Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
            Log.d("TAG", messageHistory.toString())
            runOnUiThread {
                messageHistory?.forEach {
                    adapter.add(
                        ChatMessage(
                            parseMessage(
                                it
                            )!!
                        )
                    )
                }
            }
        }

        private fun retrieveCookie() {
            val cookies = CookieManager.getInstance().getCookie("https://strims.gg")
            if (cookies != null) {
                Log.d("TAG", "Cookies: $cookies")
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                }
                Log.d("TAG", "JWT: $jwt")
            }
        }

        private suspend fun retrieveProfile() {
            val text: String = client.get("https://strims.gg/api/profile") {
                header("Cookie", "jwt=$jwt")
            }
            Log.d("TAG", "Profile: $text")
            GlobalScope.launch {
                runOnUiThread {
                    CurrentUser.user = Klaxon().parse(text)
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    invalidateOptionsMenu()
                }
            }
        }

        suspend fun onConnect() = client.wss(
            host = "chat.strims.gg",
            path = "/ws",
            request = {
                retrieveCookie()
                if (jwt != null) {
                    Log.d("TAG", "Requesting with JWT: $jwt")
                    header("Cookie", "jwt=$jwt")
                }
            }
        ){
            if (jwt != null) {
                retrieveProfile()
            }
            retrieveOptions()
            retrieveHistory()
            sendMessageButton.setOnClickListener {
                GlobalScope.launch {
                    val messageText = sendMessageText.text.toString()
                    val first = messageText.first()
                    if (first == '/') {
                        if (messageText.substringAfter(first).substringBefore(' ') == "w") {
                            val nick = messageText.substringAfter("/w ").substringBefore(' ')
                            val message = messageText.substringAfter("/w $nick ")
                            send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")
                            runOnUiThread {
                                adapter.add(
                                    PrivateChatMessage(
                                        Message(
                                            false,
                                            "To: $nick",
                                            message,
                                            System.currentTimeMillis(),
                                            arrayOf()
                                        )
                                    )
                                )
                                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "ignore") {
                            val nickIgnore = messageText.substringAfter("/ignore ").substringBefore(' ')
                            CurrentUser.options!!.ignoreList.add(nickIgnore)
                            CurrentUser.saveOptions(this@ChatActivity)
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "unignore") {
                            val nickUnignore = messageText.substringAfter("/unignore ").substringBefore(' ')
                            if (CurrentUser.options!!.ignoreList.contains(nickUnignore)) {
                                CurrentUser.options!!.ignoreList.remove(nickUnignore)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "Unignored: $nickUnignore",
                                                System.currentTimeMillis(),
                                                arrayOf()
                                            )
                                        )
                                    )
                                }
                            } else {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User not currently ignored",
                                                System.currentTimeMillis(),
                                                arrayOf()
                                            )
                                        )
                                    )
                                }
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "highlight") {
                            val nickHighlight = messageText.substringAfter("/highlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickHighlight)) {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User already highlighted",
                                                System.currentTimeMillis(),
                                                arrayOf()
                                            )
                                        )
                                    )
                                    recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                                }
                            } else {
                                CurrentUser.options!!.customHighlights.add(nickHighlight)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "Highlighting user: $nickHighlight",
                                                System.currentTimeMillis(),
                                                arrayOf()
                                            )
                                        )
                                    )
                                    recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "unhighlight") {
                            val nickUnhighlight = messageText.substringAfter("/unhighlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickUnhighlight)) {
                                CurrentUser.options!!.customHighlights.remove(nickUnhighlight)
                            } else {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User not currently highlighted",
                                                System.currentTimeMillis(),
                                                arrayOf()
                                            )
                                        )
                                    )
                                    recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                                }
                            }
                        } else {
                            runOnUiThread {
                                adapter.add(
                                    ChatMessage(
                                        Message(
                                            false,
                                            "Info",
                                            "Invalid command",
                                            System.currentTimeMillis(),
                                            arrayOf()
                                        )
                                    )
                                )
                                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                            }
                        }
                    } else {
                        send("MSG {\"data\":\"${sendMessageText.text}\"}")
                    }
                    runOnUiThread {
                        sendMessageText.text.clear()
                    }
                }
            }
            if (intent.getStringExtra(NOT_USER_KEY) != null) {
                val remoteReply = RemoteInput.getResultsFromIntent(intent)

                if (remoteReply != null) {
                    val message = remoteReply.getCharSequence(NOTIFICATION_REPLY_KEY) as String
                    val nick = intent.getStringExtra(NOT_USER_KEY)
                    send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")

                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(NOTIFICATION_ID)
                }
            }
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        println(frame.readText())
                        val msg: Message? = parseMessage(frame.readText())
                        if (msg != null) {
                            if (!CurrentUser.options!!.ignoreList.contains(msg.nick)) {
                                runOnUiThread {
                                    if (msg.privMsg) {
                                        adapter.add(
                                            PrivateChatMessage(
                                                msg
                                            )
                                        )
                                        if (CurrentUser.options!!.notifications) {
                                            displayNotification(msg)
                                        }
                                    } else {
                                        adapter.add(
                                            ChatMessage(
                                                msg
                                            )
                                        )
                                    }
                                    val layoutTest =
                                        recyclerViewChat.layoutManager as LinearLayoutManager
                                    val lastItem = layoutTest.findLastVisibleItemPosition()
                                    if (lastItem >= recyclerViewChat.adapter!!.itemCount - 3) {
                                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                                    }
                                }
                            }
                        }
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }

        private fun parseMessage(input: String): Message? {
            val msg = input.split(" ", limit = 2)
            when (msg[0]) {
                "NAMES" -> {
                    Log.d("TAG", "Names: ${msg[1]}")
                    val users: List<ChatUser>? = Klaxon().parseArray(msg[1].substringAfter("\"users\":").substringBefore(",\"connectioncount\":"))
                    CurrentUser.users = users?.toMutableList()
                    CurrentUser.connectionCount = msg[1].substringAfter("\"connectioncount\":").substringBefore('}').toInt()
                    Log.d("TAG", CurrentUser.connectionCount.toString())
                    Log.d("TAG", CurrentUser.users!![0].nick)
                    runOnUiThread {
                        adapter.add(
                            ChatMessage(
                                Message(
                                    false,
                                    "Info",
                                    "Connected users: ${CurrentUser.connectionCount}",
                                    System.currentTimeMillis(),
                                    arrayOf()
                                )
                            )
                        )
                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                    }
                }
                "JOIN" -> {
                    val userJoin = Klaxon().parse<ChatUser>(msg[1])
                    if (!CurrentUser.users!!.contains(userJoin)) {
                        CurrentUser.users!!.add(userJoin!!)
                    }
                }
                "QUIT" -> {
                    val userQuit = Klaxon().parse<ChatUser>(msg[1])
                    if (CurrentUser.users!!.contains(userQuit)) {
                        CurrentUser.users!!.remove(userQuit)
                    }
                }
                "PRIVMSG" -> {
                    val message = Klaxon().parse<Message>(msg[1])!!
                    message.privMsg = true
                    return message
                }
                "MSG" -> {
                    return Klaxon().parse<Message>(msg[1])
                }
                "MUTE" -> {
                    val message = Klaxon().parse<Message>(msg[1])
                    message!!.data = message.data.plus(" muted by Bot.")
                    return message
                }
            }
            return null
        }
    }

    inner class StrimsClient {

        private val client = HttpClient {
            install(WebSockets)
        }

        suspend fun onConnect() = client.wss(
            host = "strims.gg",
            path = "/ws"
        ){
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        println(frame.readText())
                        parseStream(frame.readText())
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }

        private fun parseStream(input: String) {
            if (input.substringAfter("[\"").substringBefore("\"") == "STREAMS_SET") {
                val msg = input.substringAfter("\",").substringBeforeLast(']')
                val streams: List<Stream>? = Klaxon().parseArray(msg)
                CurrentUser.streams = streams?.toMutableList()
            } else if (input.substringAfter("[\"").substringBefore("\"") == "RUSTLERS_SET") {
                val id = input.substringAfter("\"RUSTLERS_SET\",").substringBefore(",").toLong()
                if (CurrentUser.streams != null) {
                    CurrentUser.streams!!.forEach {
                        if (it.id == id) {
                            val newRustlers = input.substringAfter("$id,").substringBefore(",").toInt()
                            val newAfk = input.substringAfter("$id,$newRustlers,").substringBefore("]").toInt()
                            it.rustlers = newRustlers
                            it.afk_rustlers = newAfk
                            return
                        }
                    }
                }
            }
        }
    }
}
