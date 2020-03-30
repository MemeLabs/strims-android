package gg.strims.android

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import gg.strims.android.models.ChatUser
import gg.strims.android.models.Options
import gg.strims.android.models.User
import java.io.FileOutputStream
import java.lang.Exception

object CurrentUser {
    var user: User? = null
    var options: Options? = null
    var tempSequentialNick: String? = null
    var tempHighlightNick: MutableList<String>? = null
    var users: MutableList<ChatUser>? = null
    var connectionCount: Int? = null

    fun saveOptions(context: Context) {
        val userOptions = CurrentUser.options
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = context.openFileOutput("filename.txt", Context.MODE_PRIVATE)
            Log.d("TAG", "Saving: ${Gson().toJson(userOptions)}")
            fileOutputStream.write(Gson().toJson(userOptions).toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}