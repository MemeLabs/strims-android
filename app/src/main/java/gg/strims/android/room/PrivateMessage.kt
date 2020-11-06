package gg.strims.android.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import gg.strims.android.models.Entities
import gg.strims.android.models.Message

@TypeConverters(Converters::class)
@Entity(tableName = "private_messages_table")
data class PrivateMessage(
    @PrimaryKey(autoGenerate = true)
    var id: Int,
    var privMsg: Boolean,
    var nick: String,
    var data: String,
    var timestamp: Long = System.currentTimeMillis(),
    var features: Array<String>,
    var entities: Entities,
    var targetNick: String
) {

    fun toMessage(): Message {
        return Message(privMsg, nick, data, timestamp, features, entities, targetNick)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrivateMessage

        if (id != other.id) return false
        if (privMsg != other.privMsg) return false
        if (nick != other.nick) return false
        if (data != other.data) return false
        if (timestamp != other.timestamp) return false
        if (!features.contentEquals(other.features)) return false
        if (entities != other.entities) return false
        if (targetNick != other.targetNick) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + privMsg.hashCode()
        result = 31 * result + nick.hashCode()
        result = 31 * result + data.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + features.contentHashCode()
        result = 31 * result + entities.hashCode()
        result = 31 * result + targetNick.hashCode()
        return result
    }
}