package gg.strims.mobile

data class Message(val privMsg: Boolean, val nick: String, var data: String, var timestamp: Long) {

    constructor(): this(false,"", "", -1)

//    private var privMsg: Boolean? = null
//
//    constructor(privMsg: Boolean, nick: String, data: String, timestamp: Long) : this(nick, data, timestamp){
//        this.privMsg = privMsg
//    }
}