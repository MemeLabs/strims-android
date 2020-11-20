package gg.strims.android.viewmodels

import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import gg.strims.android.models.Message
import gg.strims.android.models.ViewerState
import gg.strims.android.notifyObserver

class ChatViewModel: ViewModel() {
    var streamsSocketIntent: Intent? = null
    var chatSocketIntent: Intent? = null

    private fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }

    var users = mutableListOf<String>()
    var viewerStates = MutableLiveData<MutableList<ViewerState>>()
    var oldMessageCount = 0
    var messages = MutableLiveData<MutableList<Message>>().default(mutableListOf())
    var currentMessage: String? = null

    fun addMessage(message: Message) {
        messages.value?.add(message)
        messages.notifyObserver()
    }

    fun updateViewerState(oldState: ViewerState, newState: ViewerState) {
        viewerStates.value!![viewerStates.value!!.indexOf(oldState)] = newState
        viewerStates.notifyObserver()
    }
}