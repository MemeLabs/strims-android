package gg.strims.android.items

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.ChatActivity
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.models.Message
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.chat_message.view.*
import java.util.*

class ChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_message
    }

    @KtorExperimentalAPI
    @SuppressLint("SetTextI18n", "SimpleDateFormat", "WrongViewCast")
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        CurrentUser.options!!.ignoreList.forEach {
            if (it == messageData.nick) {
                return
            }

            if (CurrentUser.options!!.harshIgnore) {
                if (messageData.data.contains(it)) {
                    return
                }
            }
        }

        if (CurrentUser.options!!.hideNsfw) {
            if ((messageData.data.contains("nsfw") || messageData.data.contains("nsfl")
                        && messageData.data.contains("a link"))) {
                return
            }
        }

        if (CurrentUser.options!!.greentext) {
            if (messageData.data.first() == '>') {
                viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#789922"))
            } else {
                viewHolder.itemView.messageChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }

        if (CurrentUser.options!!.showTime) {
            val date = Date(messageData.timestamp)
            val time = if (date.minutes < 10) {
                "${date.hours}:0${date.minutes}"
            } else {
                "${date.hours}:${date.minutes}"
            }
            viewHolder.itemView.timestampChatMessage.visibility = View.VISIBLE
            viewHolder.itemView.timestampChatMessage.text = time
        }

        if (CurrentUser.user != null) {
            if (messageData.data.contains(CurrentUser.user!!.username)) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
            } else if (CurrentUser.user!!.username == messageData.nick) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#151515"))
            } else if (CurrentUser.user!!.username != messageData.nick && !messageData.data.contains(
                    CurrentUser.user!!.username)) {
                viewHolder.itemView.setBackgroundColor(Color.parseColor("#000000"))
            }
        }

        if (CurrentUser.options!!.customHighlights.isNotEmpty()) {
            CurrentUser.options!!.customHighlights.forEach {
                if (messageData.nick == it) {
                    viewHolder.itemView.setBackgroundColor(Color.parseColor("#001D36"))
                }
            }
        }

        if (messageData.features.contains("bot")) {
            viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FF2196F3"))
            viewHolder.itemView.botFlairChatMessage.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFFFFF"))
            viewHolder.itemView.botFlairChatMessage.visibility = View.GONE
        }

        if (CurrentUser.tempHighlightNick != null && CurrentUser.tempHighlightNick!!.contains(messageData.nick)) {
            viewHolder.itemView.usernameChatMessage.setTextColor(Color.parseColor("#FFF44336"))
        }

        viewHolder.itemView.usernameChatMessage.text = "${messageData.nick}:"
        viewHolder.itemView.messageChatMessage.text = messageData.data

        viewHolder.itemView.usernameChatMessage.setOnClickListener {
            for (i in 0 until ChatActivity().adapter.itemCount) {
                if (ChatActivity().adapter.getItem(i).layout == R.layout.chat_message) {
                    val item = ChatActivity().adapter.getItem(i) as ChatMessage
                    if (item.messageData.nick == messageData.nick) {
                        val adapterItem =
                            ChatActivity().recyclerViewChat.findViewHolderForAdapterPosition(i)

                        adapterItem?.itemView?.usernameChatMessage?.setTextColor(Color.parseColor("#FFF44336"))
                    }
                }
            }
            if (CurrentUser.tempHighlightNick == null) {
                CurrentUser.tempHighlightNick = mutableListOf()
            }
            CurrentUser.tempHighlightNick!!.add(messageData.nick)
        }

        viewHolder.itemView.setOnClickListener {
            for (i in 0 until ChatActivity().adapter.itemCount) {
                if (ChatActivity().adapter.getItem(i).layout == R.layout.chat_message) {
                    val item = ChatActivity().adapter.getItem(i) as ChatMessage
                    if (item.messageData.features.isEmpty()) {
                        val adapterItem =
                            ChatActivity().recyclerViewChat.findViewHolderForAdapterPosition(i)

                        adapterItem?.itemView?.usernameChatMessage?.setTextColor(Color.parseColor("#FFFFFF"))
                    }
                    CurrentUser.tempHighlightNick = null
                }
            }
        }
    }
}