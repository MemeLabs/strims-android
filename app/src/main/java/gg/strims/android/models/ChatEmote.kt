package gg.strims.android.models

class ChatEmote(var name: String, var bounds: List<Int>, var modifiers: List<String>) {

    constructor(): this("", listOf(), listOf())
}