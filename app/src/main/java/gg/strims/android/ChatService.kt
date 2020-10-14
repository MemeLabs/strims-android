package gg.strims.android

import android.app.IntentService
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.webkit.CookieManager
import com.beust.klaxon.Klaxon
import gg.strims.android.models.EmotesParsed
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@KtorExperimentalAPI
class ChatService: IntentService("ChatService") {

    init {
        instance = this
    }

    companion object {
        private lateinit var instance: ChatService

        fun stopSocket() {
            Log.d("TAG", "Stopping socket...")
            instance.stopSelf()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onHandleIntent(intent: Intent?) {
        try {
            GlobalScope.launch {
                ChatClient().onConnect()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

//        private fun retrieveHistory() {
//            val messageHistory =
//                Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
//            runOnUiThread {
//                messageHistory?.forEach {
//                    val msg = parseMessage(it)
//                    if (msg != null) {
//                        var consecutiveMessage = false
//                        if (adapter.itemCount > 0) {
//                            if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item || adapter.getItem(
//                                    adapter.itemCount - 1
//                                ).layout == R.layout.chat_message_item_consecutive_nick
//                            ) {
//                                val lastMessage =
//                                    adapter.getItem(adapter.itemCount - 1) as ChatMessage
//                                consecutiveMessage =
//                                    lastMessage.isNickSame(msg.nick)
//                            }
//
//                        }
//                        if (msg.entities.emotes != null && msg.entities.emotes!!.isNotEmpty() && msg.entities.emotes!![0].combo > 1) {
//                            if (msg.entities.emotes!![0].combo == 2) {
//                                adapter.removeGroupAtAdapterPosition(adapter.itemCount - 1)
//                                adapter.add(ChatMessageCombo(msg))
//                            } else {
//                                if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
//                                    val lastMessageCombo =
//                                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
//                                    lastMessageCombo.setCombo(msg.entities.emotes!![0].combo)
//                                    adapter.notifyItemChanged(adapter.itemCount - 1)
//                                }
//                            }
//
//                        } else {
//                            if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
//                                val lastMessage =
//                                    adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
//                                lastMessage.state = 1
//                                adapter.notifyItemChanged(adapter.itemCount - 1)
//                            }
//                            adapter.add(
//                                ChatMessage(msg, consecutiveMessage)
//                            )
//                        }
//                    }
//                }
//            }
//        }

        private suspend fun retrieveEmotes() {
            val text: String = client.get("https://chat.strims.gg/emote-manifest.json")
            val emotesParsed: EmotesParsed = Klaxon().parse(text)!!
            CurrentUser.emotes = emotesParsed.emotes.toMutableList()
        }

        private fun retrieveCookie() {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://strims.gg")
            cookieManager.flush()
            if (cookies != null) {
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                    CurrentUser.jwt = jwt
                }
            }
        }

//        private fun getBitmapFromURL(src: String?): Bitmap? {
//            return try {
//                val url = URL(src)
//                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
//                connection.doInput = true
//                connection.connect()
//                val input: InputStream = connection.inputStream
//                BitmapFactory.decodeStream(input)
//            } catch (e: IOException) {
//                null
//            }
//        }
//
//        private fun getGifFromURL(src: String?): GifDrawable? {
//            return try {
//                val url = URL(src)
//                val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
//                connection.doInput = true
//                connection.connect()
//                val input: InputStream = connection.inputStream
//                val bis = BufferedInputStream(input)
//                GifDrawable(bis)
//            } catch (e: IOException) {
//                null
//            }
//        }
//
//        private fun cacheEmotes() {
//            runOnUiThread {
//                CurrentUser.emotes?.forEach {
//                    val size = it.versions.size - 1
//                    val biggestEmote = it.versions[size]
//                    val url = "https://chat.strims.gg/${biggestEmote.path}"
//                    if (!biggestEmote.animated) {
//                        GlobalScope.launch {
//                            val bitmap = getBitmapFromURL(url)
//                            bitmapMemoryCache.put(it.name, bitmap)
//                        }
//                    } else {
//                        GlobalScope.launch {
//                            val gif = getGifFromURL(url)
//                            gifMemoryCache.put(it.name, gif)
//                        }
//                    }
//                }
//            }
//        }
//
//        private suspend fun retrieveProfile() {
//            val text: String = client.get("https://strims.gg/api/profile") {
//                header("Cookie", "jwt=$jwt")
//            }
//            GlobalScope.launch {
//                runOnUiThread {
//                    CurrentUser.user = Klaxon().parse(text)
//                    sendMessageText.hint = "Write something ${CurrentUser.user!!.username} ..."
//                    navHeaderUsername.text = CurrentUser.user!!.username
//                    invalidateOptionsMenu()
//                }
//            }
//        }

        suspend fun onConnect() = client.wss(
            host = "chat.strims.gg",
            path = "/ws",
            request = {
                retrieveCookie()
                if (jwt != null) {
                    Log.d("TAG", "Requesting with JWT: $jwt")
                    header("Cookie", "jwt=$jwt")
                }
            }
        ) {
//            if (jwt != null) {
//                retrieveProfile()
//            }
//            retrieveEmotes()
//            cacheEmotes()
//            retrieveOptions()
//            retrieveHistory()
//            retrievePrivateMessages()
                while (true) {
                    when (val frame = incoming.receive()) {
                        is Frame.Text -> {
                            Log.d("TAG", "FROM SOCKET: ${frame.readText()}")
//                            ChatActivity().addMessage(frame.readText())
//                            val msg: Message? = parseMessage(frame.readText())
//                            if (msg != null) {
//                                if (!CurrentUser.options!!.ignoreList.contains(msg.nick)) {
//                                    runOnUiThread {
//                                        if (msg.privMsg) {
//                                            adapter.add(
//                                                PrivateChatMessage(
//                                                    msg, true
//                                                )
//                                            )
//                                            if (CurrentUser.options!!.notifications) {
//                                                displayNotification(msg)
//                                            }
//                                        } else {
//                                            var consecutiveMessage = false
//                                            if (adapter.itemCount > 0) {
//                                                if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item || adapter.getItem(
//                                                        adapter.itemCount - 1
//                                                    ).layout == R.layout.chat_message_item_consecutive_nick
//                                                ) {
//                                                    val lastMessage =
//                                                        adapter.getItem(adapter.itemCount - 1) as ChatMessage
//                                                    consecutiveMessage =
//                                                        lastMessage.isNickSame(msg.nick)
//                                                }
//
//                                            }
//                                            if (msg.entities.emotes != null && msg.entities.emotes!!.isNotEmpty() && msg.entities.emotes!![0].combo > 1) {
//                                                if (msg.entities.emotes!![0].combo == 2) {
//                                                    adapter.removeGroupAtAdapterPosition(adapter.itemCount - 1)
//                                                    adapter.add(ChatMessageCombo(msg))
//                                                } else {
//                                                    if (adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
//                                                        val lastMessageCombo =
//                                                            adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
//                                                        lastMessageCombo.setCombo(msg.entities.emotes!![0].combo)
//                                                        adapter.notifyItemChanged(adapter.itemCount - 1)
//                                                    }
//                                                }
//
//                                            } else {
//                                                if (adapter.itemCount > 0 && adapter.getItem(
//                                                        adapter.itemCount - 1
//                                                    ).layout == R.layout.chat_message_item_emote_combo
//                                                ) {
//                                                    val lastMessage =
//                                                        adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
//                                                    lastMessage.state = 1
//                                                    adapter.notifyItemChanged(adapter.itemCount - 1)
//                                                }
//                                                adapter.add(
//                                                    ChatMessage(msg, consecutiveMessage)
//                                                )
//                                            }
//                                        }
//                                        val layoutTest =
//                                            recyclerViewChat.layoutManager as LinearLayoutManager
//                                        val lastItem = layoutTest.findLastVisibleItemPosition()
//                                        if (lastItem >= recyclerViewChat.adapter!!.itemCount - 3) {
//                                            recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
//                                        }
//                                    }
//                                }
//                            }
                        }
                        is Frame.Binary -> println(frame.readBytes())
                    }
                }
        }

//        private fun parseMessage(input: String): Message? {
//            val msg = input.split(" ", limit = 2)
//            when (msg[0]) {
//                "NAMES" -> {
//                    val names: NamesMessage = Klaxon().parse(msg[1])!!
//                    CurrentUser.users = names.users.toMutableList()
//                    CurrentUser.connectionCount = names.connectioncount
//                    runOnUiThread {
//                        if (adapter.itemCount > 0 && adapter.getItem(adapter.itemCount - 1).layout == R.layout.chat_message_item_emote_combo) {
//                            val lastMessage =
//                                adapter.getItem(adapter.itemCount - 1) as ChatMessageCombo
//                            lastMessage.state = 1
//                            adapter.notifyItemChanged(adapter.itemCount - 1)
//                        }
//                        adapter.add(
//                            ChatMessage(
//                                Message(
//                                    false,
//                                    "Info",
//                                    "Connected users: ${CurrentUser.connectionCount}"
//                                )
//                            )
//                        )
//                        recyclerViewChat.scrollToPosition(adapter.itemCount - 1)
//                    }
//                }
//                "JOIN" -> {
//                    val userJoin = Klaxon().parse<ChatUser>(msg[1])
//                    if (!CurrentUser.users!!.contains(userJoin)) {
//                        CurrentUser.users!!.add(userJoin!!)
//                    }
//                }
//                "QUIT" -> {
//                    val userQuit = Klaxon().parse<ChatUser>(msg[1])
//                    if (CurrentUser.users!!.contains(userQuit)) {
//                        CurrentUser.users!!.remove(userQuit)
//                    }
//                }
//                "PRIVMSG" -> {
//                    val message = Klaxon().parse<Message>(msg[1])!!
//                    message.privMsg = true
//                    return message
//                }
//                "MSG" -> {
//                    val message = Klaxon().parse<Message>(msg[1])!!
//                    if (CurrentUser.options!!.hideNsfw && message.entities.links!!.isNotEmpty()
//                        && message.entities.tags!!.isNotEmpty()
//                    ) {
//                        message.entities.tags!!.forEach {
//                            if (it.name == "nsfw" || it.name == "nsfl") {
//                                return null
//                            }
//                        }
//                    }
//
//                    if (CurrentUser.options!!.ignoreList.isNotEmpty()) {
//                        CurrentUser.options!!.ignoreList.forEach {
//                            if (message.nick == it) {
//                                return null
//                            }
//                            if (CurrentUser.options!!.harshIgnore) {
//                                if (message.data.contains(it)) {
//                                    return null
//                                }
//                            }
//                        }
//                    }
//                    return message
//                }
//                "MUTE" -> {
//                    val message = Klaxon().parse<Message>(msg[1])
//                    message!!.data = message.data.plus(" muted by Bot.")
//                    return message
//                }
//            }
//            return null
//        }
    }
}