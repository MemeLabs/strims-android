package gg.strims.android.viewholders

import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.R
import gg.strims.android.adapters.CustomAdapter
import gg.strims.android.createMessageTextView
import gg.strims.android.keyRequestFocus
import gg.strims.android.models.Message
import gg.strims.android.models.Stream
import io.ktor.util.*
import kotlinx.android.synthetic.main.chat_message_item.view.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class ChatMessage(
    private val context: Context,
    var adapter: CustomAdapter,
    val messageData: Message,
    private val isConsecutive: Boolean = false,
    private var streams: MutableList<Stream>,
) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        if (isConsecutive) {
            return R.layout.chat_message_item_consecutive_nick
        }
        return R.layout.chat_message_item
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        with (viewHolder.itemView) {
            if (adapter.viewerStates != null && layout == R.layout.chat_message_item && CurrentUser.optionsLiveData.value?.showViewerState!!) {
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
                    setBackgroundColor(Color.parseColor("#1A1A1A"))
                } else if (messageData.data.toLowerCase(Locale.getDefault()).contains(
                        CurrentUser.user!!.username.toLowerCase(
                            Locale.getDefault()
                        )
                    )
                ) {
                    setBackgroundColor(Color.parseColor("#001D36"))
                } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                        CurrentUser.user!!.username
                    )
                ) {
                    setBackgroundColor(Color.parseColor("#111111"))
                }
            } else {
                if (messageData.data.contains("anonymous")) {
                    setBackgroundColor(Color.parseColor("#001D36"))
                } else {
                    setBackgroundColor(Color.parseColor("#111111"))
                }
            }

            if (CurrentUser.optionsLiveData.value?.customHighlights!!.isNotEmpty()) {
                CurrentUser.optionsLiveData.value?.customHighlights?.forEach {
                    if (messageData.nick == it) {
                        setBackgroundColor(Color.parseColor("#001D36"))
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

            alpha = if (adapter.tempHighlightNick != null) {
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
                context.resources.getString(R.string.chat_message_username, messageData.nick)

            messageChatMessage.movementMethod = LinkMovementMethod.getInstance()

            createMessageTextView(context, messageData, messageChatMessage)

            usernameChatMessage.setOnClickListener {
                if (adapter.tempHighlightNick == null) {
                    adapter.tempHighlightNick = mutableListOf()
                }
                adapter.tempHighlightNick!!.add(messageData.nick)
                adapter.notifyDataSetChanged()
            }

            if (adapter.sendMessageText != null) {
                usernameChatMessage.setOnLongClickListener {
                    val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
                    val pop = PopupMenu(wrapper, it)
                    pop.inflate(R.menu.chat_message_username_menu)
                    pop.setOnMenuItemClickListener { itMenuItem ->
                        when (itMenuItem.itemId) {
                            R.id.chatWhisper -> {
                                adapter.sendMessageText!!.setText(
                                    context.resources.getString(
                                        R.string.chat_whisper_popup,
                                        messageData.nick
                                    )
                                )
                                keyRequestFocus(adapter.sendMessageText!!, context)
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
                                        context.resources.getString(
                                            R.string.chat_mention_popup, messageData.nick
                                        )
                                    )
                                }
                                keyRequestFocus(adapter.sendMessageText!!, context)
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
            }

            setOnClickListener {
                adapter.tempHighlightNick = null
                adapter.notifyDataSetChanged()
            }

            if (isConsecutive) {
                usernameChatMessage.visibility = View.GONE
                botFlairChatMessage.visibility = View.GONE
            }
        }
    }

    fun isNickSame(nick: String): Boolean {
        return messageData.nick == nick
    }
}