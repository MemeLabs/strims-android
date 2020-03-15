package gg.strims.mobile

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.chat_message.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.beust.klaxon.Klaxon
import io.ktor.client.HttpClient
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.wss
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import java.util.*

@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {

    private val adapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        GlobalScope.launch {
            WSClient().onConnect()
        }

        recyclerViewChat.adapter = adapter
        recyclerViewChat.layoutManager = LinearLayoutManager(this)
    }

    inner class ChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.chat_message
        }

        @SuppressLint("SetTextI18n", "SimpleDateFormat")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val date = Date(messageData.timestamp)
            val time = if (date.minutes < 10) {
                "${date.hours}:0${date.minutes}"
            } else {
                "${date.hours}:${date.minutes}"
            }

            val first = messageData.data.first()
            if (first.toString() == ">") {
                viewHolder.itemView.message.setTextColor(Color.parseColor("#789922"))
            }

            viewHolder.itemView.timestampMessage.text = time
            viewHolder.itemView.username.text = "${messageData.nick}:"
            viewHolder.itemView.message.text = messageData.data
        }
    }

    inner class WSClient {

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
                            runOnUiThread(kotlinx.coroutines.Runnable {
                                adapter.add(ChatMessage(msg))
                                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                            })
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
}
