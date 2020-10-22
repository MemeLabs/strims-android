package gg.strims.android.viewmodels

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder

class ChatViewModel: ViewModel() {

    var chatAdapter: GroupAdapter<GroupieViewHolder>? = null
    var streamsSocketIntent: Intent? = null
    var chatSocketIntent: Intent? = null
    var visibleStream: String? = null
}