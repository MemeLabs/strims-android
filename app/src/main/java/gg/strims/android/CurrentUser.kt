package gg.strims.android

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import gg.strims.android.models.ChatUser
import gg.strims.android.models.Options
import gg.strims.android.models.Stream
import gg.strims.android.models.User
import java.io.FileOutputStream
import java.lang.Exception

object CurrentUser {
    var user: User? = null //Current user
    var options: Options? = null //Loaded options file
    var tempHighlightNick: MutableList<String>? = null //List of nicks to highlight in chat when a user clicks on them
    var users: MutableList<ChatUser>? = null //List of all users in chat
    var connectionCount: Int? = null //Connection count
    var streams: MutableList<Stream>? = null //List of all active streams from STREAMS_SET
    var tempStream: Stream? = null //Currently playing AngelThump stream object
    var tempTwitchUrl: String? = null //Currently playing Twitch URL
    var tempYouTubeId: String? = null //Currently playing YouTube Video ID
    var jwt: String? = null

    fun saveOptions(context: Context) {
        val userOptions = options
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