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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Message

        if (privMsg != other.privMsg) return false
        if (nick != other.nick) return false
        if (data != other.data) return false
        if (timestamp != other.timestamp) return false
        if (!features.contentEquals(other.features)) return false
        if (entities != other.entities) return false

        return true
    }

    override fun hashCode(): Int {
        var result = privMsg.hashCode()
        result = 31 * result + nick.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + features.contentHashCode()
        result = 31 * result + entities.hashCode()
        return result
    }
}