package gg.strims.android.models

import android.graphics.Color
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlin.random.Random

@Parcelize
class Stream(
    val afk: Boolean,
    var afk_rustlers: Int,
    val channel: String,
    val hidden: Boolean,
    val id: Long,
    val live: Boolean,
    val nsfw: Boolean,
    val overrustle_id: String,
    val promoted: Boolean,
    var rustlers: Int,
    val service: String,
    val thumbnail: String,
    val title: String,
    val viewers: Int
) : Parcelable {
    constructor() : this(
        false,
        0,
        "test_channel",
        false,
        123,
        true,
        false,
        "test_id",
        false,
        456,
        "test_service",
        "https://thumbnail.angelthump.com/thumbnails/psrngafk.jpeg",
        "test_title",
        789
    )

    val colour = Color.argb(255, Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
}