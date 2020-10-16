package gg.strims.android

import android.app.IntentService
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.util.Log
import android.webkit.CookieManager
import com.beust.klaxon.Klaxon
import gg.strims.android.models.EmotesParsed
import gg.strims.android.models.Options
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.*
import pl.droidsonroids.gif.GifDrawable
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

@KtorExperimentalAPI
class ChatService: Service() {

    private var job: Job? = null

    init {
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            job = GlobalScope.launch {
                Log.d("TAG", "Launching socket...")
                ChatClient().onConnect()
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return START_NOT_STICKY
    }

    companion object {
        private lateinit var instance: ChatService

        fun stopSocket() {
            Log.d("TAG", "Stopping socket...")
            instance.job?.cancel()
        }
    }

    override fun onDestroy() {
        Log.d("TAG", "Destroying...")
        job!!.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

//    override fun onHandleIntent(intent: Intent?) {
//        try {
//            job = GlobalScope.launch {
//                Log.d("TAG", "Launching socket")
//                ChatClient().onConnect()
//            }
//        } catch (e: InterruptedException) {
//            Thread.currentThread().interrupt()
//        }
//    }

    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

        private fun retrieveHistory() {
            val messageHistory =
                Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
            val intent = Intent("gg.strims.android.MESSAGE_HISTORY")
            val arrayList = arrayListOf<String>()
            if (messageHistory != null) {
                arrayList.addAll(messageHistory)
            }
            intent.putStringArrayListExtra("gg.strims.android.MESSAGE_HISTORY_TEXT", ArrayList(arrayList))
            sendBroadcast(intent)
        }

        private suspend fun retrieveEmotes() {
            val text: String = client.get("https://chat.strims.gg/emote-manifest.json")
            val emotesParsed: EmotesParsed = Klaxon().parse(text)!!
            CurrentUser.emotes = emotesParsed.emotes.toMutableList()
            sendBroadcast(Intent("gg.strims.android.EMOTES"))
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

        private suspend fun retrieveProfile() {
            val text: String = client.get("https://strims.gg/api/profile") {
                header("Cookie", "jwt=$jwt")
            }
            CurrentUser.user = Klaxon().parse(text)
            sendBroadcast(Intent("gg.strims.android.PROFILE"))
        }

        private fun retrieveOptions() {
            val file = baseContext.getFileStreamPath("filename.txt")
            if (file.exists()) {
                val fileInputStream = openFileInput("filename.txt")
                val inputStreamReader = InputStreamReader(fileInputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                val stringBuilder = StringBuilder()
                var text: String? = null
                while ({ text = bufferedReader.readLine(); text }() != null) {
                    stringBuilder.append(text)
                }
                CurrentUser.options = Klaxon().parse(stringBuilder.toString())
            } else {
                CurrentUser.options = Options()
            }
        }

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
            if (jwt != null) {
                retrieveProfile()
            }
            retrieveEmotes()
            retrieveOptions()
            retrieveHistory()
//            retrievePrivateMessages()

            val broadcastReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent != null) {
                        if (intent.action == "gg.strims.android.SEND_MESSAGE") {
                            val message = intent.getStringExtra("gg.strims.android.SEND_MESSAGE_TEXT")
                            if (message != null) {
                                launch {
                                    send(message)
                                }
                            }
                        }
                    }
                }
            }

            val intentFilter = IntentFilter("gg.strims.android.SEND_MESSAGE")

            registerReceiver(broadcastReceiver, intentFilter)

            while (true) {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        val intent = Intent("gg.strims.android.MESSAGE")
                        intent.putExtra("gg.strims.android.MESSAGE_TEXT", frame.readText())
                        sendBroadcast(intent)
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }
    }
}