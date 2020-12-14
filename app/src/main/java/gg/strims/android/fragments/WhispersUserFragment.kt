package gg.strims.android.fragments

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
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.R
import gg.strims.android.utils.createMessageTextView
import gg.strims.android.customspans.MarginItemDecoration
import gg.strims.android.databinding.FragmentUserWhispersBinding
import gg.strims.android.databinding.WhisperMessageItemLeftBinding
import gg.strims.android.databinding.WhisperMessageItemRightBinding
import gg.strims.android.room.PrivateMessage
import gg.strims.android.utils.viewBinding
import gg.strims.android.viewmodels.PrivateMessagesViewModel
import io.ktor.util.*
import java.text.SimpleDateFormat
import java.util.*

@KtorExperimentalAPI
class WhispersUserFragment : Fragment() {

    private val binding by viewBinding(FragmentUserWhispersBinding::bind)

    private val whispersUserAdapter = GroupAdapter<GroupieViewHolder>()

    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel

    private val conversation = mutableListOf<Int>()
    private var open = true

    private val args: WhispersUserFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentUserWhispersBinding.inflate(layoutInflater).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        with (binding) {
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
                val layoutTest =
                    recyclerViewWhispersUser.layoutManager as LinearLayoutManager
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

            sendMessageTextWhisper.hint =
                "Write something ${CurrentUser.user?.username} ..."

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
                    sendMessageButtonWhisper.isEnabled =
                        sendMessageTextWhisper.text.isNotEmpty()
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    sendMessageButtonWhisper.isEnabled =
                        sendMessageTextWhisper.text.isNotEmpty()
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    sendMessageButtonWhisper.isEnabled =
                        sendMessageTextWhisper.text.isNotEmpty()
                }
            })

            privateMessagesViewModel =
                ViewModelProvider(requireActivity()).get(PrivateMessagesViewModel::class.java)
            privateMessagesViewModel.privateMessages.observe(viewLifecycleOwner, { messages ->
                val newMessages = messages.filter {
                    (it.nick == CurrentUser.user!!.username && it.targetNick == args.username) || it.nick == args.username
                }
                    .reversed()

                newMessages.forEach {
                    if (!conversation.contains(it.id)) {
                        conversation.add(it.id)
                        if (it.nick == CurrentUser.user?.username) {
                            whispersUserAdapter.add(WhisperMessageItemRight(it))
                        } else {
                            whispersUserAdapter.add(WhisperMessageItemLeft(it))
                        }
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
    }

    inner class WhisperMessageItemLeft(val message: PrivateMessage) : BindableItem<WhisperMessageItemLeftBinding>() {

        override fun getLayout(): Int = R.layout.whisper_message_item_left

        override fun bind(viewBinding: WhisperMessageItemLeftBinding, position: Int) {
            createMessageTextView(
                context!!,
                message.toMessage(),
                viewBinding.messageWhisperMessageItemLeft
            )

            val dateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.getDefault())
            val time = dateFormat.format(message.timestamp)
            viewBinding.timestampWhisperMessageItemLeft.text = time
        }

        override fun initializeViewBinding(view: View): WhisperMessageItemLeftBinding {
            return WhisperMessageItemLeftBinding.bind(view)
        }
    }

    inner class WhisperMessageItemRight(val message: PrivateMessage) : BindableItem<WhisperMessageItemRightBinding>() {

        override fun getLayout(): Int = R.layout.whisper_message_item_right

        override fun bind(viewBinding: WhisperMessageItemRightBinding, position: Int) {
            with (viewBinding) {
                createMessageTextView(
                    context!!,
                    message.toMessage(),
                    messageWhisperMessageItemRight
                )

                val dateFormat = SimpleDateFormat("HH:mm yyyy-MM-dd", Locale.getDefault())
                val time = dateFormat.format(message.timestamp)
                timestampWhisperMessageItemRight.text = time
            }
        }

        override fun initializeViewBinding(view: View): WhisperMessageItemRightBinding {
            return WhisperMessageItemRightBinding.bind(view)
        }
    }
}

