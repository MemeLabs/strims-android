package gg.strims.mobile

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
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
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.readBytes
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.send
import kotlinx.android.synthetic.main.chat_message.*
import kotlinx.android.synthetic.main.chat_message.view.message
import kotlinx.android.synthetic.main.private_chat_message.view.*
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

        sendMessageText.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (sendMessageText.text.isNotEmpty()) {
                    sendMessageButton.isEnabled = true
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                if (sendMessageText.text.isNotEmpty()) {
                    sendMessageButton.isEnabled = true
                }
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (sendMessageText.text.isNotEmpty()) {
                    sendMessageButton.isEnabled = true
                }
            }
        })

        sendMessageText.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                sendMessageButton.performClick()
                return@OnKeyListener true
            }
            false
        })

        recyclerViewChat.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true
        recyclerViewChat.layoutManager = layoutManager
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.chat_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chatLogin -> {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        return true
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

    inner class PrivateChatMessage(private val messageData: Message) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.private_chat_message
        }

        @SuppressLint("SetTextI18n")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val date = Date(messageData.timestamp)
            val time = if (date.minutes < 10) {
                "${date.hours}:0${date.minutes}"
            } else {
                "${date.hours}:${date.minutes}"
            }

            if (messageData.data.first() == '>') {
                viewHolder.itemView.message.setTextColor(Color.parseColor("#789922"))
            }

            viewHolder.itemView.timestampMessagePrivate.text = time
            viewHolder.itemView.usernamePrivate.text = messageData.nick
            viewHolder.itemView.messagePrivate.text = " whispered: ${messageData.data}"
        }
    }

    inner class WSClient {

        private var jwt: String? = null

        private fun retrieveCookie() {
            val cookies = CookieManager.getInstance().getCookie("https://strims.gg")
            if (cookies != null) {
                Log.d("TAG", "cookie: $cookies")
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                }
                Log.d("TAG", "JWT: $jwt")
            }
        }

        private val client = HttpClient {
            install(WebSockets)
        }

        suspend fun onConnect() = client.wss(
            host = "chat.strims.gg",
            path = "/ws",
            request = {
                retrieveCookie()
                Log.d("TAG", "requesting with JWT: $jwt")
                header("Cookie", "jwt=$jwt")
            }
        ){
            sendMessageButton.setOnClickListener {
                GlobalScope.launch {
                    if (sendMessageText.text.toString().substringBefore(" ") == "/w") {
                        val nick = sendMessageText.text.toString().substringAfter("/w ").substringBefore(" ")
                        send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"${sendMessageText.text.toString().substringAfter("/w $nick ")}\"}")
                    }
                    send("MSG {\"data\":\"${sendMessageText.text}\"}")
                    sendMessageText.text.clear()
                    runOnUiThread(kotlinx.coroutines.Runnable {
                        sendMessageButton.isEnabled = false
                    })
                }
            }
            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        println(frame.readText())
                        val msg: Message? = parseMessage(frame.readText())
                        if (msg != null) {
                            runOnUiThread(kotlinx.coroutines.Runnable {
                                if (msg.privMsg) {
                                    adapter.add(PrivateChatMessage(msg))
                                } else {
                                    adapter.add(ChatMessage(msg))
                                }
                                recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
                            })
                        }
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }

        private fun parseMessage(input: String): Message? {
            val msg = input.split(" ", limit = 2)
            val msgType = msg[0]
            if (msgType == "PRIVMSG") {
                val message = Klaxon().parse<Message>(msg[1])!!
                return Message(true, message.nick, message.data, message.timestamp)
            } else if (msgType == "MSG") {
                return Klaxon().parse<Message>(msg[1])
            }
            return null
        }
    }
}
