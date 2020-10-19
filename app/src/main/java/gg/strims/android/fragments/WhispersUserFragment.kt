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
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Message
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_user_whispers.*
import kotlinx.android.synthetic.main.whisper_message_item_left.view.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.messageWhisperMessageItemLeft
import java.text.SimpleDateFormat

@SuppressLint("SetTextI18n", "SimpleDateFormat")
@KtorExperimentalAPI
class WhispersUserFragment : Fragment() {

    private val whispersUserAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_whispers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerViewWhispersUser.layoutManager = LinearLayoutManager(view.context)
        recyclerViewWhispersUser.adapter = whispersUserAdapter

        requireActivity().toolbar.title = CurrentUser.tempWhisperUser

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

        recyclerViewWhispersUser.addItemDecoration(
            MarginItemDecoration(
                (TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5f,
                    resources.displayMetrics
                )).toInt()
            )
        )

        sendMessageTextWhisper.hint = "Write something ${CurrentUser.user?.username}"

        sendMessageButtonWhisper.setOnClickListener {
            val intent = Intent("gg.strims.android.SEND_MESSAGE")
            intent.putExtra("gg.strims.android.SEND_MESSAGE_TEXT", "PRIVMSG {\"nick\":\"${CurrentUser.tempWhisperUser}\", \"data\":\"${sendMessageTextWhisper.text}\"}")
            requireActivity().sendBroadcast(intent)
            sendMessageTextWhisper.text.clear()
        }

        fetchPrivateMessages()
    }

    private fun fetchPrivateMessages() {
        CurrentUser.whispersDictionary[CurrentUser.tempWhisperUser]?.forEach {
            whispersUserAdapter.add(WhisperMessageItem(it))
        }
    }

    inner class WhisperMessageItem(val message: Message) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int {
            if (message.nick != CurrentUser.user?.username) {
                return R.layout.whisper_message_item_left
            }
            return R.layout.whisper_message_item_right
        }

        @SuppressLint("SimpleDateFormat")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val parentActivity = requireActivity() as ChatActivity
            parentActivity.createMessageTextView(message, viewHolder.itemView.messageWhisperMessageItemLeft)

            if (CurrentUser.options!!.showTime) {
                val dateFormat = SimpleDateFormat("HH:mm")
                val time = dateFormat.format(message.timestamp)
            }

            if (layout == R.layout.whisper_message_item_left) {
                viewHolder.itemView.usernameWhisperMessageItemLeft.text = message.nick

                if (CurrentUser.options!!.showTime) {
                    val dateFormat = SimpleDateFormat("HH:mm")
                    val time = dateFormat.format(message.timestamp)
                    viewHolder.itemView.timestampWhisperMessageItemLeft.text = time
                }
            } else if (layout == R.layout.whisper_message_item_right) {
                if (CurrentUser.options!!.showTime) {
                    val dateFormat = SimpleDateFormat("HH:mm")
                    val time = dateFormat.format(message.timestamp)
                    viewHolder.itemView.timestampWhisperMessageItemRight.text = time
                }
            }
        }
    }


//    override fun onHiddenChanged(hidden: Boolean) {
//        if (hidden) {
//            return
//        }
//        if (CurrentUser.privateMessages != null && CurrentUser.tempWhisperUser != null) {
//            whispersUserAdapter.clear()
//            whispersUserAdapter.notifyDataSetChanged()
//            CurrentUser.privateMessages!!.forEach {
//                if (it.getNick() == CurrentUser.tempWhisperUser!!) {
//                    whispersUserAdapter.add(it)
//                }
//            }
//
//            // showWhispers()
//        }
//        recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
//        whispersUserAdapter.notifyDataSetChanged()
//
//    }
}

