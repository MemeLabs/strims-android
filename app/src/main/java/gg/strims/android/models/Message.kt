package gg.strims.android.models

data class Message(
    var privMsg: Boolean,
    var nick: String,
    var data: String,
    var timestamp: Long = System.currentTimeMillis(),
    var features: Array<String> = arrayOf(""),
    var entities: Entities = Entities()
) {

    constructor(): this(
        false,
        "",
        "",
        -1,
        arrayOf<String>(),
        Entities()
    )
}