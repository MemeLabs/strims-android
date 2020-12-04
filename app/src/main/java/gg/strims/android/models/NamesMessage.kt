package gg.strims.android.models

class NamesMessage(val users: List<ChatUser>, val connectioncount: Int) {

    constructor(): this(listOf(), 0)
}

class ChatUser(var nick: String, var features: Array<String>) {

    constructor(): this("", arrayOf<String>())
}