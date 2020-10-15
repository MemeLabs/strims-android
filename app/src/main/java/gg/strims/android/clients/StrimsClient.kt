package gg.strims.android.clients

import com.google.gson.Gson
import com.google.gson.JsonElement
import gg.strims.android.CurrentUser
import gg.strims.android.models.Stream
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
class StrimsClient {

    private val client = HttpClient {
        install(WebSockets)
    }

    suspend fun onConnect() = client.wss(
        host = "strims.gg",
        path = "/ws"
    ) {
        while (true) {
            when (val frame = incoming.receive()) {
                is Frame.Text -> {
                    println(frame.readText())
                    parseStream(frame.readText())
                }
                is Frame.Binary -> println(frame.readBytes())
            }
        }
    }

    private fun parseStream(input: String) {
        val test = Gson().fromJson(input, JsonElement::class.java)
        when (test.asJsonArray[0].asString) {
            "RUSTLERS_SET" -> {
                if (CurrentUser.streams != null) {
                    CurrentUser.streams!!.forEach {
                        if (it.id == test.asJsonArray[1].asLong) {
                            it.rustlers = test.asJsonArray[2].asInt
                            it.afk_rustlers = test.asJsonArray[3].asInt
                        }
                    }
                }
            }
            "STREAMS_SET" -> {
                val streams2 = Gson().fromJson(test.asJsonArray[1], Array<Stream>::class.java)
                CurrentUser.streams = streams2.toMutableList()
            }
        }
    }
}