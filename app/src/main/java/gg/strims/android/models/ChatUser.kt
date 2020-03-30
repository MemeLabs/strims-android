package gg.strims.android.models

class ChatUser(var nick: String, var features: Array<String>) {

    constructor(): this("", arrayOf<String>())
}