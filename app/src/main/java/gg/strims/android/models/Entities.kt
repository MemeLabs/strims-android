package gg.strims.android.models

import androidx.room.TypeConverter
import com.beust.klaxon.Klaxon
import com.google.gson.Gson

class Entities(
    var emotes: List<ChatEmote>?,
    var spoilers: List<Bounds>?,
    var codes: List<Bounds>?,
    var greentext: Bounds?,
    var me: Bounds?,
    var links: List<Link>?,
    var nicks: List<Nick>?,
    var tags: List<Tag>?
) {

    constructor(): this(listOf(), listOf(), listOf(), Bounds(), Bounds(), listOf(), listOf(), listOf())
}

class ChatEmote(
    var name: String,
    var bounds: List<Int>,
    var modifiers: List<String>,
    var combo: Int
) {

    constructor() : this("", listOf(), listOf(), 0)
}

class Bounds(var bounds: List<Int>) {

    constructor(): this(listOf())
}

class Link(var url: String?, var bounds: List<Int>) {

    constructor(): this("", listOf())
}

class Nick(var nick: String, var bounds: List<Int>) {

    constructor(): this("", listOf())
}

class Tag(var name: String?, var bounds: List<Int>) {

    constructor(): this("", listOf())
}