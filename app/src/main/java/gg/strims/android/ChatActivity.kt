package gg.strims.android

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.google.android.material.navigation.NavigationView
import com.melegy.redscreenofdeath.RedScreenOfDeath
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.clients.ChatService
import gg.strims.android.clients.StreamsService
import gg.strims.android.fragments.*
import gg.strims.android.models.*
import gg.strims.android.room.PrivateMessage
import gg.strims.android.viewmodels.PrivateMessagesViewModel
import gg.strims.android.viewholders.ChatMessage
import gg.strims.android.viewholders.ChatMessageCombo
import gg.strims.android.viewholders.ErrorChatMessage
import gg.strims.android.viewholders.PrivateChatMessage
import gg.strims.android.viewmodels.ChatViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.autofill_item.view.textViewAutofill
import kotlinx.android.synthetic.main.autofill_item_emote.view.*
import kotlinx.android.synthetic.main.nav_header_main.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.URL
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.collections.HashMap

@KtorExperimentalAPI
@SuppressLint("SetTextI18n", "SimpleDateFormat", "WrongViewCast")
class ChatActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    var chatSocketIntent: Intent? = null
    var streamsSocketIntent: Intent? = null

    companion object {
        var channelId = "chat_notifications"
        var NOTIFICATION_ID = 1
        var NOTIFICATION_REPLY_KEY = "Text"
    }

    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel

    var adapter = GroupAdapter<GroupieViewHolder>()

    private var privateMessageArray = arrayOf("w", "whisper", "msg", "tell", "t", "notify")

    private var modifiersArray = arrayOf(
        "banned",
        "dank",
        "fast",
        "flip",
        "hyper",
        "lag",
        "love",
        "mirror",
        "rain",
        "rustle",
        "slow",
        "smol",
        "snow",
        "spin",
        "virus",
        "wide",
        "worth"
    )

    private val autofillAdapter = GroupAdapter<GroupieViewHolder>()

    private val broadcastReceiver = object : BroadcastReceiver() {

        fun printMessage(message: Message) {
            var consecutiveMessage = false
            /** Check for consecutive message **/
            if (adapter.itemCount > 0) {
                if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item || adapter.getItem(
                        adapter.itemCount - 1
                    ).layout == R.layout.chat_message_item_consecutive_nick
                ) {
                    val lastMessage =
                        adapter.getItem(adapter.itemCount - 1) as ChatMessage
                    consecutiveMessage =
                        lastMessage.isNickSame(message.nick)
                }
            }
            /** Check for combo and adjust previous message accordingly **/
            if (adapter.itemCount > 0 && message.entities.emotes != null && message.entities.emotes!!.isNotEmpty() && message.entities.emotes!![0].combo > 1) {
                if (message.entities.emotes!![0].combo == 2) {
                    adapter.removeGroupAtAdapterPosition(adapter.itemCount - 1)
                    adapter.add(
                        ChatMessageCombo(
                            message,
                            this@ChatActivity,
                            adapter,
                        )
                    )
                } else {
                    if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                        val lastMessageCombo =
                            adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                        lastMessageCombo.setCombo(message.entities.emotes!![0].combo)
                        adapter.notifyItemChanged(adapter.itemCount - 1)
                    }
                }
            } else {
                /** Check if previous message was combo then add message **/
                if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                    val lastMessage =
                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                    lastMessage.state = 1
                    adapter.notifyItemChanged(adapter.itemCount - 1)
                }
                adapter.add(
                    ChatMessage(
                        this@ChatActivity,
                        adapter,
                        message,
                        consecutiveMessage,
                        sendMessageText
                    )
                )
            }
        }

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "gg.strims.android.MESSAGE_HISTORY" && adapter.itemCount <= 1) {
                    Log.d(
                        "TAG",
                        "STARTING PARSING ${(System.currentTimeMillis() - CurrentUser.time)}"
                    )
                    val messageHistory =
                        intent.getStringArrayListExtra("gg.strims.android.MESSAGE_HISTORY_TEXT")
                    messageHistory?.forEach {
                        val message = parseChatMessage(it)
                        if (message != null) {
                            printMessage(message)
                        }
                    }
                    Log.d(
                        "TAG",
                        "ENDING PARSING ${(System.currentTimeMillis() - CurrentUser.time)}"
                    )
                } else if (intent.action == "gg.strims.android.MESSAGE") {
                    val message =
                        parseChatMessage(intent.getStringExtra("gg.strims.android.MESSAGE_TEXT")!!)

                    /** Remove duplicate messages **/
                    if (adapter.itemCount > 0 && message != null) {
                        if (adapter.getItem(adapter.itemCount - 2).layout == R.layout.chat_message_item || adapter.getItem(
                                adapter.itemCount - 2
                            ).layout == R.layout.chat_message_item_consecutive_nick
                        ) {
                            val lastMessage =
                                adapter.getItem(adapter.itemCount - 2) as ChatMessage
                            if (lastMessage.messageData.nick == message.nick && lastMessage.messageData.data == message.data) {
                                return
                            }
                        }
                    }

                    if (message != null) {
                        Log.d("TAG", "FROM SERVICE: ${message.nick} + ${message.data}")
                        if (message.privMsg) {
                            val isReceived = message.nick != CurrentUser.user?.username
                            adapter.add(
                                PrivateChatMessage(
                                    this@ChatActivity,
                                    adapter,
                                    message,
                                    isReceived,
                                    sendMessageText
                                )
                            )
                            if (CurrentUser.options!!.notifications && message.nick != CurrentUser.user!!.username) {
                                displayNotification(message)
                            }
                        } else {
                            printMessage(message)
                        }
                        val recycler = findViewById<RecyclerView>(R.id.recyclerViewChat)
                        val layoutTest =
                            recycler.layoutManager as LinearLayoutManager
                        val lastItem = layoutTest.findLastVisibleItemPosition()
                        if (lastItem >= adapter.itemCount - 3) {
                            recycler.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                } else if (intent.action == "gg.strims.android.PROFILE") {
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    navHeaderUsername.text = CurrentUser.user!!.username
                    nav_view.menu.findItem(R.id.nav_Profile).isVisible = true
                    nav_view.menu.findItem(R.id.nav_Whispers).isVisible = true
                    nav_view.setCheckedItem(R.id.nav_Chat)
                    invalidateOptionsMenu()
                } else if (intent.action == "gg.strims.android.CHAT_SOCKET_CLOSE") {
                    adapter.add(
                        ChatMessage(
                            this@ChatActivity,
                            adapter,
                            Message(
                                false,
                                "Info",
                                "Disconnected, reconnecting..."
                            )
                        )
                    )
                    stopService(chatSocketIntent)
                    startService(chatSocketIntent)
                } else if (intent.action == "gg.strims.android.STREAMS_SOCKET_CLOSE") {
                    stopService(streamsSocketIntent)
                    startService(streamsSocketIntent)
                } else if (intent.action == "gg.strims.android.STREAMS") {
                    val streams = intent.getStringExtra("gg.strims.android.STREAMS_TEXT")
                    StreamsFragment().parseStream(streams!!)
                } else if (intent.action == "gg.strims.android.SHOWSTREAM") {
                    val fragment = supportFragmentManager.findFragmentById(R.id.angelthump_fragment)
                    showFragment(this@ChatActivity, fragment!!)
                }
            }
        }
    }

    private var scrollUponResume = false

    override fun onPause() {
        val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
        val lastItem = layoutTest.findLastVisibleItemPosition()
        if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
            scrollUponResume = false
        } else {
            goToBottomLayout.visibility = View.GONE
            scrollUponResume = true
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (scrollUponResume) {
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
        val lastItem = layoutTest.findLastVisibleItemPosition()
        if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
            goToBottomLayout.visibility = View.VISIBLE
            goToBottom.isEnabled = true
        } else {
            goToBottomLayout.visibility = View.GONE
            goToBottom.isEnabled = false
        }
    }

    private var chatViewModel: ChatViewModel? = null

    override fun onSaveInstanceState(outState: Bundle) {
        chatViewModel?.chatAdapter = adapter

        chatViewModel?.streamsSocketIntent = streamsSocketIntent
        chatViewModel?.chatSocketIntent = chatSocketIntent

        if (CurrentUser.tempStream == null &&
            (CurrentUser.tempTwitchVod == null &&
                    CurrentUser.tempTwitchUrl == null) &&
            CurrentUser.tempYouTubeId == null) {
            chatViewModel?.visibleStream = null
        } else if (CurrentUser.tempStream != null) {
            chatViewModel?.visibleStream = "angelthump"
        } else if (CurrentUser.tempTwitchVod != null || CurrentUser.tempTwitchUrl != null) {
            chatViewModel?.visibleStream = "twitch"
        } else if (CurrentUser.tempYouTubeId != null) {
            chatViewModel?.visibleStream = "youtube"
        }

        for (i in 0 until adapter.itemCount) {
            if (adapter.getItem(i).layout == R.layout.chat_message_item ||
                adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                val item = adapter.getItem(i) as ChatMessage
                item.adapter = null
                item.sendMessageText = null
            }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation_drawer)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment2) as NavHostFragment
        val navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_Streams, R.id.nav_Profile, R.id.nav_Settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.setNavigationItemSelectedListener(this)

        RedScreenOfDeath.init(this.application)

        toolbar.title = "Chat"

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            supportActionBar?.hide()
        }

        navView.setCheckedItem(R.id.nav_Chat)

        chatViewModel = ViewModelProvider(this).get(ChatViewModel::class.java)

        val intentFilter = IntentFilter()
        intentFilter.addAction("gg.strims.android.MESSAGE")
        intentFilter.addAction("gg.strims.android.MESSAGE_HISTORY")
        intentFilter.addAction("gg.strims.android.PROFILE")
        intentFilter.addAction("gg.strims.android.CHAT_SOCKET_CLOSE")
        intentFilter.addAction("gg.strims.android.STREAMS_SOCKET_CLOSE")
        intentFilter.addAction("gg.strims.android.STREAMS")
        intentFilter.addAction("gg.strims.android.SHOWSTREAM")
        registerReceiver(broadcastReceiver, intentFilter)

        if (savedInstanceState != null) {
            progressBar.visibility = View.GONE

            if (chatViewModel != null && chatViewModel!!.chatAdapter != null) {
                adapter = chatViewModel!!.chatAdapter!!

                if (CurrentUser.user != null) {
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    nav_view.menu.findItem(R.id.nav_Profile).isVisible = true
                    nav_view.menu.findItem(R.id.nav_Whispers).isVisible = true
                    val header = navView.getHeaderView(0)
                    header.navHeaderUsername.text = CurrentUser.user!!.username
                }
                nav_view.setCheckedItem(R.id.nav_Chat)

                streamsSocketIntent = chatViewModel?.streamsSocketIntent
                chatSocketIntent = chatViewModel?.chatSocketIntent

                if (chatViewModel?.visibleStream != null && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    constraintLayoutStream.visibility = View.VISIBLE
                }

                when (chatViewModel?.visibleStream) {
                    "angelthump" -> {
                        showFragment(
                            this,
                            supportFragmentManager.findFragmentById(R.id.angelthump_fragment)!!
                        )
                    }

                    "twitch" -> {
                        showFragment(
                            this,
                            supportFragmentManager.findFragmentById(R.id.twitch_fragment)!!
                        )
                    }

                    "youtube" -> {
                        showFragment(
                            this,
                            supportFragmentManager.findFragmentById(R.id.youtube_fragment)!!
                        )
                    }
                }
            }

            for (i in 0 until adapter.itemCount) {
                if (adapter.getItem(i).layout == R.layout.chat_message_item ||
                    adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                    val item = adapter.getItem(i) as ChatMessage
                    item.adapter = adapter
                    item.sendMessageText = sendMessageText
                }
            }
        } else {
            CurrentUser.bitmapMemoryCache = HashMap()
            CurrentUser.gifMemoryCache = HashMap()

            privateMessagesViewModel =
                ViewModelProvider(this).get(PrivateMessagesViewModel::class.java)

            chatSocketIntent = Intent(this, ChatService::class.java)
            streamsSocketIntent = Intent(this, StreamsService::class.java)
            startService(chatSocketIntent)
            startService(streamsSocketIntent)

            GlobalScope.launch(Dispatchers.IO) {
                Log.d("TAG", "OPTIONS ${(System.currentTimeMillis() - CurrentUser.time)}")
                retrieveOptions()
                Log.d("TAG", "OPTIONS ENDING ${(System.currentTimeMillis() - CurrentUser.time)}")
            }

            GlobalScope.launch(Dispatchers.IO) {
                Log.d("TAG", "EMOTES ${(System.currentTimeMillis() - CurrentUser.time)}")
                retrieveEmotes()
                Log.d("TAG", "EMOTES ENDING ${(System.currentTimeMillis() - CurrentUser.time)}")
            }
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
                if (sendMessageText.text.isNotEmpty() && sendMessageText.text.last() != ' ') {
                    recyclerViewAutofill.visibility = View.VISIBLE
                    goToBottomLayout.visibility = View.GONE
                    if (sendMessageText.text.first() == '/' && !sendMessageText.text.contains(' ')) {
                        val currentWord = sendMessageText.text.toString().substringAfter('/')

                        privateMessageArray.forEach {
                            if (it.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemCommand(it))
                            }
                        }
                    } else {
                        val currentWord = sendMessageText.text.toString().substringAfterLast(' ')

                        if (currentWord.contains(':')) {
                            CurrentUser.emotes!!.forEach { emote ->
                                if (sendMessageText.text.contains(emote.name)) {
                                    modifiersArray.forEach {
                                        if (it.contains(currentWord.substringAfterLast(':'))) {
                                            autofillAdapter.add(AutofillItemModifier(it))
                                        }
                                    }
                                    return@forEach
                                }
                            }
                        }

                        CurrentUser.users.sortByDescending {
                            it
                        }

                        CurrentUser.users.forEach {
                            if (it.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemUser(it))
                            }
                        }

                        CurrentUser.bitmapMemoryCache.forEach {
                            if (it.key.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemEmote(it.key, it.value))
                            }
                        }
                    }
                } else if (sendMessageText.text.isEmpty()) {
                    recyclerViewAutofill.visibility = View.GONE
                    val layoutTest = recyclerViewChat.layoutManager as LinearLayoutManager
                    val lastItem = layoutTest.findLastVisibleItemPosition()
                    if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
                        goToBottomLayout.visibility = View.VISIBLE
                    } else {
                        goToBottomLayout.visibility = View.GONE
                    }
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
            if (lastItem < recyclerViewChat.adapter!!.itemCount - 1) {
                goToBottomLayout.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottomLayout.visibility = View.GONE
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

        userListButton.setOnClickListener {
            hideKeyboardFrom(this, sendMessageText)
            showHideFragment(
                this,
                supportFragmentManager.findFragmentById(R.id.user_list_fragment)!!
            )
        }

        emoteMenuButton.setOnClickListener {
            hideKeyboardFrom(this, sendMessageText)
            showHideFragment(
                this,
                supportFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
            )
        }

        sendMessageButton.setOnClickListener {
            val messageText = sendMessageText.text.toString()
            if (messageText.isEmpty()) {
                return@setOnClickListener
            }
            val first = messageText.first()
            if (first == '/' && messageText.substringBefore(' ') != "/me") {
                val command = messageText.substringAfter(first).substringBefore(' ')
                var privateMessageCommand = ""
                for (privateMessageItem in privateMessageArray) {
                    if (privateMessageItem.contains(
                            messageText.substringAfter(first).substringBefore(' '), true
                        )
                    ) {
                        privateMessageCommand =
                            messageText.substringAfter(first).substringBefore(' ')
                        break
                    }

                }
                if (privateMessageCommand != "") {
                    if (messageText.length <= privateMessageCommand.length + 2) {
                        adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
                    } else {
                        val nick =
                            messageText.substringAfter("$privateMessageCommand ").substringBefore(
                                ' '
                            )
                        val nickRegex = "^[A-Za-z0-9_]{3,20}$"
                        val p: Pattern = Pattern.compile(nickRegex)
                        val m: Matcher = p.matcher(nick)

                        if (!m.find()) {
                            runOnUiThread {
                                adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
                            }
                        } else {
                            var message = messageText.substringAfter("$privateMessageCommand $nick")
                            message = message.substringAfter(" ")
                            if (message.trim() == "") {
                                adapter.add(ErrorChatMessage("The message was invalid"))
                                sendMessageText.text.clear()
                                return@setOnClickListener
                            } else {
                                val intent = Intent("gg.strims.android.SEND_MESSAGE")
                                intent.putExtra(
                                    "gg.strims.android.SEND_MESSAGE_TEXT",
                                    "PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}"
                                )
                                sendBroadcast(intent)
                            }
                        }
                    }
                } else if (command == "ignore") {
                    val nickIgnore =
                        messageText.substringAfter("/ignore ").substringBefore(' ')
                    CurrentUser.options!!.ignoreList.add(nickIgnore)
                    CurrentUser.saveOptions(this@ChatActivity)
                    adapter.add(
                        ChatMessage(
                            this@ChatActivity,
                            adapter,
                            Message(
                                false,
                                "Info",
                                "Ignoring: $nickIgnore"
                            )
                        )
                    )
                } else if (command == "unignore") {
                    val nickUnignore =
                        messageText.substringAfter("/unignore ").substringBefore(' ')
                    if (CurrentUser.options!!.ignoreList.contains(nickUnignore)) {
                        CurrentUser.options!!.ignoreList.remove(nickUnignore)
                        CurrentUser.saveOptions(this@ChatActivity)
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "Unignored: $nickUnignore"
                                )
                            )
                        )
                    } else {
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "User not currently ignored"
                                )
                            )
                        )
                    }
                } else if (command == "highlight") {
                    val nickHighlight =
                        messageText.substringAfter("/highlight ").substringBefore(' ')
                    if (CurrentUser.options!!.customHighlights.contains(nickHighlight)) {
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "User already highlighted"
                                )
                            )
                        )
                    } else {
                        CurrentUser.options!!.customHighlights.add(nickHighlight)
                        CurrentUser.saveOptions(this@ChatActivity)
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "Highlighting user: $nickHighlight"
                                )
                            )
                        )
                    }
                } else if (command == "unhighlight") {
                    val nickUnhighlight =
                        messageText.substringAfter("/unhighlight ").substringBefore(' ')
                    if (CurrentUser.options!!.customHighlights.contains(nickUnhighlight)) {
                        CurrentUser.options!!.customHighlights.remove(nickUnhighlight)
                        CurrentUser.saveOptions(this@ChatActivity)
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "No longer highlighting user: $nickUnhighlight"
                                )
                            )
                        )
                    } else {
                        adapter.add(
                            ChatMessage(
                                this@ChatActivity,
                                adapter,
                                Message(
                                    false,
                                    "Info",
                                    "User not currently highlighted"
                                )
                            )
                        )
                    }
                } else if (command == "help") {
                    adapter.add(
                        ChatMessage(
                            this@ChatActivity,
                            adapter,
                            Message(
                                false,
                                "Info",
                                resources.getString(R.string.help)
                            )
                        )
                    )
                } else {
                    adapter.add(
                        ChatMessage(
                            this@ChatActivity,
                            adapter,
                            Message(
                                false,
                                "Info",
                                "Invalid command"
                            )
                        )
                    )
                }
            } else {
                val intent = Intent("gg.strims.android.SEND_MESSAGE")
                intent.putExtra(
                    "gg.strims.android.SEND_MESSAGE_TEXT",
                    "MSG {\"data\":\"${sendMessageText.text}\"}"
                )
                sendBroadcast(intent)
            }
            sendMessageText.text.clear()
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun retrieveEmotes() {
        val text = URL("https://chat.strims.gg/emote-manifest.json").readText()
        val emotesParsed: EmotesParsed = Klaxon().parse(text)!!
        CurrentUser.emotes = emotesParsed.emotes.toMutableList()
        cacheEmotes()
    }

    private fun retrieveOptions() {
        val sharedPreferences = getSharedPreferences("ChatOptions", Context.MODE_PRIVATE)
        val options = sharedPreferences.getString("options", "")
        if (options != null && options.isNotEmpty()) {
            CurrentUser.options = Klaxon().parse(options)
        } else {
            CurrentUser.options = Options()
        }
    }

    private fun cacheEmotes() {
        CurrentUser.emotes?.forEach {
            val size = it.versions.size - 1
            val biggestEmote = it.versions[size]
            val url = "https://chat.strims.gg/${biggestEmote.path}"
            if (!biggestEmote.animated) {
                GlobalScope.launch {
                    val bitmap = getBitmapFromURL(url)
                    CurrentUser.bitmapMemoryCache[it.name] = bitmap!!
                }
            } else {
                GlobalScope.launch {
                    val gif = getGifFromURL(url)
                    CurrentUser.gifMemoryCache[it.name] = gif!!
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.navigation_drawer_options, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu!!.findItem(R.id.optionsLogIn).isVisible = CurrentUser.user == null
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.optionsLogIn -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, LoginFragment(), "LoginFragment")
                    .addToBackStack("LoginFragment").commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment2) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_Streams -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, StreamsFragment(), "StreamsFragment")
                    .addToBackStack("StreamsFragment").commit()
            }

            R.id.nav_Chat -> {
                supportFragmentManager.fragments.forEach {
                    if (it.tag == "StreamsFragment" ||
                        it.tag == "ProfileFragment" ||
                        it.tag == "OptionsFragment" ||
                        it.tag == "WhispersFragment" ||
                        it.tag == "WhispersUserFragment"
                    ) {
                        supportFragmentManager.beginTransaction().remove(it).commit()
                    }
                }
                toolbar.title = "Chat"
            }

            R.id.nav_Profile -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, ProfileFragment(), "ProfileFragment")
                    .addToBackStack("ProfileFragment").commit()
            }

            R.id.nav_Whispers -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, WhispersFragment(), "WhispersFragment")
                    .addToBackStack("WhispersFragment").commit()
            }

            R.id.nav_Settings -> {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, OptionsFragment(), "OptionsFragment")
                    .addToBackStack("OptionsFragment").commit()
            }
        }
        drawer_layout.closeDrawers()
        return true
    }

    override fun onBackPressed() {
        if (nav_view.checkedItem?.title == "Chat" || toolbar.title == "Chat") {
            return
        } else {
            supportFragmentManager.popBackStack()

            if (supportFragmentManager.backStackEntryCount <= 1) {
                nav_view.setCheckedItem(R.id.nav_Chat)
                toolbar.title = "Chat"
            }
        }
    }

    inner class AutofillItemCommand(private val command: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = "/$command"

            viewHolder.itemView.textViewAutofill.setOnClickListener {

                sendMessageText.setText("/$command ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    inner class AutofillItemUser(private val user: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = user

            viewHolder.itemView.textViewAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage =
                    sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                if (!user.toLowerCase(Locale.ROOT)
                        .contains(currentMessage.toLowerCase(Locale.ROOT))
                ) {
                    sendMessageText.setText("$currentMessage $user ")
                } else {
                    sendMessageText.setText("$user ")
                }
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    inner class AutofillItemEmote(private val emote: String, private val bitmap: Bitmap) :
        Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item_emote

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = emote
            viewHolder.itemView.imageViewEmoteAutofill.setImageBitmap(bitmap)
            viewHolder.itemView.textViewAutofill.setOnClickListener {
                val currentWord = sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage =
                    sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                if (!emote.toLowerCase(Locale.ROOT)
                        .contains(currentMessage.toLowerCase(Locale.ROOT))
                ) {
                    sendMessageText.setText("$currentMessage $emote ")
                } else {
                    sendMessageText.setText("$emote ")
                }
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    inner class AutofillItemModifier(private val modifier: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = modifier

            viewHolder.itemView.textViewAutofill.setOnClickListener {
                var currentWord = sendMessageText.text.toString().substringAfterLast(':')
                if (currentWord.isEmpty()) {
                    currentWord = ":"
                }
                var currentMessage =
                    sendMessageText.text.toString().substringBeforeLast(currentWord)

                if (currentMessage.last() == ' ') {
                    currentMessage = currentMessage.trimEnd(' ')
                }

                if (currentMessage.last() != ':') {
                    currentMessage = currentMessage.plus(':')
                }
                sendMessageText.setText("${currentMessage}${modifier} ")
                sendMessageText.setSelection(sendMessageText.length())
                recyclerViewAutofill.visibility = View.GONE
            }
        }
    }

    private fun savePrivateMessage(message: Message) {
        if (CurrentUser.user == null) {
            return
        }

        privateMessagesViewModel.addMessage(
            PrivateMessage(
                0,
                message.privMsg,
                message.nick,
                message.data,
                message.timestamp,
                message.features,
                message.entities,
                message.targetNick!!
            )
        )
    }

    override fun onDestroy() {
        Log.d("TAG", "DESTROYED")
        unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    private fun displayNotification(message: Message) {
        val activityIntent = Intent(this, ChatActivity::class.java)
        val resultingActivityPendingIntent = PendingIntent.getActivity(
            this, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.nick)
            .setContentText(message.data)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(resultingActivityPendingIntent)

        val remoteInput = RemoteInput.Builder(NOTIFICATION_REPLY_KEY).setLabel("Reply").build()

        val replyIntent = Intent("gg.strims.android.SEND_NOT_MESSAGE")
        replyIntent.putExtra(
            "gg.strims.android.SEND_MESSAGE_NICK",
            message.nick
        )

        val replyPendingIntent =
            PendingIntent.getBroadcast(this, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_background,
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

    private fun parseChatMessage(input: String): Message? {
        val msg = input.split(" ", limit = 2)
        when (msg[0]) {
            "NAMES" -> {
                val names: NamesMessage = Klaxon().parse(msg[1])!!
                names.users.forEach {
                    CurrentUser.users.add(it.nick)
                }
                runOnUiThread {
                    if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                        val lastMessage =
                            adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                        lastMessage.state = 1
                        adapter.notifyItemChanged(adapter.itemCount - 1)
                    }
                    adapter.add(
                        ChatMessage(
                            this@ChatActivity,
                            adapter,
                            Message(
                                false,
                                "Info",
                                "Connected users: ${names.connectioncount}"
                            )
                        )
                    )
                    progressBar.visibility = View.GONE
                    Log.d("TAG", "ENDING BAR ${(System.currentTimeMillis() - CurrentUser.time)}")
                    recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                }
            }
            "JOIN" -> {
                val userJoin = Klaxon().parse<String>(msg[1])
                if (!CurrentUser.users.contains(userJoin)) {
                    CurrentUser.users.add(userJoin!!)
                }
            }
            "QUIT" -> {
                val userQuit = Klaxon().parse<String>(msg[1])
                if (CurrentUser.users.contains(userQuit)) {
                    CurrentUser.users.remove(userQuit)
                }
            }
            "PRIVMSG", "PRIVMSGSENT" -> {
                val message = Klaxon().parse<Message>(msg[1])!!
                message.privMsg = true

                if (CurrentUser.options!!.ignoreList.isNotEmpty()) {
                    CurrentUser.options!!.ignoreList.forEach {
                        if (message.nick == it) {
                            return null
                        }
                        if (CurrentUser.options!!.harshIgnore) {
                            if (message.data.contains(it)) {
                                return null
                            }
                        }
                    }
                }

                savePrivateMessage(message)
                sendBroadcast(Intent("gg.strims.android.PRIVATE_MESSAGE"))
                return message
            }
            "MSG" -> {
                val message = Klaxon().parse<Message>(msg[1])!!
                if (CurrentUser.options!!.hideNsfw && message.entities.links!!.isNotEmpty()
                    && message.entities.tags!!.isNotEmpty()
                ) {
                    message.entities.tags!!.forEach {
                        if (it.name == "nsfw" || it.name == "nsfl") {
                            return null
                        }
                    }
                }

                if (CurrentUser.options!!.ignoreList.isNotEmpty()) {
                    CurrentUser.options!!.ignoreList.forEach {
                        if (message.nick == it) {
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
            "ERR" -> {
                val error = msg[1].replace("\"", "")
                if (error == "throttled") {
                    adapter.add(ErrorChatMessage("Throttled! You were trying to send messages too fast."))
                } else if (error == "duplicate") {
                    adapter.add(ErrorChatMessage("The message is identical to the last one you sent."))
                }
                val layoutTest =
                    recyclerViewChat.layoutManager as LinearLayoutManager
                val lastItem = layoutTest.findLastVisibleItemPosition()
                if (lastItem >= recyclerViewChat.adapter!!.itemCount - 3) {
                    recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                }
            }
            "VIEWERSTATE" -> {
                val state = Klaxon().parse<ViewerState>(msg[1])
                CurrentUser.viewerStates?.forEach {
                    if (it.nick == state?.nick) {
                        CurrentUser.viewerStates!![CurrentUser.viewerStates!!.indexOf(it)] = state
                        return null
                    }
                }
            }
        }
        return null
    }
}
