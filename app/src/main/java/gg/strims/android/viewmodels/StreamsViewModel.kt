package gg.strims.android.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.google.gson.JsonElement
import gg.strims.android.models.Stream
import gg.strims.android.notifyObserver

class StreamsViewModel: ViewModel() {
    var streams = MutableLiveData<MutableList<Stream>>()

    fun parseStreams(input: String) {
        val stream = Gson().fromJson(input, JsonElement::class.java)
        when (stream.asJsonArray[0].asString) {
            "RUSTLERS_SET" -> {
                if (streams.value != null) {
                    streams.value!!.forEach {
                        if (it.id == stream.asJsonArray[1].asLong) {
                            it.rustlers = stream.asJsonArray[2].asInt
                            it.afk_rustlers = stream.asJsonArray[3].asInt
                            return@forEach
                        }
                    }
                    streams.notifyObserver()
                }
            }
            "STREAMS_SET" -> {
                val streamsParsed = Gson().fromJson(stream.asJsonArray[1], Array<Stream>::class.java)
                streams.value = streamsParsed.toMutableList()
            }
        }
    }
}