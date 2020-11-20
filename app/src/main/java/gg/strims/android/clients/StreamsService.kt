package gg.strims.android.clients

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import gg.strims.android.CurrentUser
import gg.strims.android.R
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
        val channelId = createNotificationChannel("strims_chat_service", "Strims Chat Service")
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setColor(Color.parseColor("#F58851"))
            .setColorized(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setPriority(PRIORITY_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        startForeground(101, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val channel = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        channel.lightColor = Color.BLUE
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
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
//                        Log.d("TAG", "Sending broadcast with ${frame.readText()}")
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            }
        }
    }
}