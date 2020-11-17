package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.createMessageTextView
import gg.strims.android.customspans.MarginItemDecoration
import gg.strims.android.room.PrivateMessage
import gg.strims.android.viewmodels.PrivateMessagesViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_user_whispers.*
import kotlinx.android.synthetic.main.whisper_message_item_left.view.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.*
import kotlinx.android.synthetic.main.whisper_message_item_right.view.messageWhisperMessageItemLeft
import java.text.SimpleDateFormat

@SuppressLint("SetTextI18n", "SimpleDateFormat")
@KtorExperimentalAPI
class WhispersUserFragment : Fragment() {

    private val whispersUserAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel

    private val conversation = mutableListOf<Int>()

    private var open = true

    private val args: WhispersUserFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_whispers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewWhispersUser.layoutManager = layoutManager
        recyclerViewWhispersUser.adapter = whispersUserAdapter

        recyclerViewWhispersUser.addItemDecoration(
            MarginItemDecoration(
                (TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5f,
                    resources.displayMetrics
                )).toInt()
            )
        )

        recyclerViewWhispersUser.itemAnimator = null

        recyclerViewWhispersUser.setOnScrollChangeListener { _, _, _, _, _ ->
            val layoutTest = recyclerViewWhispersUser.layoutManager as LinearLayoutManager
            val lastItem = layoutTest.findLastVisibleItemPosition()
            if (lastItem < recyclerViewWhispersUser.adapter!!.itemCount - 1) {
                goToBottomLayout.visibility = View.VISIBLE
                goToBottom.isEnabled = true
            } else {
                goToBottomLayout.visibility = View.GONE
                goToBottom.isEnabled = false
            }
        }

        goToBottom.setOnClickListener {
            recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
        }

        sendMessageTextWhisper.hint = "Write something ${CurrentUser.user?.username} ..."

        sendMessageButtonWhisper.setOnClickListener {
            val intent = Intent("gg.strims.android.SEND_MESSAGE")
            intent.putExtra(
                "gg.strims.android.SEND_MESSAGE_TEXT",
                "PRIVMSG {\"nick\":\"${args.username}\", \"data\":\"${sendMessageTextWhisper.text}\"}"
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

        privateMessagesViewModel = ViewModelProvider(requireActivity()).get(PrivateMessagesViewModel::class.java)
        privateMessagesViewModel.privateMessages.observe(viewLifecycleOwner, { messages ->
            val newMessages = messages.filter {
                (it.nick == CurrentUser.user!!.username && it.targetNick == args.username) || it.nick == args.username
            }
                .reversed()

            newMessages.forEach {
                if (!conversation.contains(it.id)) {
                    conversation.add(it.id)
                    whispersUserAdapter.add(WhisperMessageItem(it))
                    val layoutTest =
                        recyclerViewWhispersUser.layoutManager as LinearLayoutManager
                    val lastItem = layoutTest.findLastVisibleItemPosition()
                    if (lastItem >= whispersUserAdapter.itemCount - 3) {
                        recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
                    }
                }
            }

            if (open) {
                recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
                open = false
            }
        })

        recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
    }

    inner class WhisperMessageItem(val message: PrivateMessage) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int {
            if (message.nick != CurrentUser.user?.username) {
                return R.layout.whisper_message_item_left
            }
            return R.layout.whisper_message_item_right
        }

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            createMessageTextView(
                context!!,
                message.toMessage(),
                viewHolder.itemView.messageWhisperMessageItemLeft
            )

            if (layout == R.layout.whisper_message_item_left) {
                viewHolder.itemView.usernameWhisperMessageItemLeft.text = message.nick

                val dateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd")
                val time = dateFormat.format(message.timestamp)
                viewHolder.itemView.timestampWhisperMessageItemLeft.text = time
            } else if (layout == R.layout.whisper_message_item_right) {
                val dateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd")
                val time = dateFormat.format(message.timestamp)
                viewHolder.itemView.timestampWhisperMessageItemRight.text = time
            }
        }
    }
}

