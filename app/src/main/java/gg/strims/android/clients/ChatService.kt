package gg.strims.android.clients

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.webkit.CookieManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import com.beust.klaxon.Klaxon
import gg.strims.android.MainActivity
import gg.strims.android.R
import gg.strims.android.models.ViewerState
import gg.strims.android.singletons.CurrentUser
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
import java.net.URL

@KtorExperimentalAPI
class ChatService: Service() {

    private var job: Job? = null

    private val broadcastReceivers = mutableListOf<BroadcastReceiver>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground()

        try {
            job = GlobalScope.launch {
                try {
                    Log.d("TAG", "STARTING CHAT SERVICE ${(System.currentTimeMillis() - CurrentUser.time)}")
                    ChatClient().onConnect()
                } catch (e: ClosedReceiveChannelException) {
                    job?.cancel()
                    Log.d("TAG", "ChatSocket onClose ${e.localizedMessage}")
                    sendBroadcast(Intent("gg.strims.android.CHAT_SOCKET_CLOSE"))
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            sendBroadcast(Intent("gg.strims.android.CHAT_SOCKET_CLOSE"))
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("TAG", "Destroying Chat Service...")
        broadcastReceivers.forEach {
            unregisterReceiver(it)
        }
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val channelId = createNotificationChannel("strims_chat_service", "Strims Chat Service")
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    inner class ChatClient {

        private var jwt: String? = null

        private val client = HttpClient {
            install(WebSockets)
        }

        private fun retrieveHistory() {
            val messageHistory =
                Klaxon().parseArray<String>(URL("https://chat.strims.gg/api/chat/history").readText())
            val viewerStates =
                Klaxon().parseArray<ViewerState>(URL("https://chat.strims.gg/api/chat/viewer-states").readText())
                    ?.toMutableList()
            val intent = Intent("gg.strims.android.MESSAGE_HISTORY")
            val historyArrayList = arrayListOf<String>()
            if (messageHistory != null) {
                historyArrayList.addAll(messageHistory)
            }
            val viewerStatesArrayList = arrayListOf<ViewerState>()
            if (viewerStates != null) {
                viewerStatesArrayList.addAll(viewerStates)
            }
            intent.putStringArrayListExtra(
                "gg.strims.android.MESSAGE_HISTORY_TEXT", ArrayList(
                    historyArrayList
                )
            )
            intent.putParcelableArrayListExtra(
                "gg.strims.android.VIEWERSTATES",
                viewerStatesArrayList
            )
            sendBroadcast(intent)
        }

        private fun retrieveCookie() {
            val cookieManager = CookieManager.getInstance()
            val cookies = cookieManager.getCookie("https://strims.gg")
            cookieManager.flush()
            if (cookies != null) {
                val jwt = cookies.substringAfter("jwt=").substringBefore(" ")
                if (jwt != cookies) {
                    this.jwt = jwt
                }
            }
        }

        private suspend fun retrieveProfile() {
            val text: String = client.get("https://strims.gg/api/profile") {
                header("Cookie", "jwt=$jwt")
            }
            CurrentUser.user = Klaxon().parse(text)
            val intent = Intent("gg.strims.android.PROFILE")
            intent.putExtra("gg.strims.android.JWT", jwt)
            sendBroadcast(intent)
        }

        suspend fun onConnect() = client.wss(
            host = "chat.strims.gg",
            path = "/ws",
            request = {
                retrieveCookie()
                if (jwt != null) {
                    Log.d("TAG", "Requesting with JWT: $jwt")
                    header("Cookie", "jwt=$jwt")
                    CoroutineScope(IO).launch {
                        retrieveProfile()
                    }
                }
            }
        ) {
            Log.d("TAG", "CONNECTED ${(System.currentTimeMillis() - CurrentUser.time)}")

            retrieveHistory()
            Log.d("TAG", "HISTORY ENDING ${(System.currentTimeMillis() - CurrentUser.time)}")

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
                        } else if (intent.action == "gg.strims.android.SEND_NOT_MESSAGE") {
                            val nick = intent.getStringExtra("gg.strims.android.SEND_MESSAGE_NICK")
                            val remoteInput = RemoteInput.getResultsFromIntent(intent)
                            val message = remoteInput.getCharSequence(MainActivity.NOTIFICATION_REPLY_KEY)
                            if (nick != null) {
                                launch {
                                    send("PRIVMSG {\"nick\":\"$nick\", \"data\":\"$message\"}")

                                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                                    nm.cancel(MainActivity.NOTIFICATION_ID)
                                }
                            }
                        }
                    }
                }
            }

            val intentFilter = IntentFilter("gg.strims.android.SEND_MESSAGE")
            intentFilter.addAction("gg.strims.android.SEND_NOT_MESSAGE")
            registerReceiver(broadcastReceiver, intentFilter)
            broadcastReceivers.add(broadcastReceiver)

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