package gg.strims.mobile

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
import android.widget.CheckBox
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_message.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.beust.klaxon.Klaxon
import com.google.gson.Gson
import gg.strims.mobile.models.ChatUser
import gg.strims.mobile.models.Message
import gg.strims.mobile.models.Options
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.client.request.*
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.android.synthetic.main.activity_chat_options.*
import kotlinx.android.synthetic.main.activity_user_list.*
import kotlinx.android.synthetic.main.chat_user_row.view.*
import kotlinx.android.synthetic.main.private_chat_message.view.*
import java.io.*
import java.lang.Exception
import java.lang.StringBuilder
import java.net.URL
import java.util.*

@KtorExperimentalAPI
class ChatActivity : AppCompatActivity() {

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOT_USER_KEY = "NOT_USER_KEY"
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        GlobalScope.launch {
            WSClient().onConnect()
        }

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
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                sendMessageButton.performClick()
                return@OnKeyListener true
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
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

            if (fragment != null) {
                if (!fragment.isHidden) {
                    fragmentTransaction.hide(fragment)
                }

                fragmentTransaction.commit()
            }

            showHideFragment(supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
        }

        userListButton.setOnClickListener {
            val fragment = supportFragmentManager.findFragmentById(R.id.options_fragment)
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)

            if (fragment != null) {
                if (!fragment.isHidden) {
                    fragmentTransaction.hide(fragment)
                }

                fragmentTransaction.commit()
            }

            showHideFragment(supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!)
        }
    }

    class UserListFragment : Fragment() {
        private val userListAdapter = GroupAdapter<GroupieViewHolder>()

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.activity_user_list, container, false)
        }

        override fun onHiddenChanged(hidden: Boolean) {
            userListAdapter.clear()
            if (CurrentUser.users != null) {
                CurrentUser.users!!.sortBy { it.nick }
                    CurrentUser.users!!.forEach {
                    userListAdapter.add(UserListItem(it))
                }
                recyclerViewUserList.scrollToPosition(1)
            }

            userListSearch.addTextChangedListener(object: TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    userListAdapter.clear()
                    val list = mutableListOf<ChatUser>()
                    CurrentUser.users!!.sortBy { it.nick }
                    CurrentUser.users!!.forEach {
                        userListAdapter.add(UserListItem(it))
                    }
                    recyclerViewUserList.scrollToPosition(1)
                    for (i in 0 until userListAdapter.itemCount) {
                        val item = userListAdapter.getItem(i) as UserListItem
                        if (item.user.nick.contains(userListSearch.text.toString())) {
                            list.add(item.user)
                        }
                    }
                    userListAdapter.clear()
                    list.forEach {
                        userListAdapter.add(UserListItem(it))
                    }
                }
            })
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val layoutManager = LinearLayoutManager(view.context)
            layoutManager.stackFromEnd = true
            recyclerViewUserList.layoutManager = layoutManager
            recyclerViewUserList.adapter = userListAdapter
        }

        inner class UserListItem(val user: ChatUser) : Item<GroupieViewHolder>() {
            override fun getLayout(): Int {
                return R.layout.chat_user_row
            }

            @SuppressLint("SetTextI18n")
            override fun bind(viewHolder: GroupieViewHolder, position: Int) {
                viewHolder.itemView.chatUserUsername.text = user.nick
                if (user.features.contains("bot")) {
                    viewHolder.itemView.chatUserUsername.setTextColor(Color.parseColor("#FF2196F3"))
                } else {
                    viewHolder.itemView.chatUserUsername.setTextColor(Color.parseColor("#FFFFFF"))
                }

                viewHolder.itemView.chatUserUsername.setOnClickListener {
                    activity!!.sendMessageText.setText("/w ${user.nick} ")
                    keyRequestFocus(activity!!.sendMessageText, context!!)
                    activity!!.sendMessageText.setSelection(activity!!.sendMessageText.text.length)
                    val fragment = this@UserListFragment
                    val fragmentTransaction = fragmentManager!!.beginTransaction()
                    fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .hide(fragment)

                    fragmentTransaction.commit()
                }
            }
        }
    }

    class OptionsFragment : Fragment() {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.activity_chat_options, container, false)
        }

        override fun onHiddenChanged(hidden: Boolean) {
            if (CurrentUser.options != null) {
                checkBoxTimestamp.isChecked = CurrentUser.options!!.showTime
                checkBoxGreentext.isChecked = CurrentUser.options!!.greentext
                checkBoxHarshIgnore.isChecked = CurrentUser.options!!.harshIgnore
                checkBoxHideNsfw.isChecked = CurrentUser.options!!.hideNsfw
                checkBoxNotifications.isChecked = CurrentUser.options!!.notifications
                checkBoxEmotes.isChecked = CurrentUser.options!!.emotes

                ignoredUsersTextViewOptions.text =
                    CurrentUser.options!!.ignoreList.toString()
                        .substringAfter('[').substringBefore(']')

                customHighlightsTextViewOptions.text =
                    CurrentUser.options!!.customHighlights.toString()
                        .substringAfter('[').substringBefore(']')
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            closeMenuButton.setOnClickListener {
                fragmentManager!!.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .hide(this)
                    .commit()
            }

            saveOptionsButton.setOnClickListener {
                CurrentUser.saveOptions(context!!)
                fragmentManager!!.beginTransaction()
                    .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                    .hide(this)
                    .commit()
            }

            checkBoxTimestamp.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.showTime = isChecked
            }

            checkBoxGreentext.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.greentext = isChecked
            }

            checkBoxHarshIgnore.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.harshIgnore = isChecked
            }

            checkBoxHideNsfw.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.hideNsfw = isChecked
            }

            checkBoxNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.notifications = isChecked
            }

            checkBoxEmotes.setOnCheckedChangeListener { buttonView, isChecked ->
                CurrentUser.options!!.emotes = isChecked
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
                startActivity(Intent(this, LoginActivity::class.java))
            }
            R.id.chatProfile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
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

    inner class ChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.chat_message
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat", "WrongViewCast")
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
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(CurrentUser.user!!.username)) {
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

            if (messageData.features.contains("bot")) {
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
                    if (adapter.getItem(i).layout == R.layout.chat_message) {
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
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.messageData.features.isEmpty()) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            adapterItem?.itemView?.usernameChatMessage?.setTextColor(Color.parseColor("#FFFFFF"))
                        }
                        CurrentUser.tempHighlightNick = null
                    }
                }
            }
        }
    }

    inner class PrivateChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.private_chat_message
        }

        @SuppressLint("SetTextI18n")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.ignoreList.contains(messageData.nick)) {
                return
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

            if (CurrentUser.options!!.greentext) {
                if (messageData.data.first() == '>') {
                    viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#789922"))
                } else {
                    viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }

            viewHolder.itemView.usernamePrivateMessage.text = messageData.nick
            viewHolder.itemView.messagePrivateMessage.text = " whispered: ${messageData.data}"
        }
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
                    adapter.add(ChatMessage(parseMessage(it)!!))
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
                                        adapter.add(PrivateChatMessage(msg))
                                        if (CurrentUser.options!!.notifications) {
                                            displayNotification(msg)
                                        }
                                    } else {
                                        adapter.add(ChatMessage(msg))
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
                    CurrentUser.users!!.add(userJoin!!)
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
