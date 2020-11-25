package gg.strims.android.fragments

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
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.R
import gg.strims.android.databinding.ChatUserItemBinding
import gg.strims.android.databinding.FragmentUserListBinding
import gg.strims.android.keyRequestFocus
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.ChatViewModel
import io.ktor.util.*
import java.util.*

@KtorExperimentalAPI
class UserListFragment : Fragment() {

    private val binding by viewBinding(FragmentUserListBinding::bind)

    private var userListAdapter: GroupAdapter<GroupieViewHolder>? = null
    
    private lateinit var chatViewModel: ChatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentUserListBinding.inflate(layoutInflater).root

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            userListAdapter = null
        } else {
            userListAdapter = GroupAdapter<GroupieViewHolder>()
            chatViewModel.users.sortBy { it }
            chatViewModel.users.forEach {
                if (it.isNotEmpty() && userListAdapter != null) {
                    userListAdapter?.add(UserListItem(it))
                }
            }
            binding.recyclerViewUserList.adapter = userListAdapter
            binding.recyclerViewUserList.scrollToPosition(1)
        }
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

        binding.closeUserListButton.setOnClickListener {
            requireParentFragment().childFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                .hide(this)
                .commit()
        }

        binding.userListSearch.addTextChangedListener(object : TextWatcher {
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
                userListAdapter?.clear()
                chatViewModel.users.forEach {
                    if (binding.userListSearch.text.isNotEmpty()) {
                        if (it.toLowerCase(Locale.getDefault()).contains(
                                binding.userListSearch.text.toString().toLowerCase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            userListAdapter?.add(UserListItem(it))
                        }
                    } else {
                        userListAdapter?.add(UserListItem(it))
                    }
                }
            }
        })
    }

    inner class UserListItem(val user: String) : BindableItem<ChatUserItemBinding>() {

        override fun getLayout(): Int = R.layout.chat_user_item

        override fun bind(viewBinding: ChatUserItemBinding, position: Int) {
            with (viewBinding) {
                chatUserUsername.text = user

                chatUserUsername.setOnClickListener {
                    val parentFragment = requireParentFragment() as ChatFragment
                    parentFragment.binding.sendMessageText.setText(
                        resources.getString(
                            R.string.chat_whisper_popup,
                            user
                        )
                    )
                    keyRequestFocus(
                        parentFragment.binding.sendMessageText,
                        context!!
                    )
                    requireParentFragment().childFragmentManager.beginTransaction()
                        .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                        .hide(this@UserListFragment)
                        .commit()
                    parentFragment.binding.sendMessageText.setSelection(parentFragment.binding.sendMessageText.text.length)
                }
            }
        }

        override fun initializeViewBinding(view: View): ChatUserItemBinding {
            return ChatUserItemBinding.bind(view)
        }
    }
}