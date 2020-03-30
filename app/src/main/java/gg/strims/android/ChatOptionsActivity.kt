package gg.strims.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat_options.*

@KtorExperimentalAPI
class ChatOptionsActivity : AppCompatActivity() {

    override fun onPause() {
        CurrentUser.saveOptions(this)
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_options)

        checkBoxTimestamp.isChecked = CurrentUser.options!!.showTime
        checkBoxGreentext.isChecked = CurrentUser.options!!.greentext
        checkBoxHarshIgnore.isChecked = CurrentUser.options!!.harshIgnore
        checkBoxHideNsfw.isChecked = CurrentUser.options!!.hideNsfw
        checkBoxNotifications.isChecked = CurrentUser.options!!.notifications
        checkBoxEmotes.isChecked = CurrentUser.options!!.emotes

        ignoredUsersTextViewOptions.text =
            CurrentUser.options!!.ignoreList.toString()
                .substringAfter('[').substringBefore(']')
        customHighlightsTextViewOptions.text =
            CurrentUser.options!!.customHighlights.toString()
                .substringAfter('[').substringBefore(']')

        checkBoxTimestamp.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.showTime = isChecked
        }

        checkBoxGreentext.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.greentext = isChecked
        }

        checkBoxHarshIgnore.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.harshIgnore = isChecked
        }

        checkBoxHideNsfw.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.hideNsfw = isChecked
        }

        checkBoxNotifications.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.notifications = isChecked
        }

        checkBoxEmotes.setOnCheckedChangeListener { buttonView, isChecked ->
            CurrentUser.options!!.emotes = isChecked
        }
    }
}
