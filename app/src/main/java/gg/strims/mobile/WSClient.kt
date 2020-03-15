package gg.strims.mobile

import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.HttpMethod
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI

@io.ktor.util.KtorExperimentalAPI
object WSClient {
    private val client = HttpClient {
        install(WebSockets)
    }

    suspend fun onConnect() = client.wss(
        host = "chat.strims.gg",
        path = "/ws"
    ) {
        when (val frame = incoming.receive()) {
            is Frame.Text -> println(frame.readText())
            is Frame.Binary -> println(frame.readBytes())
        }
    }
}