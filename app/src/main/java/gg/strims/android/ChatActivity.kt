package gg.strims.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.beust.klaxon.Klaxon
import gg.strims.android.items.ChatMessage
import gg.strims.android.items.PrivateChatMessage
import gg.strims.android.models.ChatUser
import gg.strims.android.models.Message
import gg.strims.android.models.Options
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import java.io.*
import java.lang.StringBuilder
import java.net.URL

@KtorExperimentalAPI
class ChatActivity : AppCompatActivity() {

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOT_USER_KEY = "NOT_USER_KEY"
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        GlobalScope.launch {
            WSClient().onConnect()
        }

        showHideFragment(supportFragmentManager.findFragmentById(R.id.login_fragment)!!)
        showHideFragment(supportFragmentManager.findFragmentById(R.id.profile_fragment)!!)
        showHideFragment(supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
        showHideFragment(supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!)

        sendMessageText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
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
                showHideFragment(fragment)
            }
            showHideFragment(supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
        }

        userListButton.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.options_fragment)
            if (!fragment!!.isHidden) {
                showHideFragment(fragment)
            }
            showHideFragment(supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!)
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
                showHideFragment(supportFragmentManager.findFragmentById(R.id.login_fragment)!!)
            }
            R.id.chatProfile -> {
                showHideFragment(supportFragmentManager.findFragmentById(R.id.profile_fragment)!!)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showHideFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

        if (fragment.isHidden) {
            fragmentTransaction.show(fragment)
        } else if (!fragment.isHidden) {
            fragmentTransaction.hide(fragment)
        }

        fragmentTransaction.commit()
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

    inner class WSClient {

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

        @SuppressLint("SetTextI18n")
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
                            send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"${sendMessageText.text.toString()
                                .substringAfter("/w $nick ")}\"}")
                            runOnUiThread {
                                adapter.add(
                                    PrivateChatMessage(
                                        Message(
                                            false,
                                            CurrentUser.user!!.username,
                                            sendMessageText.text.toString(),
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
            }
            return null
        }
    }
}
