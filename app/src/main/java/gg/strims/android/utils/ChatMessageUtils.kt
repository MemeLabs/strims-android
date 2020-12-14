package gg.strims.android.utils

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.*
import android.view.View
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import gg.strims.android.customspans.CenteredImageSpan
import gg.strims.android.customspans.ColouredUnderlineSpan
import gg.strims.android.callbacks.DrawableCallback
import gg.strims.android.customspans.NoUnderlineClickableSpan
import gg.strims.android.models.Message
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import pl.droidsonroids.gif.GifDrawable

@KtorExperimentalAPI
fun createMessageTextView(
    context: Context,
    messageData: Message,
    messageTextView: TextView,
    emotes: Boolean = CurrentUser.optionsLiveData.value?.emotes!!,
    greentext: Boolean = CurrentUser.optionsLiveData.value?.greentext!!,
    links: Boolean = true,
    codes: Boolean = true,
    spoilers: Boolean = true,
    me: Boolean = true
) {
    val ssb = SpannableStringBuilder(messageData.data)

    /** Emotes **/
    if (emotes) {
        if (messageData.entities.emotes != null && messageData.entities.emotes!!.isNotEmpty() && messageData.entities.emotes!![0].name != "") {
            messageData.entities.emotes!!.forEach {
                var animated = false
                CurrentUser.emotes?.forEach { it2 ->
                    if (it.name == it2.name && it2.versions[0].animated) {
                        animated = true
                        return@forEach
                    }
                }
                if (!animated) {
                    var bitmap: Bitmap? = null
                    while (bitmap == null) {
                        bitmap = CurrentUser.bitmapMemoryCache[it.name]
                    }
                    var width = bitmap.width * 0.75

                    if (it.modifiers.contains("wide")) {
                        width = (bitmap.width * 1.5)
                    }

                    var height = bitmap.height * 0.75

                    if (it.modifiers.contains("smol")) {
                        width *= 0.5
                        height *= 0.5
                    }

                    if (it.modifiers.contains("flip")) {
                        bitmap = bitmap.flip(180f)
                    }

                    if (it.modifiers.contains("mirror")) {
                        bitmap = bitmap.mirror()
                    }
                    val resized =
                        Bitmap.createScaledBitmap(bitmap, width.toInt(), height.toInt(), false)
                    ssb.setSpan(
                        CenteredImageSpan(
                            context,
                            resized
                        ),
                        it.bounds[0],
                        it.bounds[1],
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                } else {

                    var gif: GifDrawable? = null
                    while (gif == null) {
                        gif = CurrentUser.gifMemoryCache[it.name]
                    }
//                    gif.loopCount = 1
                    gif.callback = DrawableCallback(messageTextView)
                    gif.setBounds(0, 0, gif.minimumWidth, gif.minimumHeight)
                    gif.start()
                    val animatedEmote = ImageSpan(gif)

                    ssb.setSpan(
                        animatedEmote,
                        it.bounds[0],
                        it.bounds[1],
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                }
            }
        }
    }

    /** Greentext **/
    if (messageData.entities.greentext!!.bounds.isNotEmpty() && greentext) {
        ssb.setSpan(
            ForegroundColorSpan(Color.parseColor("#789922")),
            messageData.entities.greentext!!.bounds[0],
            messageData.entities.greentext!!.bounds[1],
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    }

    /** Links **/
    if (messageData.entities.links!!.isNotEmpty() && links) {
        messageData.entities.links!!.forEach {
            val clickSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (messageData.entities.spoilers!!.isNotEmpty()) {
                        messageData.entities.spoilers!!.forEach { it2 ->
                            if (it.bounds[0] >= it2.bounds[0] && it.bounds[1] <= it2.bounds[1]) {
                                val span3 = ssb.getSpans(
                                    it.bounds[0],
                                    it.bounds[1],
                                    ForegroundColorSpan::class.java
                                )
                                if (span3[span3.size - 1].foregroundColor == Color.parseColor(
                                        "#AAAAAA"
                                    ) ||
                                    span3[span3.size - 1].foregroundColor == Color.parseColor(
                                        "#03DAC5"
                                    )
                                ) {
                                    var webpage = Uri.parse(it.url)

                                    if (it.url!!.startsWith("strims.gg/") && !it.url!!.startsWith("strims.gg/m3u8")) {
                                        var channel = it.url!!.substringAfter("strims.gg/")
                                        if (channel.startsWith("angelthump")) {
                                            channel = channel.substringAfter("angelthump/")
                                        }
                                        val intent = Intent("gg.strims.android.SHOWSTREAM")
                                        intent.putExtra("gg.strims.android.STREAM", channel)
                                        context.sendBroadcast(intent)
                                        return
                                    } else if (!it.url!!.startsWith("http://") && !it.url!!.startsWith(
                                            "https://"
                                        )
                                    ) {
                                        webpage = Uri.parse("http://${it.url}")
                                    }

                                    val intent = Intent(Intent.ACTION_VIEW, webpage)
                                    context.startActivity(intent)
                                }
                            }
                        }
                    } else {
                        var webpage = Uri.parse(it.url)

                        if (it.url!!.startsWith("strims.gg/") && !it.url!!.startsWith("strims.gg/m3u8")) {
                            var channel = it.url!!.substringAfter("strims.gg/")
                            if (channel.startsWith("angelthump")) {
                                channel = channel.substringAfter("angelthump/")
                            }
                            val intent = Intent("gg.strims.android.SHOWSTREAM")
                            intent.putExtra("gg.strims.android.STREAM", channel)
                            context.sendBroadcast(intent)
                            return
                        } else if (!it.url!!.startsWith("http://") && !it.url!!.startsWith(
                                "https://"
                            )
                        ) {
                            webpage = Uri.parse("http://${it.url}")
                        }

                        val intent = Intent(Intent.ACTION_VIEW, webpage)
                        context.startActivity(intent)
                    }
                }
            }
            if (messageData.entities.codes!!.isNotEmpty()) {
                messageData.entities.codes!!.forEach { it2 ->
                    if (it.bounds[0] >= it2.bounds[0] && it.bounds[1] <= it2.bounds[1]) {
                        return@forEach
                    } else {
                        ssb.setSpan(
                            clickSpan,
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }
            } else {
                ssb.setSpan(
                    clickSpan,
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
        }

        with (messageData.data) {
            when {
                contains("nsfl") -> {
                    messageData.entities.links!!.forEach {
                        ssb.setSpan(
                            ColouredUnderlineSpan(Color.parseColor("#FFFF00")),
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }

                contains("nsfw") -> {
                    messageData.entities.links!!.forEach {
                        ssb.setSpan(
                            ColouredUnderlineSpan(Color.parseColor("#FF2D00")),
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }

                contains("weeb") -> {
                    messageData.entities.links!!.forEach {
                        ssb.setSpan(
                            ColouredUnderlineSpan(Color.parseColor("#FF00EE")),
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }

                contains("loud") -> {
                    messageData.entities.links!!.forEach {
                        ssb.setSpan(
                            ColouredUnderlineSpan(Color.parseColor("#0022FF")),
                            it.bounds[0],
                            it.bounds[1],
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
                }
            }
        }
    }

    /** Codes **/
    if (messageData.entities.codes!!.isNotEmpty() && codes) {
        messageData.entities.codes!!.forEach {
            with (ssb) {
                setSpan(
                    BackgroundColorSpan(Color.parseColor("#2B2B2B")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(Color.parseColor("#AAAAAA")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    TypefaceSpan("monospace"),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[0],
                    it.bounds[0] + 1,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[1] - 1,
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                if (messageData.entities.links!!.isNotEmpty()) {
                    messageData.entities.links!!.forEach { it2 ->
                        if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                            val span3 = getSpans(
                                it2.bounds[0],
                                it2.bounds[1],
                                ColouredUnderlineSpan::class.java
                            )
                            if (span3.isNotEmpty()) {
                                span3[span3.size - 1].color = Color.parseColor("#00000000")
                            }
                        }
                    }
                }
            }
        }
    }

    /** Spoilers **/
    if (messageData.entities.spoilers!!.isNotEmpty() && spoilers) {
        messageData.entities.spoilers!!.forEach {

            if (messageData.entities.emotes!!.isNotEmpty() && CurrentUser.optionsLiveData.value?.emotes!!) {
                messageData.entities.emotes!!.forEach { emote ->
                    if (emote.bounds[0] >= it.bounds[0] && emote.bounds[1] <= it.bounds[1]) {
                        val emoteSpan = ssb.getSpans(
                            emote.bounds[0], emote.bounds[1],
                            ImageSpan::class.java
                        )

                        if (emoteSpan.isNotEmpty()) {
                            emoteSpan[0].drawable.alpha = 0
                        }
                    }
                }
            }

            val span1: NoUnderlineClickableSpan = object : NoUnderlineClickableSpan() {
                override fun onClick(widget: View) {

                    if (messageData.entities.emotes!!.isNotEmpty() && CurrentUser.optionsLiveData.value?.emotes!!) {
                        messageData.entities.emotes!!.forEach { emote ->
                            if (emote.bounds[0] >= it.bounds[0] && emote.bounds[1] <= it.bounds[1]) {
                                val emoteSpan = ssb.getSpans(
                                    emote.bounds[0], emote.bounds[1],
                                    ImageSpan::class.java
                                )

                                if (emoteSpan.isNotEmpty()) {
                                    emoteSpan[0].drawable.alpha = 255
                                }
                            }
                        }
                    }

                    val span = ssb.getSpans(
                        it.bounds[0], it.bounds[1],
                        ForegroundColorSpan::class.java
                    )
                    if (span[span.size - 1].foregroundColor == Color.parseColor("#00000000")) {
                        ssb.setSpan(
                            ForegroundColorSpan(Color.parseColor("#AAAAAA")),
                            it.bounds[0] + 2,
                            it.bounds[1] - 2,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        if (messageData.entities.links!!.isNotEmpty()) {
                            messageData.entities.links!!.forEach { it2 ->
                                if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                                    ssb.setSpan(
                                        ForegroundColorSpan(Color.parseColor("#03DAC5")),
                                        it2.bounds[0],
                                        it2.bounds[1],
                                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                    )
                                    messageData.entities.tags!!.forEach { it3 ->
                                        when (it3.name) {
                                            "nsfl" -> {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#FFFF00")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            }

                                            "nsfw" -> {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#FF2D00")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            }

                                            "weeb" -> {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#FF00EE")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            }

                                            "loud" -> {
                                                ssb.setSpan(
                                                    ColouredUnderlineSpan(Color.parseColor("#0022FF")),
                                                    it2.bounds[0],
                                                    it2.bounds[1],
                                                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (span[span.size - 1].foregroundColor == Color.parseColor("#AAAAAA") ||
                        span[span.size - 1].foregroundColor == Color.parseColor("#03DAC5")
                    ) {
                        ssb.setSpan(
                            ForegroundColorSpan(Color.parseColor("#00000000")),
                            it.bounds[0] + 2,
                            it.bounds[1] - 2,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )

                        if (messageData.entities.emotes!!.isNotEmpty() && CurrentUser.optionsLiveData.value?.emotes!!) {
                            messageData.entities.emotes!!.forEach { emote ->
                                if (emote.bounds[0] >= it.bounds[0] && emote.bounds[1] <= it.bounds[1]) {
                                    val emoteSpan = ssb.getSpans(
                                        emote.bounds[0], emote.bounds[1],
                                        ImageSpan::class.java
                                    )

                                    if (emoteSpan.isNotEmpty()) {
                                        emoteSpan[0].drawable.alpha = 0
                                    }
                                }
                            }
                        }

                        if (messageData.entities.links!!.isNotEmpty()) {
                            messageData.entities.links!!.forEach { it2 ->
                                if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                                    val span3 = ssb.getSpans(
                                        it2.bounds[0],
                                        it2.bounds[1],
                                        ColouredUnderlineSpan::class.java
                                    )
                                    if (span3.isNotEmpty()) {
                                        span3[span3.size - 1].color =
                                            Color.parseColor("#00000000")
                                    }
                                }
                            }
                        }
                    }
                    messageTextView.setText(
                        ssb,
                        TextView.BufferType.SPANNABLE
                    )
                }
            }

            if (messageData.entities.links!!.isNotEmpty()) {
                messageData.entities.links!!.forEach { it2 ->
                    if (it2.bounds[0] >= it.bounds[0] && it2.bounds[1] <= it.bounds[1]) {
                        val span3 = ssb.getSpans(
                            it2.bounds[0],
                            it2.bounds[1],
                            ColouredUnderlineSpan::class.java
                        )
                        if (span3.isNotEmpty()) {
                            span3[span3.size - 1].color = Color.parseColor("#00000000")
                        }
                    }
                }
            }
            with (ssb) {
                setSpan(
                    span1,
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[0],
                    it.bounds[0] + 2,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    RelativeSizeSpan(0f),
                    it.bounds[1] - 2,
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    BackgroundColorSpan(Color.parseColor("#353535")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                setSpan(
                    ForegroundColorSpan(Color.parseColor("#00000000")),
                    it.bounds[0],
                    it.bounds[1],
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
            }
        }
    }
    /** /me **/
    if (messageData.entities.me!!.bounds.isNotEmpty() && me) {
        messageTextView.setTypeface(
            Typeface.DEFAULT,
            Typeface.ITALIC
        )
        ssb.setSpan(
            RelativeSizeSpan(0f),
            0,
            3,
            Spannable.SPAN_INCLUSIVE_INCLUSIVE
        )
    } else {
        messageTextView.typeface = Typeface.DEFAULT
    }
    messageTextView.setText(ssb, TextView.BufferType.SPANNABLE)
}