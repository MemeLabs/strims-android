package gg.strims.android.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import gg.strims.android.models.Message

class ChatViewModel: ViewModel() {
    var streamsSocketIntent: Intent? = null
    var chatSocketIntent: Intent? = null

    private fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

    private fun <T> MutableLiveData<T>.notifyObserver() {
        this.value = this.value
    }

    var oldMessageCount = 0
    var messages = MutableLiveData<MutableList<Message>>().default(mutableListOf())
//    var latestMessage = MutableLiveData<Message?>()

    fun addMessage(message: Message) {
        messages.value?.add(message)
//        latestMessage.value = message
        messages.notifyObserver()
    }
}