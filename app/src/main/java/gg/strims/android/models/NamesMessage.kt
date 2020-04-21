package gg.strims.android.models

class NamesMessage(val users: List<ChatUser>, val connectioncount: Int) {

    constructor(): this(listOf(), 0)
}