package gg.strims.android

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.google.gson.Gson
import gg.strims.android.models.*
import io.ktor.util.*
import pl.droidsonroids.gif.GifDrawable
import java.io.FileOutputStream
import java.lang.Exception
import java.util.HashMap

@KtorExperimentalAPI
object CurrentUser {
    var user: User? = null // Current user
    var options: Options? = null // Loaded options file
    var tempHighlightNick: MutableList<String>? = null // List of nicks to highlight in chat when a user clicks on them
    var users = mutableListOf<String>() // List of all users in chat
    var connectionCount: Int? = null // Connection count
    var streams: MutableList<Stream>? = null // List of all active streams from STREAMS_SET
    var tempStream: Stream? = null // Currently playing AngelThump stream object
    var tempTwitchUrl: String? = null // Currently playing Twitch URL
    var tempTwitchVod: Boolean? = null // True if selected stream is a Twitch VOD
    var tempYouTubeId: String? = null // Currently playing YouTube Video ID
    var emotes: MutableList<Emote>? = null // Collection of all emote models
    var jwt: String? = null // JSON Web Token of current user
    var privateMessageUsers: MutableList<String>? = null // List of users currently with private conversations
    var tempWhisperUser: String? = null // User to be passed to WhispersUserFragment
    var whispersMap = HashMap<String, MutableList<Message>>() // HashMap of users and their private conversations
    var viewerStates: MutableList<ViewerState>? = null
    lateinit var bitmapMemoryCache: HashMap<String, Bitmap> // Collection of all emote Bitmaps
    lateinit var gifMemoryCache: LruCache<String, GifDrawable> // Collection of all animated emote GifDrawables

    val time = System.currentTimeMillis()

    fun saveOptions(context: Context) {
        val userOptions = options
        val fileOutputStream: FileOutputStream
        try {
            fileOutputStream = context.openFileOutput("options.txt", Context.MODE_PRIVATE)
            Log.d("TAG", "Saving: ${Gson().toJson(userOptions)}")
            fileOutputStream.write(Gson().toJson(userOptions).toByteArray())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}