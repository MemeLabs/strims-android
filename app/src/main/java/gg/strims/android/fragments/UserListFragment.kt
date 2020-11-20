package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.R
import gg.strims.android.databinding.FragmentUserListBinding
import gg.strims.android.keyRequestFocus
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.ChatViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.chat_user_item.view.*
import kotlinx.android.synthetic.main.fragment_chat.*
import java.util.*

@KtorExperimentalAPI
@SuppressLint("SetTextI18n")
class UserListFragment : Fragment() {

    private val binding by viewBinding(FragmentUserListBinding::bind)

    private val userListAdapter = GroupAdapter<GroupieViewHolder>()
    
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentUserListBinding.inflate(layoutInflater).root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        userListAdapter.clear()
        chatViewModel.users.sortBy { it }
        chatViewModel.users.forEach {
            if (it.isNotEmpty()) {
                userListAdapter.add(UserListItem(it))
            }
        }
        binding.recyclerViewUserList.scrollToPosition(1)

        binding.userListSearch.addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                userListAdapter.clear()
                chatViewModel.users.forEach {
                    if (binding.userListSearch.text.isNotEmpty()) {
                        if (it.toLowerCase(Locale.ROOT).contains(
                                binding.userListSearch.text.toString().toLowerCase(
                                    Locale.ROOT
                                )
                            )
                        ) {
                            userListAdapter.add(UserListItem(it))
                        }
                    } else {
                        userListAdapter.add(UserListItem(it))
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chatViewModel = ViewModelProvider(requireActivity()).get(ChatViewModel::class.java)

        requireParentFragment().childFragmentManager.beginTransaction()
            .hide(this)
            .commit()
        val layoutManager =
            LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        binding.recyclerViewUserList.layoutManager = layoutManager
        binding.recyclerViewUserList.adapter = userListAdapter

        binding.closeUserListButton.setOnClickListener {
            requireParentFragment().childFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                .hide(this)
                .commit()
        }
    }

    inner class UserListItem(val user: String) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.chat_user_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.chatUserUsername.text = user

            viewHolder.itemView.chatUserUsername.setOnClickListener {
                parentFragment!!.sendMessageText.setText("/w $user ")
                keyRequestFocus(
                    parentFragment!!.sendMessageText,
                    context!!
                )
                requireParentFragment().childFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                    .hide(this@UserListFragment)
                    .commit()
                parentFragment!!.sendMessageText.setSelection(parentFragment!!.sendMessageText.text.length)
            }
        }
    }
}