package gg.strims.android.viewholders

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.R
import gg.strims.android.adapters.CustomAdapter
import gg.strims.android.utils.createMessageTextView
import gg.strims.android.databinding.ChatMessageItemBinding
import gg.strims.android.utils.keyRequestFocus
import gg.strims.android.models.Message
import gg.strims.android.models.Stream
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class ChatMessage(
    var adapter: CustomAdapter,
    val messageData: Message,
    private var streams: MutableList<Stream>,
) :
    BindableItem<ChatMessageItemBinding>() {

    override fun getLayout(): Int = R.layout.chat_message_item

    override fun bind(viewBinding: ChatMessageItemBinding, position: Int) {
        with (viewBinding) {
            if (adapter.viewerStates != null && CurrentUser.optionsLiveData.value?.showViewerState!!) {
                var changed = false
                adapter.viewerStates!!.forEach {
                    if (it.nick == messageData.nick) {
                        streams.forEach { stream ->
                            if (it.channel?.channel == stream.channel) {
                                viewerStateChatMessage.visibility = View.VISIBLE
                                viewerStateChatMessage.setColorFilter(stream.colour)
                                changed = true
                                return@forEach
                            }
                        }
                    }
                }
                if (!changed) {
                    viewerStateChatMessage.visibility = View.GONE
                }
            } else if (layout == R.layout.chat_message_item && !CurrentUser.optionsLiveData.value?.showViewerState!!) {
                viewerStateChatMessage.visibility = View.GONE
            }

            if (CurrentUser.optionsLiveData.value?.showTime!!) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val time = dateFormat.format(messageData.timestamp)
                if (CurrentUser.optionsLiveData.value?.showTime!!) {
                    timestampChatMessage.visibility = View.VISIBLE
                } else {
                    timestampChatMessage.visibility = View.GONE
                }
                timestampChatMessage.text = time
            }

            if (CurrentUser.user != null) {
                if (CurrentUser.user!!.username == messageData.nick) {
                    root.setBackgroundColor(Color.parseColor("#1A1A1A"))
                } else if (messageData.data.toLowerCase(Locale.getDefault()).contains(
                        CurrentUser.user!!.username.toLowerCase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    root.setBackgroundColor(Color.parseColor("#001D36"))
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                        CurrentUser.user!!.username
                    )
                ) {
                    root.setBackgroundColor(Color.parseColor("#111111"))
                }
            } else {
                if (messageData.data.contains("anonymous")) {
                    root.setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    root.setBackgroundColor(Color.parseColor("#111111"))
                }
            }

            if (CurrentUser.optionsLiveData.value?.customHighlights!!.isNotEmpty()) {
                CurrentUser.optionsLiveData.value?.customHighlights?.forEach {
                    if (messageData.nick == it) {
                        root.setBackgroundColor(Color.parseColor("#001D36"))
                        return@forEach
                    }
                }
            }

            if (messageData.features.contains("bot") || messageData.nick == "Info") {
                usernameChatMessage.setTextColor(Color.parseColor("#FF2196F3"))
                botFlairChatMessage.visibility = View.VISIBLE
            } else {
                usernameChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
                botFlairChatMessage.visibility = View.GONE
            }

            root.alpha = if (adapter.tempHighlightNick != null) {
                when {
                    adapter.tempHighlightNick!!.contains(messageData.nick) -> {
                        1f
                    }
                    adapter.tempHighlightNick!!.isEmpty() -> {
                        1f
                    }
                    else -> {
                        0.5f
                    }
                }
            } else {
                1f
            }

            usernameChatMessage.text =
                root.context.resources.getString(R.string.chat_message_username, messageData.nick)

            messageChatMessage.movementMethod = LinkMovementMethod.getInstance()

            createMessageTextView(root.context, messageData, messageChatMessage)

            usernameChatMessage.setOnClickListener {
                if (adapter.tempHighlightNick == null) {
                    adapter.tempHighlightNick = mutableListOf()
                }
                adapter.tempHighlightNick!!.add(messageData.nick)
                adapter.notifyDataSetChanged()
            }

            if (adapter.sendMessageText != null) {
                usernameChatMessage.setOnLongClickListener {
                    val wrapper = ContextThemeWrapper(root.context, R.style.PopupMenu)
                    val pop = PopupMenu(wrapper, it)
                    pop.inflate(R.menu.chat_message_username_menu)
                    pop.setOnMenuItemClickListener { itMenuItem ->
                        when (itMenuItem.itemId) {
                            R.id.chatWhisper -> {
                                adapter.sendMessageText!!.setText(
                                    root.context.resources.getString(
                                        R.string.chat_whisper_popup,
                                        messageData.nick
                                    )
                                )
                                keyRequestFocus(adapter.sendMessageText!!, root.context)
                                adapter.sendMessageText!!.setSelection(adapter.sendMessageText!!.text.length)
                            }
                            R.id.chatMention -> {
                                val currentMessage = adapter.sendMessageText!!.text.toString()
                                if (currentMessage.isNotEmpty()) {
                                    if (currentMessage.last() == ' ') {
                                        adapter.sendMessageText!!.setText(currentMessage.plus("${messageData.nick} "))
                                    } else {
                                        adapter.sendMessageText!!.setText(currentMessage.plus(" ${messageData.nick} "))
                                    }
                                } else {
                                    adapter.sendMessageText!!.setText(
                                        root.context.resources.getString(
                                            R.string.chat_mention_popup, messageData.nick
                                        )
                                    )
                                }
                                keyRequestFocus(adapter.sendMessageText!!, root.context)
                                adapter.sendMessageText!!.setSelection(adapter.sendMessageText!!.text.length)
                            }
                            R.id.chatIgnore -> {
                                CurrentUser.addIgnore(messageData.nick)
                                adapter.notifyDataSetChanged()
                            }
                        }
                        true
                    }
                    pop.show()
                    true
                }
            }

            if (messageData.entities.spoilers!!.isEmpty()) {
                messageChatMessage.setOnClickListener {
                    adapter.tempHighlightNick = null
                    adapter.notifyDataSetChanged()
                }
            } else {
                messageChatMessage.setOnClickListener(null)
            }

            root.setOnClickListener {
                adapter.tempHighlightNick = null
                adapter.notifyDataSetChanged()
            }
        }
    }

    fun isNickSame(nick: String): Boolean {
        return messageData.nick == nick
    }

    override fun initializeViewBinding(view: View): ChatMessageItemBinding {
        return ChatMessageItemBinding.bind(view)
    }
}