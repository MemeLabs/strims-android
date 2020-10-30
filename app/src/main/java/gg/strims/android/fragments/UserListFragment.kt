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
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_user_list.*
import kotlinx.android.synthetic.main.chat_user_item.view.*
import java.util.*

@KtorExperimentalAPI
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
        CurrentUser.users.sortBy { it }
        CurrentUser.users.forEach {
            if (it.isNotEmpty()) {
                userListAdapter.add(UserListItem(it))
            }
        }
        recyclerViewUserList.scrollToPosition(1)

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
                CurrentUser.users.forEach {
                    if (userListSearch.text.isNotEmpty()) {
                        if (it.toLowerCase(Locale.ROOT).contains(
                                userListSearch.text.toString().toLowerCase(
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
        hideFragment(requireActivity(), this)
        val layoutManager =
            LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewUserList.layoutManager = layoutManager
        recyclerViewUserList.adapter = userListAdapter

        closeUserListButton.setOnClickListener {
            hideFragment(requireActivity(), this@UserListFragment)
        }
    }

    inner class UserListItem(val user: String) : Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.chat_user_item

        @SuppressLint("SetTextI18n")
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.chatUserUsername.text = user
            viewHolder.itemView.chatUserUsername.setTextColor(
                Color.parseColor(
                    "#FFFFFF"
                )
            )

            viewHolder.itemView.chatUserUsername.setOnClickListener {
                activity!!.sendMessageText.setText("/w $user ")
                keyRequestFocus(
                    activity!!.sendMessageText,
                    context!!
                )
                activity!!.sendMessageText.setSelection(activity!!.sendMessageText.text.length)
                val fragment = this@UserListFragment
                val fragmentTransaction = parentFragmentManager.beginTransaction()
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