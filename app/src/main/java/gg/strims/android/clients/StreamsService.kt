package gg.strims.android.clients

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import gg.strims.android.R
import gg.strims.android.singletons.CurrentUser
import io.ktor.client.*
import io.ktor.client.features.websocket.*
import io.ktor.http.cio.websocket.*
import io.ktor.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch

@KtorExperimentalAPI
class StreamsService: Service() {

    private var job: Job? = null

    private val broadcastReceivers = mutableListOf<BroadcastReceiver>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground()

        try {
            job = GlobalScope.launch {
                try {
                    Log.d("TAG", "STARTING STREAMS SERVICE ${(System.currentTimeMillis() - CurrentUser.time)}")
                    StreamsClient().onConnect()
                } catch (e: ClosedReceiveChannelException) {
                    job?.cancel()
                    Log.d("TAG", "StreamsSocket onClose ${e.localizedMessage}")
                    sendBroadcast(Intent("gg.strims.android.STREAMS_SOCKET_CLOSE"))
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            sendBroadcast(Intent("gg.strims.android.STREAMS_SOCKET_CLOSE"))
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("TAG", "Destroying Streams Service...")
        broadcastReceivers.forEach {
            unregisterReceiver(it)
        }
        job?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForeground() {
        val channelId = createNotificationChannel()
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    private fun createNotificationChannel(): String {
        val channel = NotificationChannel(
            "strims_chat_service",
            "Strims Chat Service", NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channel.id
    }

    inner class StreamsClient {

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
                        val intent = Intent("gg.strims.android.STREAMS")
                        intent.putExtra("gg.strims.android.STREAMS_TEXT", frame.readText())
                        sendBroadcast(intent)
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }
    }
}