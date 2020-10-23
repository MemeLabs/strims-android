package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == "gg.strims.android.PRIVATE_MESSAGE") {
                    // Add new message
                    whispersUserAdapter.add(WhisperMessageItem(CurrentUser.whispersDictionary[CurrentUser.tempWhisperUser]!!.last()))
                    recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
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

        sendMessageTextWhisper.hint = "Write something ${CurrentUser.user?.username} ..."

        sendMessageButtonWhisper.setOnClickListener {
            val intent = Intent("gg.strims.android.SEND_MESSAGE")
            intent.putExtra(
                "gg.strims.android.SEND_MESSAGE_TEXT",
                "PRIVMSG {\"nick\":\"${CurrentUser.tempWhisperUser}\", \"data\":\"${sendMessageTextWhisper.text}\"}"
            )
            requireActivity().sendBroadcast(intent)
            sendMessageTextWhisper.text.clear()
        }

        sendMessageTextWhisper.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                sendMessageButtonWhisper.isEnabled = sendMessageTextWhisper.text.isNotEmpty()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
                sendMessageButtonWhisper.isEnabled = sendMessageTextWhisper.text.isNotEmpty()
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                sendMessageButtonWhisper.isEnabled = sendMessageTextWhisper.text.isNotEmpty()
            }
        })

        fetchPrivateMessages()

        recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
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

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            val parentActivity = requireActivity() as ChatActivity
            parentActivity.createMessageTextView(
                message,
                viewHolder.itemView.messageWhisperMessageItemLeft
            )

            if (layout == R.layout.whisper_message_item_left) {
                viewHolder.itemView.usernameWhisperMessageItemLeft.text = message.nick

                val dateFormat = SimpleDateFormat("HH:mm yy-MM-dd")
                val time = dateFormat.format(message.timestamp)
                viewHolder.itemView.timestampWhisperMessageItemLeft.text = time
            } else if (layout == R.layout.whisper_message_item_right) {
                val dateFormat = SimpleDateFormat("HH:mm yy-MM-dd")
                val time = dateFormat.format(message.timestamp)
                viewHolder.itemView.timestampWhisperMessageItemRight.text = time
            }
        }
    }
}

