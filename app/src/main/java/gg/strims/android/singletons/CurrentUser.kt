package gg.strims.android.singletons

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import gg.strims.android.models.*
import gg.strims.android.utils.notifyObserver
import io.ktor.util.*
import pl.droidsonroids.gif.GifDrawable
import java.util.HashMap

@KtorExperimentalAPI
object CurrentUser {
    var user: Profile? = null // Current user
    var optionsLiveData = MutableLiveData<Options>() // LiveData of Options
    var emotes: MutableList<Emote>? = null // Collection of all emote models

    var bitmapMemoryCache: HashMap<String, Bitmap> = HashMap() // Collection of all emote Bitmaps
    var gifMemoryCache: HashMap<String, GifDrawable> = HashMap() // Collection of all animated emote GifDrawables

    val time = System.currentTimeMillis()

    fun addIgnore(user: String) {
        optionsLiveData.value?.ignoreList?.add(user)
        optionsLiveData.notifyObserver()
    }

    fun removeIgnore(user: String) {
        optionsLiveData.value?.ignoreList?.remove(user)
        optionsLiveData.notifyObserver()
    }

    fun addHighlight(user: String) {
        optionsLiveData.value?.customHighlights?.add(user)
        optionsLiveData.notifyObserver()
    }

    fun removeHighlight(user: String) {
        optionsLiveData.value?.customHighlights?.remove(user)
        optionsLiveData.notifyObserver()
    }

    fun saveOptions(context: Context) {
        val sharedPreferences =
            context.getSharedPreferences("ChatOptions", Context.MODE_PRIVATE).edit()
        sharedPreferences.putString("options", Gson().toJson(optionsLiveData.value))
        sharedPreferences.apply()
    }
}