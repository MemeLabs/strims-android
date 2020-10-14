package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.os.Message
import android.util.TypedValue
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
import kotlinx.android.synthetic.main.whisper_message_item_right.*
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
        hideFragment(requireActivity(), this)
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewWhispers.layoutManager = layoutManager
        recyclerViewWhispers.adapter = whispersAdapter
        class MarginItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect, view: View,
                parent: RecyclerView, state: RecyclerView.State
            ) {
                with(outRect) {
                    if (parent.getChildAdapterPosition(view) == 0) {
                        top = spaceHeight
                    }
                    left = spaceHeight
                    right = spaceHeight
                    bottom = spaceHeight
                }
            }
        }
        recyclerViewWhispers.addItemDecoration(
            MarginItemDecoration(
                (TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5f,
                    resources.displayMetrics
                )).toInt()
            )
        )

    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.privateMessages != null) {
            whispersAdapter.clear()
            CurrentUser.privateMessages!!.forEach {
                var b = true
                for (i in 0 until whispersAdapter.itemCount) {
                    val item = whispersAdapter.getItem(i) as WhisperUserItem
                    if (item.nick == it.getNick()) {
                        item.replaceLatestMessage(it)
                        b = false
                        break
                    }
                }
                if (b) {

                    whispersAdapter.add(WhisperUserItem(it))

                }
            }
        }
        recyclerViewWhispers.scrollToPosition(whispersAdapter.itemCount - 1)
        whispersAdapter.notifyDataSetChanged()
    }

    inner class WhisperUserItem(initialMessage: ChatActivity.WhisperMessageItem? = null) :
        Item<GroupieViewHolder>(), Serializable {
        override fun getLayout(): Int {
            return R.layout.whisper_user_item
        }

        private var latestMessage: ChatActivity.WhisperMessageItem? = null
        var nick: String? = null

        init {
            if (initialMessage != null) {
                latestMessage = initialMessage
                nick = initialMessage.getNick()
            }


        }

        fun replaceLatestMessage(newMessage: ChatActivity.WhisperMessageItem? = null) {
            latestMessage = newMessage
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            viewHolder.itemView.usernameWhisperUser.text = nick

            if (CurrentUser.users != null) {
                var online = false
                CurrentUser.users!!.forEach { user ->

                    if (user.nick == nick) {
                        viewHolder.itemView.onlineWhisperUser.visibility = View.VISIBLE
                        viewHolder.itemView.offlineWhisperUser.visibility = View.GONE
                        online = true
                    }
                }
                if (!online) {
                    viewHolder.itemView.offlineWhisperUser.visibility = View.VISIBLE
                    viewHolder.itemView.onlineWhisperUser.visibility = View.GONE
                }
            } else {
                viewHolder.itemView.offlineWhisperUser.visibility = View.VISIBLE
                viewHolder.itemView.onlineWhisperUser.visibility = View.GONE
            }
            viewHolder.itemView.setOnClickListener {
                if (activity != null) {
                    val item = whispersAdapter.getItem(position) as WhisperUserItem
                    CurrentUser.tempWhisperUser = item.nick
                    showHideFragment(
                        activity!!,
                        activity!!.supportFragmentManager.findFragmentById(R.id.whispers_fragment)!!
                    )
                    showHideFragment(
                        activity!!,
                        activity!!.supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!

                    )

                }
            }
            //return
        }
    }
}

