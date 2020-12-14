package gg.strims.android.fragments

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
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.beust.klaxon.Klaxon
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.*
import gg.strims.android.adapters.CustomAdapter
import gg.strims.android.databinding.AutofillItemBinding
import gg.strims.android.databinding.AutofillItemEmoteBinding
import gg.strims.android.databinding.FragmentChatBinding
import gg.strims.android.models.Message
import gg.strims.android.models.NamesMessage
import gg.strims.android.models.ViewerState
import gg.strims.android.room.PrivateMessage
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.utils.hideChildFragment
import gg.strims.android.utils.hideKeyboardFrom
import gg.strims.android.utils.showChildFragment
import gg.strims.android.utils.viewBinding
import gg.strims.android.viewholders.*
import gg.strims.android.viewmodels.*
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import pl.droidsonroids.gif.GifDrawable
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

@KtorExperimentalAPI
class ChatFragment : Fragment() {

    val binding by viewBinding(FragmentChatBinding::bind)

    var adapter = CustomAdapter()
    private val autofillAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var commandsArray: Array<String>
    private lateinit var modifiersArray: Array<String>

    private lateinit var chatViewModel: ChatViewModel
    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel
    private lateinit var exoPlayerViewModel: ExoPlayerViewModel
    private lateinit var twitchViewModel: TwitchViewModel
    private lateinit var youTubeViewModel: YouTubeViewModel
    private lateinit var profileViewModel: ProfileViewModel
    private lateinit var streamsViewModel: StreamsViewModel

    private var missedMessages = mutableListOf<Message>()

    private fun printMessage(message: Message) {
        var consecutiveMessage = false
        /** Check for consecutive message **/
        if (adapter.itemCount > 0 && message.nick != "Info") {
            val item = adapter.getItem(adapter.itemCount - 1)
            if (item.layout == R.layout.chat_message_item) {
                val lastMessage =
                    adapter.getItem(adapter.itemCount - 1) as ChatMessage
                consecutiveMessage =
                    lastMessage.isNickSame(message.nick)
            } else if (item.layout == R.layout.chat_message_item_consecutive_nick) {
                val lastMessage =
                    adapter.getItem(adapter.itemCount - 1) as ConsecutiveChatMessage
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
                        adapter,
                        message,
                        isReceived
                    )
                )
                if (CurrentUser.optionsLiveData.value?.notifications!! && message.nick != CurrentUser.user!!.username) {
                    displayNotification(message)
                }
            } else {
                if (!consecutiveMessage) {
                    adapter.add(
                        ChatMessage(
                            adapter,
                            message,
                            streamsViewModel.streams.value!!
                        )
                    )
                } else {
                    adapter.add(
                        ConsecutiveChatMessage(
                            adapter,
                            message
                        )
                    )
                }
            }
        }
    }

    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            val parentActivity = requireActivity() as MainActivity
            if (intent != null) {
                if (intent.action == "gg.strims.android.MESSAGE_HISTORY" && adapter.itemCount <= 1) {
                    Log.d(
                        "TAG",
                        "STARTING PARSING ${(System.currentTimeMillis() - CurrentUser.time)}"
                    )
                    val messageHistory =
                        intent.getStringArrayListExtra("gg.strims.android.MESSAGE_HISTORY_TEXT")
                    val viewerStates =
                        intent.getParcelableArrayListExtra<ViewerState>("gg.strims.android.VIEWERSTATES")
                    if (viewerStates != null) {
                        chatViewModel.viewerStates.value = viewerStates.toMutableList()
                    }
                    messageHistory?.forEach {
                        val message = parseChatMessage(it)
                        if (message != null) {
                            chatViewModel.addMessage(message)
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
                    if (adapter.itemCount > 1 && message != null) {
                        val item = adapter.getItem(adapter.itemCount - 2)
                        if (item.layout == R.layout.chat_message_item) {
                            val lastMessage =
                                adapter.getItem(adapter.itemCount - 2) as ChatMessage
                            if (lastMessage.messageData.nick == message.nick && lastMessage.messageData.data == message.data) {
                                return
                            }
                        } else if (item.layout == R.layout.chat_message_item_consecutive_nick) {
                            val lastMessage =
                                adapter.getItem(adapter.itemCount - 2) as ConsecutiveChatMessage
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
                        if (view != null) {
                            val recycler = binding.recyclerViewChat
                            val layoutTest =
                                recycler.layoutManager as LinearLayoutManager
                            val lastItem = layoutTest.findLastVisibleItemPosition()
                            if (lastItem >= adapter.itemCount - 3) {
                                recycler.scrollToPosition(adapter.itemCount - 1)
                            }
                        }
                    }
                } else if (intent.action == "gg.strims.android.PROFILE") {
                    profileViewModel.jwt = intent.getStringExtra("gg.strims.android.JWT")
                    binding.sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    val activity = (requireActivity() as MainActivity)
                    with (activity.binding.navView) {
                        navHeaderUsername.text = CurrentUser.user!!.username
                        menu.findItem(R.id.nav_Profile).isVisible = true
                        menu.findItem(R.id.nav_Whispers).isVisible = true
                        setCheckedItem(R.id.nav_Chat)
                    }
                    activity.invalidateOptionsMenu()
                } else if (intent.action == "gg.strims.android.CHAT_SOCKET_CLOSE") {
                    val message = Message(
                        false,
                        "Info",
                        "Disconnected, reconnecting..."
                    )
                    chatViewModel.addMessage(message)
                    parentActivity.restartChatService()
                } else if (intent.action == "gg.strims.android.STREAMS_SOCKET_CLOSE") {
                    parentActivity.restartStreamsService()
                } else if (intent.action == "gg.strims.android.STREAMS") {
                    val streams = intent.getStringExtra("gg.strims.android.STREAMS_TEXT")
                    if (::streamsViewModel.isInitialized) {
                        streamsViewModel.parseStreams(streams!!)
                    }
                } else if (intent.action == "gg.strims.android.SHOWSTREAM") {
                    val channel = intent.getStringExtra("gg.strims.android.STREAM")

                    streamsViewModel.streams.value?.forEach {
                        if (it.channel == channel && it.service == "angelthump") {
                            closeYouTube()
                            closeTwitch()

                            val angelThumpFragment =
                                childFragmentManager.findFragmentById(R.id.angelthump_fragment)!! as AngelThumpFragment

                            exoPlayerViewModel.liveDataStream.value = it

                            if (angelThumpFragment.isVisible) {
                                with (exoPlayerViewModel) {
                                    player?.release()
                                    player = SimpleExoPlayer.Builder(requireView().context).build()
                                    player?.addMediaItem(MediaItem.fromUri("https://video-cdn.angelthump.com/hls/${channel}/index.m3u8".toUri()))
                                    player?.prepare()
                                    player?.play()
                                    currentlyPlaying = channel
                                }

                                with (angelThumpFragment) {
                                    binding.angelThumpStreamTitle.text = it.title
                                    player = exoPlayerViewModel.player
                                    binding.angelThumpVideoView.player = exoPlayerViewModel.player
                                }
                            }
                            return
                        }
                    }
                    Toast.makeText(requireContext(), "Stream not found", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private var scrollUponResume = false

    override fun onPause() {
        if (binding.sendMessageText.text.isNotEmpty()) {
            chatViewModel.currentMessage = binding.sendMessageText.text.toString()
        }

        val layoutTest = binding.recyclerViewChat.layoutManager as LinearLayoutManager
        val lastItem = layoutTest.findLastVisibleItemPosition()
        if (lastItem < binding.recyclerViewChat.adapter!!.itemCount - 1) {
            scrollUponResume = false
        } else {
            binding.goToBottomLayout.visibility = View.GONE
            scrollUponResume = true
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        if (scrollUponResume) {
            binding.recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        val layoutTest = binding.recyclerViewChat.layoutManager as LinearLayoutManager
        val lastItem = layoutTest.findLastVisibleItemPosition()
        with (binding) {
            if (lastItem < binding.recyclerViewChat.adapter!!.itemCount - 1) {
                goToBottomLayout.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottomLayout.visibility = View.GONE
                goToBottom.isEnabled = false
            }
        }
    }

    override fun onDestroy() {
        Log.d("TAG", "DESTROYING CHAT FRAGMENT")
        requireActivity().unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onDestroyView() {
        Log.d("TAG", "DESTROYING CHAT FRAGMENT VIEW")
        recyclerViewChat.adapter = null
        recyclerViewAutofill.adapter = null
        super.onDestroyView()
    }

    override fun onStop() {
        Log.d("TAG", "STOPPING CHAT FRAGMENT")
        adapter.sendMessageText = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("TAG", "SAVING CHAT FRAGMENT")
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("TAG", "ONCREATE CHAT FRAGMENT")

        modifiersArray = resources.getStringArray(R.array.modifiersArray)
        commandsArray = resources.getStringArray(R.array.commandsArray)

        chatViewModel = ViewModelProvider(requireActivity()).get(ChatViewModel::class.java)
        privateMessagesViewModel =
            ViewModelProvider(requireActivity()).get(PrivateMessagesViewModel::class.java)
        exoPlayerViewModel =
            ViewModelProvider(requireActivity()).get(ExoPlayerViewModel::class.java)
        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)
        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
        streamsViewModel = ViewModelProvider(requireActivity()).get(StreamsViewModel::class.java)

        val intentFilter = IntentFilter()
        with (intentFilter) {
            addAction("gg.strims.android.MESSAGE")
            addAction("gg.strims.android.MESSAGE_HISTORY")
            addAction("gg.strims.android.PROFILE")
            addAction("gg.strims.android.CHAT_SOCKET_CLOSE")
            addAction("gg.strims.android.STREAMS_SOCKET_CLOSE")
            addAction("gg.strims.android.STREAMS")
            addAction("gg.strims.android.SHOWSTREAM")
        }
        requireActivity().registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("TAG", "ONCREATEVIEW CHAT FRAGMENT")
        return FragmentChatBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with (binding) {
            Log.d("TAG", "ONVIEWCREATED CHAT FRAGMENT")
            if (savedInstanceState != null || adapter.itemCount != 0) {
                progressBarFragment.visibility = View.GONE
                chatViewModel.oldMessageCount = 0
            }

            chatViewModel.viewerStates.observe(viewLifecycleOwner, {
                if (it.isNotEmpty()) {
                    adapter.viewerStates = it
                }
            })

            chatViewModel.messages.observe(viewLifecycleOwner, {
                if (it != null) {
                    if (it.size > chatViewModel.oldMessageCount) {
                        val diff = it.size - chatViewModel.oldMessageCount
                        for (i in 0 until diff) {
                            printMessage(it[chatViewModel.oldMessageCount + i])
                        }
                        chatViewModel.oldMessageCount = chatViewModel.oldMessageCount + diff
                        progressBarFragment.visibility = View.GONE
                    }
                }
            })

            exoPlayerViewModel.liveDataStream.observe(viewLifecycleOwner, {
                if (it != null &&
                    !(childFragmentManager.findFragmentById(R.id.angelthump_fragment)!! as AngelThumpFragment).isVisible
                ) {
                    closeYouTube()
                    closeTwitch()
                    val fragment = childFragmentManager.findFragmentById(R.id.angelthump_fragment)
                    showChildFragment(this@ChatFragment, fragment!!)
                }
            })

            twitchViewModel.channel.observe(viewLifecycleOwner, {
                if (it != null) {
                    closeAngelThump()
                    closeYouTube()
                    val fragment = childFragmentManager.findFragmentById(R.id.twitch_fragment)
                    showChildFragment(this@ChatFragment, fragment!!)
                }
            })

            youTubeViewModel.videoId.observe(viewLifecycleOwner, {
                if (it != null) {
                    closeAngelThump()
                    closeTwitch()
                    val fragment = childFragmentManager.findFragmentById(R.id.youtube_fragment)
                    showChildFragment(this@ChatFragment, fragment!!)
                }
            })

            profileViewModel.logOut.observe(viewLifecycleOwner, {
                if (it) {
                    sendMessageText.hint =
                        resources.getString(R.string.log_in_to_send_messages)
                    progressBarFragment.visibility = View.VISIBLE
                    profileViewModel.logOut.value = false
                }
            })

            if (chatViewModel.currentMessage != null) {
                sendMessageText.setText(chatViewModel.currentMessage)
                chatViewModel.currentMessage = null
            }

            if (CurrentUser.user != null) {
                sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
            }

            if (missedMessages.isNotEmpty()) {
                missedMessages.forEach {
                    chatViewModel.addMessage(it)
                }
                missedMessages.clear()
            }

            if ((exoPlayerViewModel.liveDataStream.value != null ||
                        twitchViewModel.channel.value != null ||
                        youTubeViewModel.videoId.value != null)
                && resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            ) {
                constraintLayoutStreamFragment?.visibility = View.VISIBLE
            }

            sendMessageText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    sendMessageButton.isEnabled = sendMessageText.text.isNotEmpty()
                    chatViewModel.currentMessage = sendMessageText.text.toString()
                    autofillAdapter.clear()
                    if (sendMessageText.text.isNotEmpty() && sendMessageText.text.last() != ' ') {
                        recyclerViewAutofill.visibility = View.VISIBLE
                        goToBottomLayout.visibility = View.GONE
                        if (sendMessageText.text.first() == '/' && !sendMessageText.text.contains(
                                ' '
                            )
                        ) {
                            val currentWord =
                                sendMessageText.text.toString().substringAfter('/')

                            commandsArray.forEach {
                                if (it.startsWith(currentWord, true)) {
                                    autofillAdapter.add(AutofillItemCommand(it))
                                }
                            }
                        } else {
                            val currentWord =
                                sendMessageText.text.toString().substringAfterLast(' ')

                            if (currentWord.contains(':')) {
                                CurrentUser.emotes!!.forEach { emote ->
                                    if (sendMessageText.text.contains(emote.name)) {
                                        modifiersArray.forEach {
                                            if (it.startsWith(currentWord.substringAfterLast(':'))) {
                                                autofillAdapter.add(AutofillItemModifier(it))
                                            }
                                        }
                                        return@forEach
                                    }
                                }
                            }

                            chatViewModel.users.sortByDescending {
                                it
                            }

                            chatViewModel.users.forEach {
                                if (it.startsWith(currentWord, true)) {
                                    autofillAdapter.add(AutofillItemUser(it))
                                }
                            }

                            CurrentUser.bitmapMemoryCache.forEach {
                                if (it.key.startsWith(currentWord, true)) {
                                    autofillAdapter.add(AutofillItemEmote(it.key, it.value))
                                }
                            }

                            CurrentUser.gifMemoryCache.forEach {
                                if (it.key.startsWith(currentWord, true)) {
                                    autofillAdapter.add(AutofillItemEmote(it.key, it.value))
                                }
                            }
                        }
                    } else if (sendMessageText.text.isEmpty()) {
                        recyclerViewAutofill.visibility = View.GONE
                        val layoutTest =
                            recyclerViewChat.layoutManager as LinearLayoutManager
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
            adapter.sendMessageText = sendMessageText

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
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                    )
                }
                hideKeyboardFrom(requireContext(), sendMessageText)
                if (childFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isVisible) {
                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                    )
                } else {
                    showChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                    )
                }
            }

            emoteMenuButtonFragment.setOnClickListener {
                if (childFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isVisible) {
                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                    )
                }
                hideKeyboardFrom(requireContext(), sendMessageText)
                if (childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!.isVisible) {
                    hideChildFragment(
                        this@ChatFragment,
                        childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                    )
                } else {
                    showChildFragment(
                        this@ChatFragment,
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
                                messageText.substringAfter("$privateMessageCommand ")
                                    .substringBefore(
                                        ' '
                                    )
                            val nickRegex = "^[A-Za-z0-9_]{3,20}$"
                            val p: Pattern = Pattern.compile(nickRegex)
                            val m: Matcher = p.matcher(nick)

                            if (!m.find()) {
                                adapter.add(ErrorChatMessage("Invalid nick - /$privateMessageCommand nick message"))
                            } else {
                                var message =
                                    messageText.substringAfter("$privateMessageCommand $nick")
                                message = message.substringAfter(" ")
                                if (message.trim() == "") {
                                    adapter.add(ErrorChatMessage("The message was invalid"))
                                    sendMessageText.text.clear()
                                    chatViewModel.currentMessage = null
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
                        message =
                            if (CurrentUser.optionsLiveData.value?.ignoreList!!.contains(
                                    nickUnignore
                                )
                            ) {
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
                        message =
                            if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(
                                    nickHighlight
                                )
                            ) {
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
                        message =
                            if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(
                                    nickUnhighlight
                                )
                            ) {
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
                chatViewModel.currentMessage = null
                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
            }
        }
    }

    fun closeTwitch() {
        val twitchFragment =
            childFragmentManager.findFragmentById(R.id.twitch_fragment)!! as TwitchFragment

        if (twitchFragment.isVisible) {
            twitchFragment.binding.webViewTwitch.loadUrl("")
            twitchViewModel.channel.value = null
            twitchViewModel.vod = false

            hideChildFragment(this@ChatFragment, twitchFragment)
        }
    }

    fun closeYouTube() {
        val youTubeFragment =
            childFragmentManager.findFragmentById(R.id.youtube_fragment)!! as YouTubeFragment

        if (youTubeFragment.isVisible) {
            with (youTubeFragment) {
                binding.youTubeView.getYouTubePlayerWhenReady(object :
                    YouTubePlayerCallback {
                    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.pause()
                    }
                })

                youTubeViewModel.videoId.value = null
                youTubeViewModel.currentPosition = null
            }
        }

        hideChildFragment(this, youTubeFragment)
    }

    private fun closeAngelThump() {
        val angelThumpFragment = childFragmentManager.findFragmentById(R.id.angelthump_fragment)!! as AngelThumpFragment

        with (exoPlayerViewModel) {
            player?.release()
            player = null
            liveDataStream.value = null
        }

        hideChildFragment(this, angelThumpFragment)
    }

    private fun displayNotification(message: Message) {
        val activityIntent = Intent(requireContext(), MainActivity::class.java)
        val resultingActivityPendingIntent = PendingIntent.getActivity(
            requireContext(), 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(
            requireContext(),
            MainActivity.channelId
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(message.nick)
            .setContentText(message.data)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(resultingActivityPendingIntent)

        val remoteInput =
            RemoteInput.Builder(MainActivity.NOTIFICATION_REPLY_KEY).setLabel("Reply").build()

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
                MainActivity.channelId,
                "Chat Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        notificationManager.createNotificationChannel(channel)
        NotificationManagerCompat.from(requireContext()).notify(
            MainActivity.NOTIFICATION_ID,
            notificationBuilder.build()
        )
    }

    inner class AutofillItemCommand(private val command: String) :
        BindableItem<AutofillItemBinding>() {

        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewBinding: AutofillItemBinding, position: Int) {
            with (viewBinding) {
                textViewAutofill.text =
                    resources.getString(R.string.autofill_whisper_command1, command)

                textViewAutofill.setOnClickListener {
                    binding.sendMessageText.setText(
                        resources.getString(
                            R.string.autofill_whisper_command2,
                            command
                        )
                    )
                    binding.sendMessageText.setSelection(binding.sendMessageText.length())
                }
            }
        }

        override fun initializeViewBinding(view: View): AutofillItemBinding {
            return AutofillItemBinding.bind(view)
        }
    }

    inner class AutofillItemUser(private val user: String) : BindableItem<AutofillItemBinding>() {

        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewBinding: AutofillItemBinding, position: Int) {
            with (viewBinding) {
                textViewAutofill.text = user

                textViewAutofill.setOnClickListener {
                    val currentWord =
                        binding.sendMessageText.text.toString().substringAfterLast(' ')
                    val currentMessage =
                        binding.sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                    if (!user.toLowerCase(Locale.getDefault())
                            .contains(currentMessage.toLowerCase(Locale.getDefault()))
                    ) {
                        binding.sendMessageText.setText(
                            resources.getString(
                                R.string.autofill_user_item1,
                                currentMessage,
                                user
                            )
                        )
                    } else {
                        binding.sendMessageText.setText(
                            resources.getString(
                                R.string.autofill_user_item2,
                                user
                            )
                        )
                    }
                    binding.sendMessageText.setSelection(binding.sendMessageText.length())
                }
            }
        }

        override fun initializeViewBinding(view: View): AutofillItemBinding {
            return AutofillItemBinding.bind(view)
        }
    }

    inner class AutofillItemEmote<T>(private val name: String, private val emote: T) :
        BindableItem<AutofillItemEmoteBinding>() {

        override fun getLayout(): Int = R.layout.autofill_item_emote

        override fun bind(viewBinding: AutofillItemEmoteBinding, position: Int) {
            with (viewBinding) {
                textViewAutofill.text = name
                if (emote is Bitmap) {
                    imageViewEmoteAutofill.setImageBitmap(emote)
                } else if (emote is GifDrawable) {
                    imageViewEmoteAutofill.setImageDrawable(emote as GifDrawable)
                }
                root.setOnClickListener {
                    val currentWord =
                        binding.sendMessageText.text.toString().substringAfterLast(' ')
                    val currentMessage =
                        binding.sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                    if (!name.toLowerCase(Locale.getDefault())
                            .contains(currentMessage.toLowerCase(Locale.getDefault()))
                    ) {
                        binding.sendMessageText.setText(
                            resources.getString(
                                R.string.autofill_user_item1,
                                currentMessage,
                                name
                            )
                        )
                    } else {
                        binding.sendMessageText.setText(
                            resources.getString(
                                R.string.autofill_user_item2,
                                name
                            )
                        )
                    }
                    binding.sendMessageText.setSelection(binding.sendMessageText.length())
                }
            }
        }

        override fun initializeViewBinding(view: View): AutofillItemEmoteBinding {
            return AutofillItemEmoteBinding.bind(view)
        }
    }

    inner class AutofillItemModifier(private val modifier: String) :
        BindableItem<AutofillItemBinding>() {

        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewBinding: AutofillItemBinding, position: Int) {
            with (viewBinding) {
                textViewAutofill.text = modifier

                textViewAutofill.setOnClickListener {
                    var currentWord =
                        binding.sendMessageText.text.toString().substringAfterLast(':')
                    if (currentWord.isEmpty()) {
                        currentWord = ":"
                    }
                    var currentMessage =
                        binding.sendMessageText.text.toString().substringBeforeLast(":$currentWord")

                    if (currentMessage.last() == ' ') {
                        currentMessage = currentMessage.trimEnd(' ')
                    }

                    if (currentMessage.last() != ':') {
                        currentMessage = currentMessage.plus(':')
                    }
                    binding.sendMessageText.setText(
                        resources.getString(
                            R.string.autofill_item_modifier,
                            currentMessage,
                            modifier
                        )
                    )
                    binding.sendMessageText.setSelection(binding.sendMessageText.length())
                }
            }
        }

        override fun initializeViewBinding(view: View): AutofillItemBinding {
            return AutofillItemBinding.bind(view)
        }
    }

    private fun savePrivateMessage(message: Message) {
        if (CurrentUser.user != null) {
            with (message) {
                privateMessagesViewModel.addMessage(
                    PrivateMessage(
                        0,
                        privMsg,
                        nick,
                        data,
                        timestamp,
                        features,
                        entities,
                        targetNick!!
                    )
                )
            }
        }
    }

    private fun parseChatMessage(input: String): Message? {
        val msg = input.split(" ", limit = 2)
        when (msg[0]) {
            "NAMES" -> {
                val names: NamesMessage = Klaxon().parse(msg[1])!!
                names.users.forEach {
                    if (!chatViewModel.users.contains(it.nick)) {
                        chatViewModel.users.add(it.nick)
                    }
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
                binding.progressBarFragment.visibility = View.GONE
                Log.d("TAG", "ENDING BAR ${(System.currentTimeMillis() - CurrentUser.time)}")
                binding.recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
            }
            "JOIN" -> {
                val userJoin = Klaxon().parse<String>(msg[1])
                if (!chatViewModel.users.contains(userJoin)) {
                    chatViewModel.users.add(userJoin!!)
                }
            }
            "QUIT" -> {
                val userQuit = Klaxon().parse<String>(msg[1])
                if (chatViewModel.users.contains(userQuit)) {
                    chatViewModel.users.remove(userQuit)
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
                    binding.recyclerViewChat.layoutManager as LinearLayoutManager
                val lastItem = layoutTest.findLastVisibleItemPosition()
                if (lastItem >= binding.recyclerViewChat.adapter!!.itemCount - 3) {
                    binding.recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                }
            }
            "VIEWERSTATE" -> {
                val state = Klaxon().parse<ViewerState>(msg[1])
                chatViewModel.viewerStates.value?.forEach {
                    if (it.nick == state?.nick) {
                        if (chatViewModel.viewerStates.value != null) {
                            chatViewModel.updateViewerState(it, state)
                        }
                        return null
                    }
                }
            }
        }
        return null
    }
}