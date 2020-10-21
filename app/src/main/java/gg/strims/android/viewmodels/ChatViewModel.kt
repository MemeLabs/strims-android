package gg.strims.android.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class ChatViewModel: ViewModel() {

    var chatLiveData: MutableLiveData<GroupAdapter<GroupieViewHolder>>? = null

    var chatAdapter: GroupAdapter<GroupieViewHolder>? = null

    fun getChatData(): LiveData<GroupAdapter<GroupieViewHolder>>? {
        return chatLiveData
    }
}