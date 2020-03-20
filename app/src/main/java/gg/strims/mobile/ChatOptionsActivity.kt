package gg.strims.mobile

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat_options.*
import java.io.FileOutputStream
import java.lang.Exception

@KtorExperimentalAPI
class ChatOptionsActivity : AppCompatActivity() {

    override fun onPause() {
        saveOptions()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_options)

        checkBoxTimestamp.isChecked = CurrentUser.options!!.showTime
        checkBoxGreentext.isChecked = CurrentUser.options!!.greentext
        checkBoxHarshIgnore.isChecked = CurrentUser.options!!.harshIgnore
        checkBoxHideNsfw.isChecked = CurrentUser.options!!.harshIgnore
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

    private fun saveOptions() {
        val userOptions = CurrentUser.options
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = openFileOutput("filename.txt", Context.MODE_PRIVATE)
            Log.d("TAG", "Saving: ${Gson().toJson(userOptions)}")
            fileOutputStream.write(Gson().toJson(userOptions).toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
