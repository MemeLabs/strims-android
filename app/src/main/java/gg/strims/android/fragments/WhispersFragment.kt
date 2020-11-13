package gg.strims.android.fragments

import android.annotation.SuppressLint
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
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.createMessageTextView
import gg.strims.android.customspans.MarginItemDecoration
import gg.strims.android.room.PrivateMessage
import gg.strims.android.viewmodels.PrivateMessagesViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_whispers.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class WhispersFragment : Fragment() {

    private var whispersAdapter: GroupAdapter<GroupieViewHolder>? = GroupAdapter<GroupieViewHolder>()

    private lateinit var privateMessagesViewModel: PrivateMessagesViewModel

    private var newMap: HashMap<String, PrivateMessage> = hashMapOf()

    override fun onDetach() {
        whispersAdapter = null
        super.onDetach()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_whispers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        whispersAdapter = GroupAdapter<GroupieViewHolder>()
        recyclerViewWhispers.layoutManager = LinearLayoutManager(view.context)
        recyclerViewWhispers.adapter = whispersAdapter

//        requireActivity().toolbar.title = "Private Messages"
//
//        requireActivity().nav_view.setCheckedItem(R.id.nav_Whispers)

        recyclerViewWhispers.addItemDecoration(
            MarginItemDecoration(
                (TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    5f,
                    resources.displayMetrics
                )).toInt()
            )
        )

        privateMessagesViewModel = ViewModelProvider(this).get(PrivateMessagesViewModel::class.java)
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

    inner class WhisperUserItem(var message: PrivateMessage) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.whisper_user_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {

            val otherUser = if (CurrentUser.user!!.username == message.nick) message.targetNick else message.nick
            viewHolder.itemView.usernameWhisperUser.text = otherUser

            var online = false
            CurrentUser.users.forEach { user ->
                if (user == otherUser) {
                    viewHolder.itemView.onlineWhisperUser.visibility = View.VISIBLE
                    online = true
                    return@forEach
                }
            }
            if (!online) {
                viewHolder.itemView.onlineWhisperUser.visibility = View.GONE
            }
            createMessageTextView(context!!, message.toMessage(), viewHolder.itemView.latestMessageWhisperUser)

            viewHolder.itemView.setOnClickListener {
//                parentFragmentManager.beginTransaction()
//                    .replace(R.id.nav_host_fragment, WhispersUserFragment(), "WhispersUserFragment")
//                    .addToBackStack("WhispersUserFragment").commit()
                val action = WhispersFragmentDirections.actionNavWhispersToWhispersUserFragment(otherUser)
                findNavController().navigate(action)
            }
        }
    }
}

