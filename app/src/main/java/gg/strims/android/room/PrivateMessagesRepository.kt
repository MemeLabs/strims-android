package gg.strims.android.room

import androidx.lifecycle.LiveData

class PrivateMessagesRepository(private val privateMessagesDao: PrivateMessagesDao) {

    val privateMessages: LiveData<List<PrivateMessage>> = privateMessagesDao.readAllData()

    suspend fun addMessage(message: PrivateMessage) {
        privateMessagesDao.addMessage(message)
    }

    suspend fun deleteMessage(message: PrivateMessage) {
        privateMessagesDao.deleteMessage(message)
    }

    suspend fun deleteAllMessages() {
        privateMessagesDao.deleteAllMessages()
    }

    fun findMessages(nick: String) {
        privateMessagesDao.findMessages(nick)
    }
}