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
import gg.strims.android.databinding.PrivateChatMessageItemBinding
import gg.strims.android.utils.keyRequestFocus
import gg.strims.android.models.Message
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class PrivateChatMessage(
    var adapter: CustomAdapter,
    private val messageData: Message,
    private val isReceived: Boolean = false
) :
    BindableItem<PrivateChatMessageItemBinding>() {
    override fun getLayout(): Int = R.layout.private_chat_message_item

    override fun bind(viewBinding: PrivateChatMessageItemBinding, position: Int) {
        with (viewBinding) {
            if (isReceived) {
                whisperedPrivateMessage.visibility = View.VISIBLE
                root.setBackgroundColor(Color.parseColor("#001D36"))
            } else {
                toPrivateMessage.visibility = View.VISIBLE
                whisperedPrivateMessage.text = ":"
                whisperedPrivateMessage.visibility = View.VISIBLE
                root.setBackgroundColor(Color.parseColor("#1A1A1A"))
            }

            if (CurrentUser.optionsLiveData.value?.showTime!!) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val time = dateFormat.format(messageData.timestamp)
                timestampPrivateMessage.visibility = View.VISIBLE
                timestampPrivateMessage.text = time
            }

            if (CurrentUser.optionsLiveData.value?.customHighlights!!.isNotEmpty()) {
                CurrentUser.optionsLiveData.value?.customHighlights!!.forEach {
                    if (messageData.nick == it) {
                        root.setBackgroundColor(Color.parseColor("#001D36"))
                        return@forEach
                    }
                }
            }

            if (adapter.tempHighlightNick != null) {
                when {
                    adapter.tempHighlightNick!!.contains(messageData.nick) -> {
                        root.alpha = 1f
                    }
                    adapter.tempHighlightNick!!.isEmpty() -> {
                        root.alpha = 1f
                    }
                    else -> {
                        root.alpha = 0.5f
                    }
                }
            } else {
                root.alpha = 1f
            }

            usernamePrivateMessage.text =
                if (messageData.nick == CurrentUser.user?.username) messageData.targetNick else messageData.nick

            messagePrivateMessage.movementMethod =
                LinkMovementMethod.getInstance()

            createMessageTextView(
                root.context,
                messageData,
                messagePrivateMessage
            )

            usernamePrivateMessage.setOnClickListener {
                if (adapter.tempHighlightNick == null) {
                    adapter.tempHighlightNick = mutableListOf()
                }
                adapter.tempHighlightNick!!.add(messageData.nick)
                adapter.notifyDataSetChanged()
            }

            if (adapter.sendMessageText != null) {
                usernamePrivateMessage.setOnLongClickListener {
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
                                            R.string.chat_mention_popup,
                                            messageData.nick
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

            messagePrivateMessage.setOnClickListener {
                adapter.tempHighlightNick = null
                adapter.notifyDataSetChanged()
            }

            root.setOnClickListener {
                adapter.tempHighlightNick = null
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun initializeViewBinding(view: View): PrivateChatMessageItemBinding {
        return PrivateChatMessageItemBinding.bind(view)
    }
}