package gg.strims.android.viewholders

import android.view.View
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.R
import gg.strims.android.databinding.ErrorChatMessageItemBinding
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class ErrorChatMessage(private val message: String) : BindableItem<ErrorChatMessageItemBinding>() {
    override fun getLayout(): Int = R.layout.error_chat_message_item

    override fun bind(viewBinding: ErrorChatMessageItemBinding, position: Int) {
        with (viewBinding) {
            if (CurrentUser.optionsLiveData.value?.showTime!!) {
                val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val time = dateFormat.format(System.currentTimeMillis())
                timestampErrorChatMessage.visibility = View.VISIBLE
                timestampErrorChatMessage.text = time
            }
            messageErrorChatMessage.text = message
        }
    }

    override fun initializeViewBinding(view: View): ErrorChatMessageItemBinding {
        return ErrorChatMessageItemBinding.bind(view)
    }
}