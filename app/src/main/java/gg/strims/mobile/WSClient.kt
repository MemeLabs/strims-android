package gg.strims.mobile

import com.beust.klaxon.Klaxon
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI

object WSClient {

    var message: Message? = null

    @KtorExperimentalAPI
    private val client = HttpClient {
        install(WebSockets)
    }

    suspend fun onConnect() = client.wss(
        host = "chat.strims.gg",
        path = "/ws"
    ) {
        while (true) {
            when (val frame = incoming.receive()) {
                is Frame.Text -> {
                    val msg = parseMessage(frame.readText())
                    if (msg != null) {
                        message = msg
                        println(msg.data)
                    }
                }
                is Frame.Binary -> println(frame.readBytes())
            }
        }
    }

    private fun parseMessage(input: String): Message? {
        val msg = input.split(" ", limit = 2)
        val msgType = msg[0]
        if (msgType == "MSG") {
            return Klaxon().parse<Message>(msg[1])
        }
        return null
    }
}