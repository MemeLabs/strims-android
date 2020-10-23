package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_navigation_drawer.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_whispers.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class WhispersFragment : Fragment() {

    private val whispersAdapter = GroupAdapter<GroupieViewHolder>()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "gg.strims.android.PRIVATE_MESSAGE") {
                    whispersAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val intentFilter = IntentFilter("gg.strims.android.PRIVATE_MESSAGE")
        requireActivity().registerReceiver(broadcastReceiver, intentFilter)
        return inflater.inflate(R.layout.fragment_whispers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerViewWhispers.layoutManager = LinearLayoutManager(view.context)
        recyclerViewWhispers.adapter = whispersAdapter

        requireActivity().toolbar.title = "Private Messages"

        requireActivity().nav_view.setCheckedItem(R.id.nav_Whispers)

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

        displayPrivateMessages()
    }

    private fun displayPrivateMessages() {
        whispersAdapter.clear()
        if (CurrentUser.privateMessageUsers != null) {
            CurrentUser.privateMessageUsers!!.forEach {
                whispersAdapter.add(WhisperUserItem(it))
            }
        }
        whispersAdapter.notifyDataSetChanged()
    }

    inner class WhisperUserItem(var nick: String) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int {
            return R.layout.whisper_user_item
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            viewHolder.itemView.usernameWhisperUser.text = nick

            if (CurrentUser.users != null) {
                var online = false
                CurrentUser.users!!.forEach { user ->
                    if (user.nick == nick) {
                        viewHolder.itemView.onlineWhisperUser.visibility = View.VISIBLE
                        online = true
                        return@forEach
                    }
                }
                if (!online) {
                    viewHolder.itemView.onlineWhisperUser.visibility = View.GONE
                }
            } else {
                viewHolder.itemView.onlineWhisperUser.visibility = View.GONE
            }
            val parentActivity = requireActivity() as ChatActivity
            parentActivity.createMessageTextView(CurrentUser.whispersMap[nick]!!.last(), viewHolder.itemView.latestMessageWhisperUser)

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempWhisperUser = nick
                parentFragmentManager.beginTransaction()
                    .add(R.id.nav_host_fragment, WhispersUserFragment(), "WhispersUserFragment")
                    .addToBackStack("WhispersUserFragment").commit()
            }
        }
    }
}

