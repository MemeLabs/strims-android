package gg.strims.android.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PrivateMessagesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMessage(message: PrivateMessage)

    @Query("SELECT * FROM private_messages_table ORDER BY timestamp DESC")
    fun readAllData(): LiveData<List<PrivateMessage>>

    @Query("DELETE FROM private_messages_table")
    suspend fun deleteAllMessages()

    @Query("SELECT * FROM private_messages_table WHERE targetNick = :nick")
    fun findMessages(nick: String): LiveData<List<PrivateMessage>>

    @Delete
    suspend fun deleteMessage(message: PrivateMessage)
}