package gg.strims.android.viewholders

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.R
import gg.strims.android.adapters.CustomAdapter
import gg.strims.android.createMessageTextView
import gg.strims.android.databinding.ChatMessageItemConsecutiveNickBinding
import gg.strims.android.models.Message
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class ConsecutiveChatMessage(
    var adapter: CustomAdapter,
    val messageData: Message,
) : BindableItem<ChatMessageItemConsecutiveNickBinding>() {

    override fun getLayout(): Int = R.layout.chat_message_item_consecutive_nick

    override fun bind(viewBinding: ChatMessageItemConsecutiveNickBinding, position: Int) {
        with (viewBinding) {
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

            messageChatMessage.movementMethod = LinkMovementMethod.getInstance()

            createMessageTextView(root.context, messageData, messageChatMessage)

            if (messageData.entities.spoilers!!.isEmpty()) {
                messageChatMessage.setOnClickListener {
                    adapter.tempHighlightNick = null
                    adapter.notifyDataSetChanged()
                }
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

    override fun initializeViewBinding(view: View): ChatMessageItemConsecutiveNickBinding {
        return ChatMessageItemConsecutiveNickBinding.bind(view)
    }
}