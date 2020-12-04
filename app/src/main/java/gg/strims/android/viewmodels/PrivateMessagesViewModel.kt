package gg.strims.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import gg.strims.android.room.PrivateMessage
import gg.strims.android.room.PrivateMessagesDatabase
import gg.strims.android.room.PrivateMessagesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrivateMessagesViewModel(application: Application): AndroidViewModel(application) {

    val privateMessages: LiveData<List<PrivateMessage>>
    private val repository: PrivateMessagesRepository

    init {
        val userDao = PrivateMessagesDatabase.getDatabase(application).privateMessagesDao()
        repository = PrivateMessagesRepository(userDao)
        privateMessages = repository.privateMessages
    }

    fun addMessage(message: PrivateMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addMessage(message)
        }
    }

    fun deleteMessage(message: PrivateMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMessage(message)
        }
    }

    fun deleteAllMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllMessages()
        }
    }

    fun findMessages(nick: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.findMessages(nick)
        }
    }
}