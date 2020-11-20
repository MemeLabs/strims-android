package gg.strims.android.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
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
    var adapter: CustomAdapter,
    private val messageData: Message,
    private val isReceived: Boolean = false
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

        if (CurrentUser.optionsLiveData.value?.showTime!!) {
            val dateFormat = SimpleDateFormat("HH:mm")
            val time = dateFormat.format(messageData.timestamp)
            viewHolder.itemView.timestampPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.timestampPrivateMessage.text = time
        }

        if (CurrentUser.optionsLiveData.value?.customHighlights!!.isNotEmpty()) {
            CurrentUser.optionsLiveData.value?.customHighlights!!.forEach {
                if (messageData.nick == it) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                    return@forEach
                }
            }
        }

        if (adapter.tempHighlightNick != null) {
            when {
                adapter.tempHighlightNick!!.contains(messageData.nick) -> {
                    viewHolder.itemView.alpha = 1f
                }
                adapter.tempHighlightNick!!.isEmpty() -> {
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
            if (adapter.tempHighlightNick == null) {
                adapter.tempHighlightNick = mutableListOf()
            }
            adapter.tempHighlightNick!!.add(messageData.nick)
            adapter.notifyDataSetChanged()
        }

        if (adapter.sendMessageText != null) {
            viewHolder.itemView.usernamePrivateMessage.setOnLongClickListener {
                val wrapper = ContextThemeWrapper(context, R.style.PopupMenu)
                val pop = PopupMenu(wrapper, it)
                pop.inflate(R.menu.chat_message_username_menu)
                pop.setOnMenuItemClickListener { itMenuItem ->
                    when (itMenuItem.itemId) {
                        R.id.chatWhisper -> {
                            adapter.sendMessageText!!.setText("/w ${messageData.nick} ")
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
                                adapter.sendMessageText!!.setText("${messageData.nick} ")
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

        viewHolder.itemView.messagePrivateMessage.setOnClickListener {
            adapter.tempHighlightNick = null
            adapter.notifyDataSetChanged()
        }

        viewHolder.itemView.setOnClickListener {
            adapter.tempHighlightNick = null
            adapter.notifyDataSetChanged()
        }
    }
}