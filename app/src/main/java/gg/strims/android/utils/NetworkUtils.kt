package gg.strims.android.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import pl.droidsonroids.gif.GifDrawable
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

fun getBitmapFromURL(src: String?): Bitmap? {
    return try {
        val url = URL(src)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input: InputStream = connection.inputStream
        BitmapFactory.decodeStream(input)
    } catch (e: IOException) {
        null
    }
}

fun getGifFromURL(src: String?): GifDrawable? {
    return try {
        val url = URL(src)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connect()
        val input: InputStream = connection.inputStream
        val bis = BufferedInputStream(input)
        GifDrawable(bis)
    } catch (e: IOException) {
        null
    }
}