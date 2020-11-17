package gg.strims.android.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Message
import io.ktor.util.*
import kotlinx.android.synthetic.main.chat_message_item.view.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
@SuppressLint("SimpleDateFormat", "SetTextI18n")
class ChatMessage(
    private val context: Context,
    var adapter: GroupAdapter<GroupieViewHolder>?,
    val messageData: Message,
    private val isConsecutive: Boolean = false,
    private var sendMessageText: EditText? = null
) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        if (isConsecutive) {
            return R.layout.chat_message_item_consecutive_nick
        }
        return R.layout.chat_message_item
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (CurrentUser.viewerStates != null && layout == R.layout.chat_message_item && CurrentUser.optionsLiveData.value?.showViewerState!!) {
            var changed = false
            CurrentUser.viewerStates!!.forEach {
                if (it.nick == messageData.nick) {
                    CurrentUser.streams?.forEach { stream ->
                        if (it.channel?.channel == stream.channel) {
                            viewHolder.itemView.viewerStateChatMessage.visibility = View.VISIBLE
                            viewHolder.itemView.viewerStateChatMessage.setColorFilter(stream.colour)
                            changed = true
                            return@forEach
                        }
                    }
                }
            }
            if (!changed) {
                viewHolder.itemView.viewerStateChatMessage.visibility = View.GONE
            }
        } else if (layout == R.layout.chat_message_item && !CurrentUser.optionsLiveData.value?.showViewerState!!) {
            viewHolder.itemView.viewerStateChatMessage.visibility = View.GONE
        }

        if (CurrentUser.optionsLiveData.value?.showTime!!) {
            val dateFormat = SimpleDateFormat("HH:mm")
            val time = dateFormat.format(messageData.timestamp)
            if (CurrentUser.optionsLiveData.value?.showTime!!) {
                viewHolder.itemView.timestampChatMessage.visibility = View.VISIBLE
            } else {
                viewHolder.itemView.timestampChatMessage.visibility = View.GONE
            }
            viewHolder.itemView.timestampChatMessage.text = time
        }

        if (CurrentUser.user != null) {
            if (CurrentUser.user!!.username == messageData.nick) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#1A1A1A"))
            } else if (messageData.data.toLowerCase(Locale.ROOT).contains(
                    CurrentUser.user!!.username.toLowerCase(
                        Locale.ROOT
                    )
                )
            ) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
            } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                    CurrentUser.user!!.username
                )
            ) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#111111"))
            }
        } else if (CurrentUser.user == null) {
            if (messageData.data.contains("anonymous")) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
            } else {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#111111"))
            }
        }

        if (CurrentUser.optionsLiveData.value?.customHighlights!!.isNotEmpty()) {
            CurrentUser.optionsLiveData.value?.customHighlights?.forEach {
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

        if (CurrentUser.tempHighlightNick != null) {
            when {
                CurrentUser.tempHighlightNick!!.contains(messageData.nick) -> {
                    viewHolder.itemView.alpha = 1f
                }
                CurrentUser.tempHighlightNick!!.isEmpty() -> {
                    viewHolder.itemView.alpha = 1f
                }
                else -> {
                    viewHolder.itemView.alpha = 0.5f
                }
            }
        } else {
            viewHolder.itemView.alpha = 1f
        }

        viewHolder.itemView.usernameChatMessage.text = "${messageData.nick}:"

        viewHolder.itemView.messageChatMessage.movementMethod = LinkMovementMethod.getInstance()

        createMessageTextView(context, messageData, viewHolder.itemView.messageChatMessage)

        viewHolder.itemView.usernameChatMessage.setOnClickListener {
            if (CurrentUser.tempHighlightNick == null) {
                CurrentUser.tempHighlightNick = mutableListOf()
            }
            CurrentUser.tempHighlightNick!!.add(messageData.nick)
            adapter?.notifyDataSetChanged()
        }

        if (sendMessageText != null) {
            viewHolder.itemView.usernameChatMessage.setOnLongClickListener {
                val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
                val pop = PopupMenu(wrapper, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            sendMessageText!!.setText("/w ${messageData.nick} ")
                            keyRequestFocus(sendMessageText!!, context)
                            sendMessageText!!.setSelection(sendMessageText!!.text.length)
                        }
                        R.id.chatMention -> {
                            val currentMessage = sendMessageText!!.text.toString()
                            if (currentMessage.isNotEmpty()) {
                                if (currentMessage.last() == ' ') {
                                    sendMessageText!!.setText(currentMessage.plus("${messageData.nick} "))
                                } else {
                                    sendMessageText!!.setText(currentMessage.plus(" ${messageData.nick} "))
                                }
                            } else {
                                sendMessageText!!.setText("${messageData.nick} ")
                            }
                            keyRequestFocus(sendMessageText!!, context)
                            sendMessageText!!.setSelection(sendMessageText!!.text.length)
                        }
                        R.id.chatIgnore -> {
//                            CurrentUser.optionsLiveData.value?.ignoreList?.add(messageData.nick)
                            CurrentUser.addIgnore(messageData.nick)
//                            CurrentUser.saveOptions(context)
                            adapter?.notifyDataSetChanged()
                        }
                    }
                    true
                }
                pop.show()
                true
            }
        }

        if (messageData.entities.spoilers!!.isEmpty()) {
            viewHolder.itemView.messageChatMessage.setOnClickListener {
                CurrentUser.tempHighlightNick = null
                adapter?.notifyDataSetChanged()
            }
        }

        viewHolder.itemView.setOnClickListener {
            CurrentUser.tempHighlightNick = null
            adapter?.notifyDataSetChanged()
        }

        if (isConsecutive) {
            viewHolder.itemView.usernameChatMessage.visibility = View.GONE
            viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
        }
    }

    fun isNickSame(nick: String): Boolean {
        return messageData.nick == nick
    }
}