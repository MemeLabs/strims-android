package gg.strims.android

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import gg.strims.android.models.*
import io.ktor.util.*
import pl.droidsonroids.gif.GifDrawable
import java.util.HashMap

@KtorExperimentalAPI
object CurrentUser {
    var user: Profile? = null // Current user
    var options: Options? = null // Loaded options file
    var tempHighlightNick: MutableList<String>? = null // List of nicks to highlight in chat when a user clicks on them
    var users = mutableListOf<String>() // List of all users in chat
    var streams: MutableList<Stream>? = null // List of all active streams from STREAMS_SET
    var tempStream: Stream? = null // Currently playing AngelThump stream object
    var tempTwitchUrl: String? = null // Currently playing Twitch URL
    var tempTwitchVod: Boolean? = null // True if selected stream is a Twitch VOD
    var tempYouTubeId: String? = null // Currently playing YouTube Video ID
    var emotes: MutableList<Emote>? = null // Collection of all emote models
    var jwt: String? = null // JSON Web Token of current user
    var viewerStates: MutableList<ViewerState>? = null // Collection of ViewerStates
    lateinit var bitmapMemoryCache: HashMap<String, Bitmap> // Collection of all emote Bitmaps
    lateinit var gifMemoryCache: HashMap<String, GifDrawable> // Collection of all animated emote GifDrawables

    var liveDataStream = MutableLiveData<Stream>()

    val time = System.currentTimeMillis()

    fun saveOptions(context: Context) {
        val sharedPreferences = context.getSharedPreferences("ChatOptions", Context.MODE_PRIVATE).edit()
        sharedPreferences.putString("options", Gson().toJson(options))
        sharedPreferences.apply()
    }
}