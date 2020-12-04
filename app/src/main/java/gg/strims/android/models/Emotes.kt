package gg.strims.android.models

class Emote(val name: String, val versions: List<EmoteVersion>) {

    constructor(): this("", listOf())
}

class EmoteVersion(val path: String, val animated: Boolean, val dimensions: Dimension, val size: String) {

    constructor(): this("", false, Dimension(), "")
}

class EmotesParsed(val emotes: List<Emote>, val css: String) {

    constructor(): this(listOf(), "")
}

class Dimension(val height: Int, val width: Int) {

    constructor(): this(0, 0)
}