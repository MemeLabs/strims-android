package gg.strims.android.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.EditText
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import androidx.recyclerview.widget.RecyclerView
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
    private val recyclerViewChat: RecyclerView,
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
            for (i in 0 until adapter.itemCount) {
                if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                    val item = adapter.getItem(i) as ChatMessage
                    if (item.isNickSame(messageData.nick)) {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f

                    } else {

                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)

                        if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                item.getNick()
                            )
                        ) {
                            adapterItem?.itemView?.alpha = 1f
                        } else {
                            adapterItem?.itemView?.alpha = 0.5f
                        }
                    }
                } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                    val item = adapter.getItem(i) as PrivateChatMessage
                    if (item.messageData.nick == messageData.nick) {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        adapterItem?.itemView?.alpha = 1f
                    } else {
                        val adapterItem =
                            recyclerViewChat.findViewHolderForAdapterPosition(i)
                        if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(
                                item.getNick()
                            )
                        ) {
                            adapterItem?.itemView?.alpha = 1f
                        } else {
                            adapterItem?.itemView?.alpha = 0.5f
                        }
                    }
                } else {
                    val adapterItem =
                        recyclerViewChat.findViewHolderForAdapterPosition(i)
                    adapterItem?.itemView?.alpha = 0.5f
                }
                adapter.notifyItemChanged(i)
            }
            if (CurrentUser.tempHighlightNick == null) {
                CurrentUser.tempHighlightNick = mutableListOf()
            }
            CurrentUser.tempHighlightNick!!.add(messageData.nick)
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
            for (i in 0 until adapter.itemCount) {
                if (adapter.getItem(i).layout == R.layout.chat_message_item || adapter.getItem(i).layout == R.layout.chat_message_item_consecutive_nick) {
                    val adapterItem =
                        recyclerViewChat.findViewHolderForAdapterPosition(i)
                    adapterItem?.itemView?.alpha = 1f

                } else if (adapter.getItem(i).layout == R.layout.private_chat_message_item) {
                    val adapterItem =
                        recyclerViewChat.findViewHolderForAdapterPosition(i)
                    adapterItem?.itemView?.alpha = 1f

                } else {
                    val adapterItem =
                        recyclerViewChat.findViewHolderForAdapterPosition(i)
                    adapterItem?.itemView?.alpha = 1f
                }
                adapter.notifyItemChanged(i)
            }
        }
    }

    fun isNickSame(nick: String): Boolean {
        return nick == messageData.nick
    }

    fun getNick(): String {
        return messageData.nick
    }
}