package gg.strims.mobile

class ChatUser(var nick: String, var features: Array<String>) {

    constructor(): this("", arrayOf<String>())
}