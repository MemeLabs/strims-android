package gg.strims.android.viewholders

import android.annotation.SuppressLint
import android.view.View
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import io.ktor.util.*
import kotlinx.android.synthetic.main.error_chat_message_item.view.*
import java.text.SimpleDateFormat

@SuppressLint("SimpleDateFormat")
@KtorExperimentalAPI
class ErrorChatMessage(private val message: String) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int = R.layout.error_chat_message_item

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if (CurrentUser.optionsLiveData.value?.showTime!!) {
            val dateFormat = SimpleDateFormat("HH:mm")
            val time = dateFormat.format(System.currentTimeMillis())
            viewHolder.itemView.timestampErrorChatMessage.visibility = View.VISIBLE
            viewHolder.itemView.timestampErrorChatMessage.text = time
        }
        viewHolder.itemView.messageErrorChatMessage.text = message
    }
}