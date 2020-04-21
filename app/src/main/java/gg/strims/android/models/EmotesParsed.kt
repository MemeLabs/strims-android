package gg.strims.android.models

class EmotesParsed(val emotes: List<Emote>, val css: String) {

    constructor(): this(listOf(), "")
}