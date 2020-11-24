package gg.strims.android.adapters

import android.widget.EditText
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import gg.strims.android.models.ViewerState

class CustomAdapter: GroupAdapter<GroupieViewHolder>() {
    var tempHighlightNick: MutableList<String>? = null
    var viewerStates: MutableList<ViewerState>? = null
    var sendMessageText: EditText? = null
}