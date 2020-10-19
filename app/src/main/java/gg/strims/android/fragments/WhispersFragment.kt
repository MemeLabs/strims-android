package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Message
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_user_whispers.*
import kotlinx.android.synthetic.main.fragment_whispers.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*
import java.io.BufferedReader
import java.io.InputStreamReader

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
        recyclerViewWhispers.layoutManager = LinearLayoutManager(view.context)
        recyclerViewWhispers.adapter = whispersAdapter

        requireActivity().toolbar.title = "Private Messages"

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
                val nick = it.substringAfter("private_messages_").substringBefore(".txt")
                val file =
                    requireActivity().baseContext.getFileStreamPath(it)
                if (file.exists()) {
                    val fileInputStream =
                        requireActivity().openFileInput(it)
                    val inputStreamReader = InputStreamReader(fileInputStream)
                    val bufferedReader = BufferedReader(inputStreamReader)
                    val messagesArray = mutableListOf<Message>()
                    while (bufferedReader.ready()) {
                        val line = bufferedReader.readLine()
                        val curPMessage: Message = Gson().fromJson(line, Message::class.java)
                        messagesArray.add(curPMessage)
                    }
                    CurrentUser.whispersDictionary[nick] = messagesArray
                    CurrentUser.whispersDictionary[nick]?.sortBy { message ->
                        message.timestamp
                    }
                }
                whispersAdapter.add(WhisperUserItem(nick))
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
            parentActivity.createMessageTextView(CurrentUser.whispersDictionary[nick]!!.last(), viewHolder.itemView.latestMessageWhisperUser)

            viewHolder.itemView.setOnClickListener {
                CurrentUser.tempWhisperUser = nick
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, WhispersUserFragment(), "WhispersUserFragment")
                    .addToBackStack("WhispersUserFragment").commit()
            }
        }
    }
}

