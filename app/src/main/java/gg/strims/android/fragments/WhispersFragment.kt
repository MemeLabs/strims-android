package gg.strims.android.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.R
import gg.strims.android.createMessageTextView
import gg.strims.android.customspans.MarginItemDecoration
import gg.strims.android.databinding.FragmentWhispersBinding
import gg.strims.android.databinding.WhisperUserItemBinding
import gg.strims.android.room.PrivateMessage
import gg.strims.android.singletons.CurrentUser
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.ChatViewModel
import gg.strims.android.viewmodels.PrivateMessagesViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*

@KtorExperimentalAPI
class WhispersFragment : Fragment() {

    private val binding by viewBinding(FragmentWhispersBinding::bind)

    private var whispersAdapter: GroupAdapter<GroupieViewHolder>? = GroupAdapter<GroupieViewHolder>()

    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel
    private lateinit var chatViewModel: ChatViewModel

    private var newMap: HashMap<String, PrivateMessage> = hashMapOf()

    override fun onDetach() {
        whispersAdapter = null
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentWhispersBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        whispersAdapter = GroupAdapter<GroupieViewHolder>()
        with (binding) {
            recyclerViewWhispers.layoutManager = LinearLayoutManager(view.context)
            recyclerViewWhispers.adapter = whispersAdapter
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

        chatViewModel = ViewModelProvider(requireActivity()).get(ChatViewModel::class.java)
        privateMessagesViewModel = ViewModelProvider(requireActivity()).get(PrivateMessagesViewModel::class.java)
        privateMessagesViewModel.privateMessages.observe(viewLifecycleOwner, { messages ->
            whispersAdapter!!.clear()
            messages.forEach { message ->
                val otherUser = if (CurrentUser.user!!.username == message.nick) message.targetNick else message.nick
                if (!newMap.containsKey(otherUser)) {
                    newMap[otherUser] = message
                } else if (newMap[otherUser] != null) {
                    if (message.timestamp > newMap[otherUser]!!.timestamp) {
                        newMap[otherUser] = message
                    }
                }
            }
            newMap.forEach {
                whispersAdapter!!.add(WhisperUserItem(it.value))
            }
        })
    }

    inner class WhisperUserItem(var message: PrivateMessage) : BindableItem<WhisperUserItemBinding>() {

        override fun getLayout(): Int = R.layout.whisper_user_item

        override fun bind(viewBinding: WhisperUserItemBinding, position: Int) {
            with (viewBinding) {
                val otherUser =
                    if (CurrentUser.user!!.username == message.nick) message.targetNick else message.nick
                usernameWhisperUser.text = otherUser

                var online = false
                chatViewModel.users.forEach { user ->
                    if (user == otherUser) {
                        onlineWhisperUser.visibility = View.VISIBLE
                        online = true
                        return@forEach
                    }
                }
                if (!online) {
                    onlineWhisperUser.visibility = View.GONE
                }
                createMessageTextView(
                    context!!,
                    message.toMessage(),
                    latestMessageWhisperUser
                )

                root.setOnClickListener {
                    val action =
                        WhispersFragmentDirections.actionNavWhispersToWhispersUserFragment(otherUser)
                    findNavController().navigate(action)
                }
            }
        }

        override fun initializeViewBinding(view: View): WhisperUserItemBinding {
            return WhisperUserItemBinding.bind(view)
        }
    }
}

