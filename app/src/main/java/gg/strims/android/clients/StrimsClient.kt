package gg.strims.android.clients

import com.beust.klaxon.Klaxon
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
        val input2 = input.substringAfter("[\"").substringBefore("\"")
        if (input2 == "STREAMS_SET") {
            val msg = input.substringAfter("\",").substringBeforeLast(']')
            val streams: List<Stream>? = Klaxon()
                .parseArray(msg)
            CurrentUser.streams = streams?.toMutableList()
        } else if (input2 == "RUSTLERS_SET") {
            val id = input.substringAfter("\"RUSTLERS_SET\",").substringBefore(",").toLong()
            if (CurrentUser.streams != null) {
                CurrentUser.streams!!.forEach {
                    if (it.id == id) {
                        val newRustlers =
                            input.substringAfter("$id,").substringBefore(",").toInt()
                        val newAfk =
                            input.substringAfter("$id,$newRustlers,").substringBefore("]")
                                .toInt()
                        it.rustlers = newRustlers
                        it.afk_rustlers = newAfk
                        return
                    }
                }
            }
        }
    }
}