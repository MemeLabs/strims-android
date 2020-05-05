package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Stream
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.fragment_whispers.*
import kotlinx.android.synthetic.main.fragment_whispers.view.*
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import kotlinx.android.synthetic.main.whisper_item.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*
import java.io.Serializable

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class WhispersFragment : Fragment() {

    private val whispersAdapter = GroupAdapter<GroupieViewHolder>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_whispers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewWhispers.layoutManager = layoutManager
        recyclerViewWhispers.adapter = whispersAdapter
        backWhispers.setOnClickListener {
            backWhispers.visibility = View.GONE
        }


    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.privateMessages != null) {
            whispersAdapter.clear()
            CurrentUser.privateMessages!!.forEach {
                var b = true
                for (i in 0 until whispersAdapter.itemCount) {

                    val item = whispersAdapter.getItem(i) as WhisperUserItem
                    if (item.nick == it.getNick()) {
                        item.addMessage(it)
                        b = false
                        break
                    }
                }
                //streamsAdapter.add(StreamItem(it))
                if (b) {

                    whispersAdapter.add(WhisperUserItem(it))

                }
            }
            // showWhispers()
        }
        recyclerViewWhispers.scrollToPosition(0)
        whispersAdapter.notifyDataSetChanged()
    }

    inner class WhisperUserItem(initialMessage: ChatActivity.WhisperMessageItem? = null) :
        Item<GroupieViewHolder>(), Serializable {
        override fun getLayout(): Int {
            return R.layout.whisper_user_item
        }

        private var pMessages: MutableList<ChatActivity.WhisperMessageItem>? = null
        var nick: String? = null

        init {
            if (initialMessage != null) {
                pMessages = mutableListOf(initialMessage)
                nick = initialMessage.getNick()
            }


        }


        fun addMessage(pMes: ChatActivity.WhisperMessageItem) {
            if (pMessages == null) {
                pMessages = mutableListOf(pMes)
                return
            }
            pMessages!!.add(pMes)
        }

        fun removeMessage(pos: Int) {
            if (pMessages != null && pos < pMessages!!.size) {
                pMessages!!.removeAt(pos)
            }
            //TODO: remove from origin
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            if (pMessages == null || pMessages!!.size < 1) {
                //TODO: no messages
                return

            }
            viewHolder.itemView.usernameWhisperUser.text = nick
            var online = false
            if (CurrentUser.users != null) {
                CurrentUser.users!!.forEach { user ->
                    if (user.nick == nick) {
                        viewHolder.itemView.onlineWhisperUser.visibility = View.VISIBLE
                        online = true
                    }
                }
            } else if (CurrentUser.users == null || !online) {
                viewHolder.itemView.offlineWhisperUser.visibility = View.VISIBLE
            }
            viewHolder.itemView.setOnClickListener {

                //TODO: open fragment with whispers // username
            }
            //return
        }
    }
}

