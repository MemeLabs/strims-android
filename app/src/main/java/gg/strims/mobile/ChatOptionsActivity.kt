package gg.strims.mobile

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat_options.*

class ChatOptionsActivity : AppCompatActivity() {

    @KtorExperimentalAPI
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_options)

        checkBoxTimestamp.isChecked = ChatActivity.CurrentOptions.options!!.showTime
        checkBoxGreentext.isChecked = ChatActivity.CurrentOptions.options!!.greentext
        checkBoxHarshIgnore.isChecked = ChatActivity.CurrentOptions.options!!.harshIgnore
        checkBoxHideNsfw.isChecked = ChatActivity.CurrentOptions.options!!.harshIgnore
        checkBoxNotifications.isChecked = ChatActivity.CurrentOptions.options!!.notifications
        checkBoxEmotes.isChecked = ChatActivity.CurrentOptions.options!!.emotes

        ignoredUsersEditTextOptions.setText(ChatActivity.CurrentOptions.options!!.ignoreList.toString())
        customHighlightsEditTextOptions.setText(ChatActivity.CurrentOptions.options!!.customHighlights.toString())
    }
}
