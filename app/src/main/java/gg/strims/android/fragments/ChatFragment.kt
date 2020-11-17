package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.Klaxon
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Message
import gg.strims.android.models.NamesMessage
import gg.strims.android.models.ViewerState
import gg.strims.android.room.PrivateMessage
import gg.strims.android.viewholders.ChatMessage
import gg.strims.android.viewholders.ChatMessageCombo
import gg.strims.android.viewholders.ErrorChatMessage
import gg.strims.android.viewholders.PrivateChatMessage
import gg.strims.android.viewmodels.*
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.autofill_item.view.textViewAutofill
import kotlinx.android.synthetic.main.autofill_item_emote.view.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.nav_header_main.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

@KtorExperimentalAPI
@SuppressLint("SetTextI18n")
class ChatFragment : Fragment() {

    var adapter = GroupAdapter<GroupieViewHolder>()
    private val autofillAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var commandsArray: Array<String>
    private lateinit var modifiersArray: Array<String>

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel
    private lateinit var exoPlayerViewModel: ExoPlayerViewModel
    private lateinit var twitchViewModel: TwitchViewModel
    private lateinit var youTubeViewModel: YouTubeViewModel
    private lateinit var profileViewModel: ProfileViewModel

    private var missedMessages = mutableListOf<Message>()

    private fun printMessage(message: Message) {
        var consecutiveMessage = false
        /** Check for consecutive message **/
        if (message.nick == "Info") {
            consecutiveMessage = false
        } else if (adapter.itemCount > 0) {
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
                        requireContext(),
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
            if (message.privMsg) {
                val isReceived = message.nick != CurrentUser.user?.username
                adapter.add(
                    PrivateChatMessage(
                        requireContext(),
                        adapter,
                        message,
                        isReceived,
                        sendMessageText
                    )
                )
                if (CurrentUser.optionsLiveData.value?.notifications!! && message.nick != CurrentUser.user!!.username) {
                    displayNotification(message)
                }
            } else {
                adapter.add(
                    ChatMessage(
                        requireContext(),
                        adapter,
                        message,
                        consecutiveMessage,
                        sendMessageText
                    )
                )
            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val parentActivity = requireActivity() as ChatActivity
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
                            chatViewModel.addMessage(message)
                        }
                    }
                    progressBarFragment.visibility = View.GONE
                    Log.d(
                        "TAG",
                        "ENDING PARSING ${(System.currentTimeMillis() - CurrentUser.time)}"
                    )
                } else if (intent.action == "gg.strims.android.MESSAGE") {
                    val message =
                        parseChatMessage(intent.getStringExtra("gg.strims.android.MESSAGE_TEXT")!!)

                    /** Remove duplicate messages **/
                    if (adapter.itemCount > 1 && message != null) {
                        // TODO: LOOK AT THIS BELOW: "Wanted item at position -1 but an Item is a Group of size 1"
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
                        if (::chatViewModel.isInitialized) {
                            chatViewModel.addMessage(message)
                        } else {
                            missedMessages.add(message)
                        }
                        val recycler = recyclerViewChat
                        if (recycler != null) {
                            val layoutTest =
                                recycler.layoutManager as LinearLayoutManager
                            val lastItem = layoutTest.findLastVisibleItemPosition()
                            if (lastItem >= adapter.itemCount - 3) {
                                recycler.scrollToPosition(adapter.itemCount - 1)
                            }
                        }
                    }
                } else if (intent.action == "gg.strims.android.PROFILE") {
                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    requireActivity().navHeaderUsername.text = CurrentUser.user!!.username
                    requireActivity().nav_view.menu.findItem(R.id.nav_Profile).isVisible = true
                    requireActivity().nav_view.menu.findItem(R.id.nav_Whispers).isVisible = true
                    requireActivity().nav_view.setCheckedItem(R.id.nav_Chat)
                    requireActivity().invalidateOptionsMenu()
                } else if (intent.action == "gg.strims.android.CHAT_SOCKET_CLOSE") {
                    val message = Message(
                        false,
                        "Info",
                        "Disconnected, reconnecting..."
                    )
                    chatViewModel.addMessage(message)
                    parentActivity.stopService(parentActivity.chatSocketIntent)
                    parentActivity.startService(parentActivity.chatSocketIntent)
                } else if (intent.action == "gg.strims.android.STREAMS_SOCKET_CLOSE") {
                    //TODO: INVESTIGATE BELOW NULL OBJECT REFERENCE
                    parentActivity.stopService(parentActivity.streamsSocketIntent)
                    parentActivity.startService(parentActivity.streamsSocketIntent)
                } else if (intent.action == "gg.strims.android.STREAMS") {
                    val streams = intent.getStringExtra("gg.strims.android.STREAMS_TEXT")
                    StreamsFragment().parseStream(streams!!)
                } else if (intent.action == "gg.strims.android.SHOWSTREAM") {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        constraintLayoutStreamFragment.visibility = View.VISIBLE
                    }

                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.angelthump_fragment)!!
                    )
                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.twitch_fragment)!!
                    )
                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.youtube_fragment)!!
                    )

                    with(exoPlayerViewModel) {
                        when {
                            liveDataStream.value != null -> {
                                val fragment = childFragmentManager.findFragmentById(R.id.angelthump_fragment)
                                showChildFragment(this@ChatFragment, fragment!!)
                            }
                        }
                    }
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

    override fun onDestroy() {
        Log.d("TAG", "DESTROYING FRAGMENT")
        requireActivity().unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("TAG", "SAVING")

        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TAG", "ONCREATE")

        modifiersArray = resources.getStringArray(R.array.modifiersArray)
        commandsArray = resources.getStringArray(R.array.commandsArray)

        privateMessagesViewModel =
            ViewModelProvider(requireActivity()).get(PrivateMessagesViewModel::class.java)

        val intentFilter = IntentFilter()
        intentFilter.addAction("gg.strims.android.MESSAGE")
        intentFilter.addAction("gg.strims.android.MESSAGE_HISTORY")
        intentFilter.addAction("gg.strims.android.PROFILE")
        intentFilter.addAction("gg.strims.android.CHAT_SOCKET_CLOSE")
        intentFilter.addAction("gg.strims.android.STREAMS_SOCKET_CLOSE")
        intentFilter.addAction("gg.strims.android.STREAMS")
        intentFilter.addAction("gg.strims.android.SHOWSTREAM")
        requireActivity().registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("TAG", "ONCREATEVIEW")
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("TAG", "ONVIEWCREATED")
        chatViewModel = ViewModelProvider(requireActivity()).get(ChatViewModel::class.java)
        exoPlayerViewModel = ViewModelProvider(requireActivity()).get(ExoPlayerViewModel::class.java)
        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)
        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)

        chatViewModel.latestMessage.value = null

        chatViewModel.latestMessage.observe(viewLifecycleOwner, {
            if (it != null) {
                printMessage(it)
            }
        })

        exoPlayerViewModel.liveDataStream.observe(viewLifecycleOwner, {
            if (it != null) {
                // Close YouTube and Twitch
                val fragment = childFragmentManager.findFragmentById(R.id.angelthump_fragment)
                showChildFragment(this@ChatFragment, fragment!!)
            }
        })

        twitchViewModel.channel.observe(viewLifecycleOwner, {
            if (it != null) {
                // Close AT and YouTube
                val fragment = childFragmentManager.findFragmentById(R.id.twitch_fragment)
                showChildFragment(this@ChatFragment, fragment!!)
            }
        })

        youTubeViewModel.videoId.observe(viewLifecycleOwner, {
            if (it != null) {
                // Close AT and Twitch
                val fragment = childFragmentManager.findFragmentById(R.id.youtube_fragment)
                showChildFragment(this@ChatFragment, fragment!!)
            }
        })

        profileViewModel.logOut.observe(viewLifecycleOwner, {
            if (it) {
                sendMessageText.hint = resources.getString(R.string.log_in_to_send_messages)
                progressBarFragment.visibility = View.VISIBLE
                profileViewModel.logOut.value = false
            }
        })

        if (CurrentUser.user != null) {
            sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
        }

        if (savedInstanceState != null) {
            Log.d("TAG", "SAVESTATE NOT NULL")

            chatViewModel.messages.value?.forEach {
                printMessage(it)
            }

            progressBarFragment.visibility = View.GONE
        }

        if (missedMessages.isNotEmpty()) {
            missedMessages.forEach {
                printMessage(it)
            }
            missedMessages.clear()
        }

        if ((exoPlayerViewModel.liveDataStream.value != null ||
                    twitchViewModel.channel.value != null ||
                    youTubeViewModel.videoId.value != null)
            && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        ) {
            constraintLayoutStreamFragment.visibility = View.VISIBLE
        }

        if (adapter.itemCount == 0) {
            progressBarFragment.visibility = View.VISIBLE
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

                        commandsArray.forEach {
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

        val layoutManager = LinearLayoutManager(requireContext())
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
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        goToBottom.setOnClickListener {
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        userListButtonFragment.setOnClickListener {
            if (childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                )
            }
            hideKeyboardFrom(requireContext(), sendMessageText)
            if (childFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                )
            } else {
                showChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                )
            }
        }

        emoteMenuButtonFragment.setOnClickListener {
            if (childFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                )
            }
            hideKeyboardFrom(requireContext(), sendMessageText)
            if (childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                )
            } else {
                showChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                )
            }
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
                for (privateMessageItem in commandsArray) {
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
                            adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
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
                                requireContext().sendBroadcast(intent)
                            }
                        }
                    }
                } else if (command == "ignore") {
                    val nickIgnore =
                        messageText.substringAfter("/ignore ").substringBefore(' ')
                    CurrentUser.optionsLiveData.value?.ignoreList?.add(nickIgnore)
                    CurrentUser.addIgnore(nickIgnore)
                    val message = Message(
                        false,
                        "Info",
                        "Ignoring: $nickIgnore"
                    )
                    chatViewModel.addMessage(message)
                } else if (command == "unignore") {
                    val message: Message?
                    val nickUnignore =
                        messageText.substringAfter("/unignore ").substringBefore(' ')
                    message = if (CurrentUser.optionsLiveData.value?.ignoreList!!.contains(nickUnignore)) {
                        CurrentUser.optionsLiveData.value?.ignoreList?.remove(nickUnignore)
                        CurrentUser.removeIgnore(nickUnignore)
                        Message(
                            false,
                            "Info",
                            "Unignored: $nickUnignore"
                        )
                    } else {
                        Message(
                            false,
                            "Info",
                            "User not currently ignored"
                        )
                    }
                    chatViewModel.addMessage(message)
                } else if (command == "highlight") {
                    val message: Message?
                    val nickHighlight =
                        messageText.substringAfter("/highlight ").substringBefore(' ')
                    message = if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(nickHighlight)) {
                        Message(
                            false,
                            "Info",
                            "User already highlighted"
                        )
                    } else {
                        CurrentUser.addHighlight(nickHighlight)
                        Message(
                            false,
                            "Info",
                            "Highlighting user: $nickHighlight"
                        )
                    }
                    chatViewModel.addMessage(message)
                } else if (command == "unhighlight") {
                    val message: Message?
                    val nickUnhighlight =
                        messageText.substringAfter("/unhighlight ").substringBefore(' ')
                    message = if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(nickUnhighlight)) {
                        CurrentUser.removeHighlight(nickUnhighlight)
                        Message(
                            false,
                            "Info",
                            "No longer highlighting user: $nickUnhighlight"
                        )
                    } else {
                        Message(
                            false,
                            "Info",
                            "User not currently highlighted"
                        )
                    }
                    chatViewModel.addMessage(message)
                } else if (command == "help") {
                    val message = Message(
                        false,
                        "Info",
                        resources.getString(R.string.help)
                    )
                    chatViewModel.addMessage(message)
                } else {
                    val message = Message(
                        false,
                        "Info",
                        "Invalid command"
                    )
                    chatViewModel.addMessage(message)
                }
            } else {
                val intent = Intent("gg.strims.android.SEND_MESSAGE")
                intent.putExtra(
                    "gg.strims.android.SEND_MESSAGE_TEXT",
                    "MSG {\"data\":\"${sendMessageText.text}\"}"
                )
                requireContext().sendBroadcast(intent)
            }
            sendMessageText.text.clear()
            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }
    }

    private fun displayNotification(message: Message) {
        val activityIntent = Intent(requireContext(), ChatActivity::class.java)
        val resultingActivityPendingIntent = PendingIntent.getActivity(
            requireContext(), 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(
            requireContext(),
            ChatActivity.channelId
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.nick)
            .setContentText(message.data)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(resultingActivityPendingIntent)

        val remoteInput = RemoteInput.Builder(ChatActivity.NOTIFICATION_REPLY_KEY).setLabel("Reply").build()

        val replyIntent = Intent("gg.strims.android.SEND_NOT_MESSAGE")
        replyIntent.putExtra(
            "gg.strims.android.SEND_MESSAGE_NICK",
            message.nick
        )

        val replyPendingIntent =
            PendingIntent.getBroadcast(
                requireContext(),
                0,
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )

        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_launcher_background,
            "Reply",
            replyPendingIntent
        )
            .addRemoteInput(remoteInput).build()

        notificationBuilder.addAction(action)

        val notificationManager =
            requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                ChatActivity.channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        notificationManager.createNotificationChannel(channel)
        NotificationManagerCompat.from(requireContext()).notify(
            ChatActivity.NOTIFICATION_ID,
            notificationBuilder.build()
        )
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
            viewHolder.itemView.setOnClickListener {
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

    private fun parseChatMessage(input: String): Message? {
        val msg = input.split(" ", limit = 2)
        when (msg[0]) {
            "NAMES" -> {
                val names: NamesMessage = Klaxon().parse(msg[1])!!
                names.users.forEach {
                    CurrentUser.users.add(it.nick)
                }
                if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
                    val lastMessage =
                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
                    lastMessage.state = 1
                    adapter.notifyItemChanged(adapter.itemCount - 1)
                }
                val message = Message(
                    false,
                    "Info",
                    "Connected users: ${names.connectioncount}"
                )
                chatViewModel.addMessage(message)
                progressBarFragment.visibility = View.GONE
                Log.d("TAG", "ENDING BAR ${(System.currentTimeMillis() - CurrentUser.time)}")
                Toast.makeText(
                    requireContext(),
                    "ENDING BAR ${(System.currentTimeMillis() - CurrentUser.time)}",
                    Toast.LENGTH_SHORT
                ).show()
                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
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

                if (CurrentUser.optionsLiveData.value?.ignoreList!!.isNotEmpty()) {
                    CurrentUser.optionsLiveData.value?.ignoreList?.forEach {
                        if (message.nick == it) {
                            return null
                        }
                        if (CurrentUser.optionsLiveData.value?.harshIgnore!!) {
                            if (message.data.contains(it)) {
                                return null
                            }
                        }
                    }
                }

                savePrivateMessage(message)
                requireContext().sendBroadcast(Intent("gg.strims.android.PRIVATE_MESSAGE"))
                return message
            }
            "MSG" -> {
                val message = Klaxon().parse<Message>(msg[1])!!
                if (CurrentUser.optionsLiveData.value?.hideNsfw!! && message.entities.links!!.isNotEmpty()
                    && message.entities.tags!!.isNotEmpty()
                ) {
                    message.entities.tags!!.forEach {
                        if (it.name == "nsfw" || it.name == "nsfl") {
                            return null
                        }
                    }
                }

                if (CurrentUser.optionsLiveData.value?.ignoreList!!.isNotEmpty()) {
                    CurrentUser.optionsLiveData.value?.ignoreList?.forEach {
                        if (message.nick == it) {
                            return null
                        }
                        if (CurrentUser.optionsLiveData.value?.harshIgnore!!) {
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