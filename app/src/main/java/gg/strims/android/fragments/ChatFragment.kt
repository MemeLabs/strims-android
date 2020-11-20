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
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.databinding.FragmentChatBinding
import gg.strims.android.models.Message
import gg.strims.android.models.NamesMessage
import gg.strims.android.models.ViewerState
import gg.strims.android.room.PrivateMessage
import gg.strims.android.viewholders.*
import gg.strims.android.viewmodels.*
import io.ktor.util.*
import kotlinx.android.synthetic.main.autofill_item.view.textViewAutofill
import kotlinx.android.synthetic.main.autofill_item_emote.view.*
import kotlinx.android.synthetic.main.nav_header_main.view.*
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

@KtorExperimentalAPI
@SuppressLint("SetTextI18n")
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
        if (message.nick == "Info") {
            consecutiveMessage = false
        } else if (adapter.itemCount > 0) {
            val item = adapter.getItem(adapter.itemCount - 1)
            if (item.layout == R.layout.chat_message_item || item.layout == R.layout.chat_message_item_consecutive_nick
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
                        isReceived
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
                        streamsViewModel.streams.value!!
                    )
                )
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
                    binding.progressBarFragment.visibility = View.GONE
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
                        val item = adapter.getItem(adapter.itemCount - 2)
                        if (item.layout == R.layout.chat_message_item || item.layout == R.layout.chat_message_item_consecutive_nick
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
                        val recycler = binding.recyclerViewChat
                        val layoutTest =
                            recycler.layoutManager as LinearLayoutManager
                        val lastItem = layoutTest.findLastVisibleItemPosition()
                        if (lastItem >= adapter.itemCount - 3) {
                            recycler.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                } else if (intent.action == "gg.strims.android.PROFILE") {
                    profileViewModel.jwt = intent.getStringExtra("gg.strims.android.JWT")
                    binding.sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
                    val activity = (requireActivity() as MainActivity)
                    activity.binding.navView.navHeaderUsername.text = CurrentUser.user!!.username
                    activity.binding.navView.menu.findItem(R.id.nav_Profile).isVisible = true
                    activity.binding.navView.menu.findItem(R.id.nav_Whispers).isVisible = true
                    activity.binding.navView.setCheckedItem(R.id.nav_Chat)
                    activity.invalidateOptionsMenu()
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
                    streamsViewModel.parseStreams(streams!!)
                } else if (intent.action == "gg.strims.android.SHOWSTREAM") {
                    Log.d("TAG", "GOT SHOW STREAM")
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

                                angelThumpFragment.binding.angelThumpStreamTitle.text = it.title
                                angelThumpFragment.player = exoPlayerViewModel.player
                                angelThumpFragment.binding.angelThumpVideoView.player = exoPlayerViewModel.player
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
        if (lastItem < binding.recyclerViewChat.adapter!!.itemCount - 1) {
            binding.goToBottomLayout.visibility = View.VISIBLE
            binding.goToBottom.isEnabled = true
        } else {
            binding.goToBottomLayout.visibility = View.GONE
            binding.goToBottom.isEnabled = false
        }
    }

    override fun onDestroy() {
        Log.d("TAG", "DESTROYING FRAGMENT")
        requireActivity().unregisterReceiver(broadcastReceiver)
        super.onDestroy()
    }

    override fun onStop() {
        Log.d("TAG", "STOPPING FRAGMENT")
        adapter.sendMessageText = null
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d("TAG", "SAVING")

        if (binding.sendMessageText.text.isNotEmpty()) {
            chatViewModel.currentMessage = binding.sendMessageText.text.toString()
        }

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
        return FragmentChatBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("TAG", "ONVIEWCREATED")
        chatViewModel = ViewModelProvider(requireActivity()).get(ChatViewModel::class.java)
        exoPlayerViewModel =
            ViewModelProvider(requireActivity()).get(ExoPlayerViewModel::class.java)
        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)
        youTubeViewModel = ViewModelProvider(requireActivity()).get(YouTubeViewModel::class.java)
        profileViewModel = ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
        streamsViewModel = ViewModelProvider(requireActivity()).get(StreamsViewModel::class.java)

        chatViewModel.oldMessageCount = adapter.itemCount

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
                    binding.progressBarFragment.visibility = View.GONE
                }
            }
        })

        exoPlayerViewModel.liveDataStream.observe(viewLifecycleOwner, {
            if (it != null &&
                !(childFragmentManager.findFragmentById(R.id.angelthump_fragment)!! as AngelThumpFragment).isVisible) {
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
                binding.sendMessageText.hint = resources.getString(R.string.log_in_to_send_messages)
                binding.progressBarFragment.visibility = View.VISIBLE
                profileViewModel.logOut.value = false
            }
        })

        if (CurrentUser.user != null) {
            binding.sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
        }

        if (savedInstanceState != null) {
            Log.d("TAG", "SAVESTATE NOT NULL")

            if (chatViewModel.currentMessage != null) {
                binding.sendMessageText.setText(chatViewModel.currentMessage)
                chatViewModel.currentMessage = null
            }

            binding.progressBarFragment.visibility = View.GONE
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
            binding.constraintLayoutStreamFragment?.visibility = View.VISIBLE
        }

        binding.sendMessageText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.sendMessageButton.isEnabled = binding.sendMessageText.text.isNotEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                binding.sendMessageButton.isEnabled = binding.sendMessageText.text.isNotEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.sendMessageButton.isEnabled = binding.sendMessageText.text.isNotEmpty()
                autofillAdapter.clear()
                if (binding.sendMessageText.text.isNotEmpty() && binding.sendMessageText.text.last() != ' ') {
                    binding.recyclerViewAutofill.visibility = View.VISIBLE
                    binding.goToBottomLayout.visibility = View.GONE
                    if (binding.sendMessageText.text.first() == '/' && !binding.sendMessageText.text.contains(' ')) {
                        val currentWord = binding.sendMessageText.text.toString().substringAfter('/')

                        commandsArray.forEach {
                            if (it.contains(currentWord, true)) {
                                autofillAdapter.add(AutofillItemCommand(it))
                            }
                        }
                    } else {
                        val currentWord = binding.sendMessageText.text.toString().substringAfterLast(' ')

                        if (currentWord.contains(':')) {
                            CurrentUser.emotes!!.forEach { emote ->
                                if (binding.sendMessageText.text.contains(emote.name)) {
                                    modifiersArray.forEach {
                                        if (it.contains(currentWord.substringAfterLast(':'))) {
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
                } else if (binding.sendMessageText.text.isEmpty()) {
                    binding.recyclerViewAutofill.visibility = View.GONE
                    val layoutTest = binding.recyclerViewChat.layoutManager as LinearLayoutManager
                    val lastItem = layoutTest.findLastVisibleItemPosition()
                    if (lastItem < binding.recyclerViewChat.adapter!!.itemCount - 1) {
                        binding.goToBottomLayout.visibility = View.VISIBLE
                    } else {
                        binding.goToBottomLayout.visibility = View.GONE
                    }
                }
            }
        })

        binding.sendMessageText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                binding.sendMessageButton.performClick()
            }
            true
        }

        binding.sendMessageText.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.sendMessageButton.performClick()
            }
            false
        }

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.recyclerViewChat.layoutManager = layoutManager
        binding.recyclerViewChat.adapter = adapter
        adapter.sendMessageText = binding.sendMessageText

        binding.recyclerViewChat.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutTest = binding.recyclerViewChat.layoutManager as LinearLayoutManager
            val lastItem = layoutTest.findLastVisibleItemPosition()
            if (lastItem < binding.recyclerViewChat.adapter!!.itemCount - 1) {
                binding.goToBottomLayout.visibility = View.VISIBLE
                binding.goToBottom.isEnabled = true
            } else {
                binding.goToBottomLayout.visibility = View.GONE
                binding.goToBottom.isEnabled = false
            }
        }

        binding.recyclerViewChat.itemAnimator = null

        binding.recyclerViewAutofill.adapter = autofillAdapter
        binding.recyclerViewAutofill.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.goToBottom.setOnClickListener {
            binding.recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }

        binding.userListButtonFragment.setOnClickListener {
            if (childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.emote_menu_fragment)!!
                )
            }
            hideKeyboardFrom(requireContext(), binding.sendMessageText)
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

        binding.emoteMenuButtonFragment.setOnClickListener {
            if (childFragmentManager.findFragmentById(R.id.user_list_fragment)!!.isVisible) {
                hideChildFragment(
                    this,
                    childFragmentManager.findFragmentById(R.id.user_list_fragment)!!
                )
            }
            hideKeyboardFrom(requireContext(), binding.sendMessageText)
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

        binding.sendMessageButton.setOnClickListener {
            val messageText = binding.sendMessageText.text.toString()
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
                                binding.sendMessageText.text.clear()
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
                        if (CurrentUser.optionsLiveData.value?.ignoreList!!.contains(nickUnignore)) {
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
                    message = if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(
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
                    message = if (CurrentUser.optionsLiveData.value?.customHighlights!!.contains(
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
                    "MSG {\"data\":\"${binding.sendMessageText.text}\"}"
                )
                requireContext().sendBroadcast(intent)
            }
            binding.sendMessageText.text.clear()
            binding.recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
        }
    }

    fun closeTwitch() {
        val twitchFragment =
            childFragmentManager.findFragmentById(R.id.twitch_fragment)!! as TwitchFragment

        if (twitchFragment.isVisible) {
            with (twitchFragment) {
                binding.webViewTwitch.loadUrl("")
                twitchViewModel.channel.value = null
                twitchViewModel.vod = false
            }

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
            }
        }

        hideChildFragment(this, youTubeFragment)
    }

    private fun closeAngelThump() {
        val angelThumpFragment = childFragmentManager.findFragmentById(R.id.angelthump_fragment)!! as AngelThumpFragment

        exoPlayerViewModel.player?.release()
        exoPlayerViewModel.player = null
        exoPlayerViewModel.liveDataStream.value = null

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

    inner class AutofillItemCommand(private val command: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = "/$command"

            viewHolder.itemView.textViewAutofill.setOnClickListener {

                binding.sendMessageText.setText("/$command ")
                binding.sendMessageText.setSelection(binding.sendMessageText.length())
            }
        }
    }

    inner class AutofillItemUser(private val user: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = user

            viewHolder.itemView.textViewAutofill.setOnClickListener {
                val currentWord = binding.sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage =
                    binding.sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                if (!user.toLowerCase(Locale.ROOT)
                        .contains(currentMessage.toLowerCase(Locale.ROOT))
                ) {
                    binding.sendMessageText.setText("$currentMessage $user ")
                } else {
                    binding.sendMessageText.setText("$user ")
                }
                binding.sendMessageText.setSelection(binding.sendMessageText.length())
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
                val currentWord = binding.sendMessageText.text.toString().substringAfterLast(' ')
                val currentMessage =
                    binding.sendMessageText.text.toString().substringBeforeLast(" $currentWord")
                if (!emote.toLowerCase(Locale.ROOT)
                        .contains(currentMessage.toLowerCase(Locale.ROOT))
                ) {
                    binding.sendMessageText.setText("$currentMessage $emote ")
                } else {
                    binding.sendMessageText.setText("$emote ")
                }
                binding.sendMessageText.setSelection(binding.sendMessageText.length())
            }
        }
    }

    inner class AutofillItemModifier(private val modifier: String) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int = R.layout.autofill_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.textViewAutofill.text = modifier

            viewHolder.itemView.textViewAutofill.setOnClickListener {
                var currentWord = binding.sendMessageText.text.toString().substringAfterLast(':')
                if (currentWord.isEmpty()) {
                    currentWord = ":"
                }
                var currentMessage =
                    binding.sendMessageText.text.toString().substringBeforeLast(currentWord)

                if (currentMessage.last() == ' ') {
                    currentMessage = currentMessage.trimEnd(' ')
                }

                if (currentMessage.last() != ':') {
                    currentMessage = currentMessage.plus(':')
                }
                binding.sendMessageText.setText("${currentMessage}${modifier} ")
                binding.sendMessageText.setSelection(binding.sendMessageText.length())
            }
        }
    }

    private fun savePrivateMessage(message: Message) {
        if (CurrentUser.user != null) {
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
    }

    private fun parseChatMessage(input: String): Message? {
        val msg = input.split(" ", limit = 2)
        when (msg[0]) {
            "NAMES" -> {
                val names: NamesMessage = Klaxon().parse(msg[1])!!
                names.users.forEach {
                    chatViewModel.users.add(it.nick)
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
                Toast.makeText(
                    requireContext(),
                    "ENDING BAR ${(System.currentTimeMillis() - CurrentUser.time)}",
                    Toast.LENGTH_SHORT
                ).show()
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