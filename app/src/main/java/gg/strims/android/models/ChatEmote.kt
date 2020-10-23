package gg.strims.android.models

class ChatEmote(
    var name: String,
    var bounds: List<Int>,
    var modifiers: List<String>,
    var combo: Int
) {
    constructor() : this("", listOf(), listOf(), 0)
}