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
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.createMessageTextView
import gg.strims.android.keyRequestFocus
import gg.strims.android.models.Message
import io.ktor.util.*
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import java.text.SimpleDateFormat

@SuppressLint("SetTextI18n", "SimpleDateFormat")
@KtorExperimentalAPI
class PrivateChatMessage(
    private val context: Context,
    private val adapter: GroupAdapter<GroupieViewHolder>,
    private val messageData: Message,
    private val isReceived: Boolean = false,
    private val sendMessageText: EditText? = null
) :
    Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.private_chat_message_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (isReceived) {
            viewHolder.itemView.whisperedPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
        } else {
            viewHolder.itemView.toPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.whisperedPrivateMessage.text = ":"
            viewHolder.itemView.whisperedPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.setBackgroundColor(Color.parseColor("#1A1A1A"))
        }

        if (CurrentUser.options!!.showTime) {
            val dateFormat = SimpleDateFormat("HH:mm")
            val time = dateFormat.format(messageData.timestamp)
            viewHolder.itemView.timestampPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.timestampPrivateMessage.text = time
        }

        if (CurrentUser.options!!.customHighlights.isNotEmpty()) {
            CurrentUser.options!!.customHighlights.forEach {
                if (messageData.nick == it) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                }
            }
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

        viewHolder.itemView.usernamePrivateMessage.text =
            if (messageData.nick == CurrentUser.user?.username) messageData.targetNick else messageData.nick

        viewHolder.itemView.messagePrivateMessage.movementMethod =
            LinkMovementMethod.getInstance()

        createMessageTextView(
            context,
            messageData,
            viewHolder.itemView.messagePrivateMessage
        )

        viewHolder.itemView.usernamePrivateMessage.setOnClickListener {
            if (CurrentUser.tempHighlightNick == null) {
                CurrentUser.tempHighlightNick = mutableListOf()
            }
            CurrentUser.tempHighlightNick!!.add(messageData.nick)
            adapter.notifyDataSetChanged()
        }

        if (sendMessageText != null) {
            viewHolder.itemView.usernamePrivateMessage.setOnLongClickListener {
                val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
                val pop = PopupMenu(wrapper, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            sendMessageText.setText("/w ${messageData.nick} ")
                            keyRequestFocus(sendMessageText, context)
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
                            keyRequestFocus(sendMessageText, context)
                            sendMessageText.setSelection(sendMessageText.text.length)
                        }
                        R.id.chatIgnore -> {
                            CurrentUser.options!!.ignoreList.add(messageData.nick)
                            CurrentUser.saveOptions(context)
                            adapter.notifyDataSetChanged()
                        }
                    }
                    true
                }
                pop.show()
                true
            }
        }

        viewHolder.itemView.setOnClickListener {
            CurrentUser.tempHighlightNick = null
            adapter.notifyDataSetChanged()
        }
    }

    fun isNickSame(nick: String): Boolean {
        return nick == messageData.nick
    }
}