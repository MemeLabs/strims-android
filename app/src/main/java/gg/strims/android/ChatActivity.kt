package gg.strims.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.Log
import android.util.LruCache
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.app.TaskStackBuilder
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.Klaxon
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.models.*
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.autofill_item.view.*
import kotlinx.android.synthetic.main.chat_message_item.view.*
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

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

    private lateinit var bitmapMemoryCache: LruCache<String, Bitmap>

    private lateinit var gifMemoryCache: LruCache<String, Drawable>

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

        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        bitmapMemoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
        gifMemoryCache = object : LruCache<String, Drawable>(cacheSize) {}

        supportActionBar!!.hide()

        chatBottomNavigationView.selectedItemId =
            chatBottomNavigationView.menu.findItem(R.id.chatChat).itemId

        chatBottomNavigationView.setOnNavigationItemSelectedListener {
            hideKeyboardFrom(this, sendMessageText)
            when (it.itemId) {
                R.id.chatChat -> {
                    hideFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.profile_fragment)!!
                    )
                    hideFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.streams_fragment)!!
                    )
                    hideFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.login_fragment)!!
                    )
                }
                R.id.chatLogin -> {
                    goToBottom.visibility = View.GONE
                    showFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.login_fragment)!!
                    )
                }
                R.id.chatProfile -> {
                    goToBottom.visibility = View.GONE
                    if (supportFragmentManager.findFragmentById(R.id.streams_fragment)!!.isVisible) {
                        hideFragment(
                            this,
                            supportFragmentManager.findFragmentById(R.id.streams_fragment)!!
                        )
                    }
                    showFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.profile_fragment)!!
                    )
                }
                R.id.chatStreams -> {
                    goToBottom.visibility = View.GONE
                    if (supportFragmentManager.findFragmentById(R.id.profile_fragment)!!.isVisible) {
                        hideFragment(
                            this,
                            supportFragmentManager.findFragmentById(R.id.profile_fragment)!!
                        )
                    }
                    showFragment(
                        this,
                        supportFragmentManager.findFragmentById(R.id.streams_fragment)!!
                    )
                }
            }
            true
        }

        sendMessageText.addTextChangedListener(object : TextWatcher {
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
                            autofillAdapter.add(AutofillItemUser(it))
                        }
                    }
                    CurrentUser.emotes!!.forEach {
                        if (it.name.contains(currentWord, true)) {
                            autofillAdapter.add(AutofillItemEmote(it))
                        }
                    }
                } else if (sendMessageText.text.isEmpty() || sendMessageText.text.last() == ' ') {
                    recyclerViewAutofill.visibility = View.GONE
                }
            }
        })

        sendMessageText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessageButton.performClick()
            }
            true
        }

        sendMessageText.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                sendMessageButton.performClick()
            }
            false
        }

        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = layoutManager
        recyclerViewChat.adapter = adapter

        recyclerViewChat.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
            val lastItem = layoutTest.findLastVisibleItemPosition()
            if (lastItem < recyclerViewChat.adapter!!.itemCount - 1
                && (supportFragmentManager.findFragmentById(R.id.profile_fragment)!!.isHidden
                        && supportFragmentManager.findFragmentById(R.id.streams_fragment)!!.isHidden
                        && supportFragmentManager.findFragmentById(R.id.options_fragment)!!.isHidden
                        && supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isHidden
                        && supportFragmentManager.findFragmentById(R.id.login_fragment)!!.isHidden)
            ) {
                goToBottom.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottom.visibility = View.GONE
                goToBottom.isEnabled = false
            }
        }

        recyclerViewChat.itemAnimator = null

        recyclerViewAutofill.adapter = autofillAdapter
        recyclerViewAutofill.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        goToBottom.setOnClickListener {
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        optionsButton.setOnClickListener {
            goToBottom.visibility = View.GONE
            hideKeyboardFrom(this, sendMessageText)
            val fragment = supportFragmentManager.findFragmentById(R.id.user_list_fragment)
            if (!fragment!!.isHidden) {
                showHideFragment(this, fragment)
            }
            showHideFragment(this, supportFragmentManager.findFragmentById(R.id.options_fragment)!!)
        }

        userListButton.setOnClickListener {
            goToBottom.visibility = View.GONE
            hideKeyboardFrom(this, sendMessageText)
            val fragment = supportFragmentManager.findFragmentById(R.id.options_fragment)
            if (!fragment!!.isHidden) {
                showHideFragment(this, fragment)
            }
            showHideFragment(
                this,
                supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!
            )
        }
    }

    inner class AutofillItemUser(private val user: ChatUser) : Item<GroupieViewHolder>() {
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

    inner class AutofillItemEmote(private val emote: Emote) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.autofill_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.usernameAutofill.text = emote.name
            viewHolder.itemView.usernameAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage = sendMessageText.text.toString().substringBefore(currentWord)
                sendMessageText.setText("${currentMessage}${emote.name} ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
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

        val replyPendingIntent =
            PendingIntent.getActivity(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_foreground,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput).build()

        notificationBuilder.addAction(action)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(channelId, "Chat Messages", NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(channel)
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    inner class ChatMessage(
        private val messageData: Message,
        private val isConsecutive: Boolean = false
    ) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            if (isConsecutive) {
                return R.layout.chat_message_item_consecutive_nick
            }
            return R.layout.chat_message_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(messageData.timestamp)
                viewHolder.itemView.timestampChatMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampChatMessage.text = time
            }

            if (CurrentUser.user != null) {
                if (messageData.data.contains(CurrentUser.user!!.username)) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                } else if (CurrentUser.user!!.username == messageData.nick) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#151515"))
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                        CurrentUser.user!!.username
                    )
                ) {
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

            if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                    messageData.nick
                )
            ) {
                viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFF44336"))
            }

            viewHolder.itemView.usernameChatMessage.text = "${messageData.nick}:"

            val ssb = SpannableStringBuilder(messageData.data)

            if (CurrentUser.options!!.emotes) {
                if (messageData.entities.emotes != null && messageData.entities.emotes!!.isNotEmpty() && messageData.entities.emotes!![0].name != "") {
                    messageData.entities.emotes!!.forEach {
                        var animated = false
                        CurrentUser.emotes!!.forEach { it2 ->
                            if (it.name == it2.name && it2.versions[0].animated) {
                                animated = true
                            }
                        }
                        if (!animated) {
                            val bitmap = bitmapMemoryCache.get(it.name)
                            if (bitmap != null) {
                                var width = bitmap.width * 3
                                if (it.modifiers.contains("wide")) {
                                    width = bitmap.width * 6
                                }
                                val height = bitmap.height * 3
                                val resized =
                                    Bitmap.createScaledBitmap(bitmap, width, height, false)
                                ssb.setSpan(
                                    ImageSpan(this@ChatActivity, resized),
                                    it.bounds[0],
                                    it.bounds[1],
                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                )
                                viewHolder.itemView.messageChatMessage.setText(
                                    ssb,
                                    TextView.BufferType.SPANNABLE
                                )
                            }
                        } else {
                            val gif = gifMemoryCache.get(it.name)
                            if (gif != null) {
                                ssb.setSpan(
                                    ImageSpan(gif, DynamicDrawableSpan.ALIGN_BOTTOM),
                                    it.bounds[0],
                                    it.bounds[1],
                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                )
                            }
                        }
                    }
                }
            }

//            if (messageData.entities.me!!.bounds.isNotEmpty()) {
//                ssb.setSpan(Typeface.ITALIC, messageData.entities.me!!.bounds[0], messageData.entities.me!!.bounds[1], Spannable.SPAN_INCLUSIVE_INCLUSIVE)
//                messageData.data = messageData.data.substringAfter(' ')
//                viewHolder.itemView.messageChatMessage.setTypeface(Typeface.DEFAULT, Typeface.ITALIC)
//            }
//
//            if (messageData.entities.greentext!!.bounds.isNotEmpty()) {
//
//            }

            viewHolder.itemView.messageChatMessage.setText(ssb, TextView.BufferType.SPANNABLE)

            viewHolder.itemView.usernameChatMessage.setOnClickListener {
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.messageData.nick == messageData.nick) {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            adapterItem?.itemView?.usernameChatMessage?.setTextColor(
                                Color.parseColor(
                                    "#F44336"
                                )
                            )
                        }
                    }
                }
                if (CurrentUser.tempHighlightNick == null) {
                    CurrentUser.tempHighlightNick = mutableListOf()
                }
                CurrentUser.tempHighlightNick!!.add(messageData.nick)
            }

            viewHolder.itemView.usernameChatMessage.setOnLongClickListener {
                val pop = PopupMenu(it.context, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            sendMessageText.setText("/w ${messageData.nick} ")
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatMention -> {
                            val currentMessage = sendMessageText.text.toString()
                            if (currentMessage.isNotEmpty()) {
                                if (currentMessage.last() == ' ') {
                                    sendMessageText.setText(currentMessage.plus("${messageData.nick} "))
                                } else {
                                    sendMessageText.setText(currentMessage.plus(" ${messageData.nick} "))
                                }
                            } else {
                                sendMessageText.setText("${messageData.nick} ")
                            }
                            keyRequestFocus(sendMessageText, this@ChatActivity)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatIgnore -> {
                            CurrentUser.options!!.ignoreList.add(messageData.nick)
                            CurrentUser.saveOptions(this@ChatActivity)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    true
                }
                pop.show()
                true
            }

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                for (i in 0 until adapter.itemCount) {
                    if (adapter.getItem(i).layout == R.layout.chat_message_item) {
                        val item = adapter.getItem(i) as ChatMessage
                        if (item.messageData.features.isEmpty() && item.messageData.nick != "Info") {
                            val adapterItem =
                                recyclerViewChat.findViewHolderForAdapterPosition(i)

                            adapterItem?.itemView?.usernameChatMessage?.setTextColor(
                                Color.parseColor(
                                    "#FFFFFF"
                                )
                            )
                        }
                    }
                }
            }
            if (isConsecutive) {
                viewHolder.itemView.usernameChatMessage.visibility = View.GONE
                viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
                viewHolder
            }
        }

        fun isNickSame(nick: String): Boolean {
            return messageData.nick == nick
        }
    }

    inner class PrivateChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.private_chat_message_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (CurrentUser.options!!.ignoreList.contains(messageData.nick)) {
                return
            }

            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(messageData.timestamp)
                viewHolder.itemView.timestampPrivateMessage.visibility = View.VISIBLE
                viewHolder.itemView.timestampPrivateMessage.text = time
            }

            viewHolder.itemView.usernamePrivateMessage.text = messageData.nick
            viewHolder.itemView.messagePrivateMessage.text = " whispered: ${messageData.data}"
        }
    }

    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

        private fun retrieveHistory() {
            val messageHistory =
                Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
            runOnUiThread {
                messageHistory?.forEach {
                    val msg = parseMessage(it)
                    if (msg != null) {
                        var consecutiveMessage = false
                        if (adapter.itemCount > 0) {
                            val lastMessage = adapter?.getItem(adapter.itemCount - 1) as ChatMessage
                            consecutiveMessage = lastMessage.isNickSame(msg.nick)
                        }
                        adapter.add(
                            ChatMessage(msg!!, consecutiveMessage)
                        )
                    }
                }
            }
        }

        private suspend fun retrieveEmotes() {
            val text: String = client.get("https://chat.strims.gg/emote-manifest.json")
            val newText = text.substringAfter("\"emotes\":").substringBeforeLast("]").plus(']')
            val emotes: List<Emote>? = Klaxon().parseArray(newText)
            CurrentUser.emotes = emotes?.toMutableList()
        }

        private fun retrieveCookie() {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://strims.gg")
            cookieManager.flush()
            if (cookies != null) {
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                }
            }
        }

        private fun getBitmapFromURL(src: String?): Bitmap? {
            return try {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                BitmapFactory.decodeStream(input)
            } catch (e: IOException) {
                null
            }
        }

        private fun getGifFromURL(src: String?): Drawable? {
            return try {
                val url = URL(src)
                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input: InputStream = connection.inputStream
                Drawable.createFromStream(input, src)
            } catch (e: IOException) {
                null
            }
        }

        private fun cacheEmotes() {
            runOnUiThread {
                CurrentUser.emotes?.forEach {
                    val url = "https://chat.strims.gg/${it.versions[0].path}"
                    if (!it.versions[0].animated) {
                        GlobalScope.launch {
                            val bitmap = getBitmapFromURL(url)
                            bitmapMemoryCache.put(it.name, bitmap)
                        }
                    } else {
                        GlobalScope.launch {
                            val gif = getGifFromURL(url)
                            gifMemoryCache.put(it.name, gif)
                        }
                    }
                }
            }
        }

        private suspend fun retrieveProfile() {
            val text: String = client.get("https://strims.gg/api/profile") {
                header("Cookie", "jwt=$jwt")
            }
            GlobalScope.launch {
                runOnUiThread {
                    CurrentUser.user = Klaxon().parse(text)
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    chatBottomNavigationView.menu.findItem(R.id.chatProfile).isVisible = true
                    chatBottomNavigationView.menu.findItem(R.id.chatLogin).isVisible = false
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
        ) {
            if (jwt != null) {
                retrieveProfile()
            }
            retrieveEmotes()
            cacheEmotes()
            retrieveOptions()
            retrieveHistory()
            sendMessageButton.setOnClickListener {
                GlobalScope.launch {
                    val messageText = sendMessageText.text.toString()
                    if (messageText.isEmpty()) {
                        return@launch
                    }
                    val first = messageText.first()
                    if (first == '/' && messageText.substringBefore(' ') != "/me") {
                        if (messageText.substringAfter(first).substringBefore(' ') == "w") {
                            val nick = messageText.substringAfter("/w ").substringBefore(' ')
                            val message = messageText.substringAfter("/w $nick ")
                            send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")
                            runOnUiThread {
                                adapter.add(
                                    PrivateChatMessage(
                                        Message(
                                            true,
                                            "To: $nick",
                                            message
                                        )
                                    )
                                )
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "ignore") {
                            val nickIgnore =
                                messageText.substringAfter("/ignore ").substringBefore(' ')
                            CurrentUser.options!!.ignoreList.add(nickIgnore)
                            CurrentUser.saveOptions(this@ChatActivity)
                            runOnUiThread {
                                adapter.add(
                                    ChatMessage(
                                        Message(
                                            false,
                                            "Info",
                                            "Ignoring: $nickIgnore"
                                        )
                                    )
                                )
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "unignore") {
                            val nickUnignore =
                                messageText.substringAfter("/unignore ").substringBefore(' ')
                            if (CurrentUser.options!!.ignoreList.contains(nickUnignore)) {
                                CurrentUser.options!!.ignoreList.remove(nickUnignore)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "Unignored: $nickUnignore"
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
                                                "User not currently ignored"
                                            )
                                        )
                                    )
                                }
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "highlight") {
                            val nickHighlight =
                                messageText.substringAfter("/highlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickHighlight)) {
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "User already highlighted"
                                            )
                                        )
                                    )
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
                                                "Highlighting user: $nickHighlight"
                                            )
                                        )
                                    )
                                }
                            }
                        } else if (messageText.substringAfter(first).substringBefore(' ') == "unhighlight") {
                            val nickUnhighlight =
                                messageText.substringAfter("/unhighlight ").substringBefore(' ')
                            if (CurrentUser.options!!.customHighlights.contains(nickUnhighlight)) {
                                CurrentUser.options!!.customHighlights.remove(nickUnhighlight)
                                CurrentUser.saveOptions(this@ChatActivity)
                                runOnUiThread {
                                    adapter.add(
                                        ChatMessage(
                                            Message(
                                                false,
                                                "Info",
                                                "No longer highlighting user: $nickUnhighlight"
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
                                                "User not currently highlighted"
                                            )
                                        )
                                    )
                                }
                            }
                        } else {
                            runOnUiThread {
                                adapter.add(
                                    ChatMessage(
                                        Message(
                                            false,
                                            "Info",
                                            "Invalid command"
                                        )
                                    )
                                )
                            }
                        }
                    } else {
                        send("MSG {\"data\":\"${sendMessageText.text}\"}")
                    }
                    runOnUiThread {
                        sendMessageText.text.clear()
                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
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
                                        val lastMessage =
                                            adapter.getItem(adapter.itemCount - 1) as ChatMessage
                                        val consecutiveMessage = lastMessage.isNickSame(msg.nick)
                                        adapter.add(
                                            ChatMessage(
                                                msg, consecutiveMessage
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
                    val users: List<ChatUser>? =
                        Klaxon().parseArray(msg[1].substringAfter("\"users\":").substringBefore(",\"connectioncount\":"))
                    CurrentUser.users = users?.toMutableList()
                    CurrentUser.connectionCount =
                        msg[1].substringAfter("\"connectioncount\":").substringBefore('}').toInt()
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
                    val message = Klaxon().parse<Message>(msg[1])
                    if (CurrentUser.options!!.hideNsfw) {
                        val urlRegex =
                            "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$"

                        val p: Pattern = Pattern.compile(urlRegex)
                        val m: Matcher = p.matcher(message!!.data)

                        if (m.find()) {
                            return null
                        }
                    }
                    if (CurrentUser.options!!.ignoreList.isNotEmpty()) {
                        CurrentUser.options!!.ignoreList.forEach {
                            if (message!!.nick == it) {
                                return null
                            }
                            if (CurrentUser.options!!.harshIgnore) {
                                if (message.data.contains(it)) {
                                    return null
                                }
                            }
                        }
                    }
                    return message
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
        ) {
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
                            val newRustlers =
                                input.substringAfter("$id,").substringBefore(",").toInt()
                            val newAfk =
                                input.substringAfter("$id,$newRustlers,").substringBefore("]")
                                    .toInt()
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