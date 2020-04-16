package gg.strims.android.models

class Emote(val name: String, val versions: List<EmoteVersion>) {

    constructor(): this("", listOf())
}