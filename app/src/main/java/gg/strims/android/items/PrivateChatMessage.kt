package gg.strims.android.items

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.models.Message
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import java.util.*

class PrivateChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.private_chat_message_item
    }

    @SuppressLint("SetTextI18n")
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (CurrentUser.options!!.ignoreList.contains(messageData.nick)) {
            return
        }

        if (CurrentUser.options!!.showTime) {
            val date = Date(messageData.timestamp)
            val time = if (date.minutes < 10) {
                "${date.hours}:0${date.minutes}"
            } else {
                "${date.hours}:${date.minutes}"
            }
            viewHolder.itemView.timestampPrivateMessage.visibility = View.VISIBLE
            viewHolder.itemView.timestampPrivateMessage.text = time
        }

        if (CurrentUser.options!!.greentext) {
            if (messageData.data.first() == '>') {
                viewHolder.itemView.messagePrivateMessage.setTextColor(Color.parseColor("#789922"))
            } else {
                viewHolder.itemView.messagePrivateMessage.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }

        viewHolder.itemView.usernamePrivateMessage.text = messageData.nick
        viewHolder.itemView.messagePrivateMessage.text = " whispered: ${messageData.data}"
    }
}