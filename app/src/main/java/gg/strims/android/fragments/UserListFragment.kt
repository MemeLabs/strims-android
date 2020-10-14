package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import gg.strims.android.keyRequestFocus
import gg.strims.android.models.ChatUser
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_user_list.*
import kotlinx.android.synthetic.main.chat_user_item.view.*

class UserListFragment : Fragment() {
    private val userListAdapter =
        GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_list, container, false)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        userListAdapter.clear()
        if (CurrentUser.users != null) {
            CurrentUser.users!!.sortBy { it.nick }
            CurrentUser.users!!.forEach {
                userListAdapter.add(UserListItem(it))
            }
            recyclerViewUserList.scrollToPosition(1)
        }

        userListSearch.addTextChangedListener(object :
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
                val list = mutableListOf<ChatUser>()
                CurrentUser.users!!.sortBy { it.nick }
                CurrentUser.users!!.forEach {
                    userListAdapter.add(UserListItem(it))
                }
                recyclerViewUserList.scrollToPosition(1)
                for (i in 0 until userListAdapter.itemCount) {
                    val item = userListAdapter.getItem(i) as UserListItem
                    if (item.user.nick.contains(userListSearch.text.toString())) {
                        list.add(item.user)
                    }
                }
                userListAdapter.clear()
                list.forEach {
                    for (i in 0 until userListAdapter.itemCount) {
                        val item = userListAdapter.getItem(i) as UserListItem
                        if (item.user.nick != it.nick) {
                            userListAdapter.add(UserListItem(it))
                        }
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)
        view.setOnTouchListener { view, motionEvent -> return@setOnTouchListener true }
        val layoutManager =
            LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewUserList.layoutManager = layoutManager
        recyclerViewUserList.adapter = userListAdapter

        closeUserListButton.setOnClickListener {
            requireFragmentManager().beginTransaction()
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .hide(this)
                .commit()
        }
    }

    inner class UserListItem(val user: ChatUser) : Item<GroupieViewHolder>() {
        override fun getLayout(): Int {
            return R.layout.chat_user_item
        }

        @SuppressLint("SetTextI18n")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.chatUserUsername.text = user.nick
            if (user.features.contains("bot")) {
                viewHolder.itemView.chatUserUsername.setTextColor(
                    Color.parseColor(
                        "#FF2196F3"
                    )
                )
            } else {
                viewHolder.itemView.chatUserUsername.setTextColor(
                    Color.parseColor(
                        "#FFFFFF"
                    )
                )
            }

            viewHolder.itemView.chatUserUsername.setOnClickListener {
                activity!!.sendMessageText.setText("/w ${user.nick} ")
                keyRequestFocus(
                    activity!!.sendMessageText,
                    context!!
                )
                activity!!.sendMessageText.setSelection(activity!!.sendMessageText.text.length)
                val fragment = this@UserListFragment
                val fragmentTransaction = fragmentManager!!.beginTransaction()
                fragmentTransaction.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                    .hide(fragment)

                fragmentTransaction.commit()
            }
        }
    }
}