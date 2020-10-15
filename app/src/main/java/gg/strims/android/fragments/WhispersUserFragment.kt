package gg.strims.android.fragments

import android.annotation.SuppressLint
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
import gg.strims.android.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_user_whispers.*
import kotlinx.android.synthetic.main.fragment_user_whispers.view.*

@SuppressLint("SetTextI18n")
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

    fun addToAdapter(newMessage: ChatActivity.WhisperMessageItem) {
        whispersUserAdapter.add(newMessage)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }
        view.backWhispersUser.setOnClickListener {
            hideKeyboardFrom(requireContext(), requireActivity().sendMessageText)
            showHideFragment(
                requireActivity(),
                requireActivity().supportFragmentManager.findFragmentById(R.id.whispers_user_fragment)!!

            )
            showHideFragment(
                requireActivity(),
                requireActivity().supportFragmentManager.findFragmentById(R.id.whispers_fragment)!!
            )

        }
        val layoutManager = LinearLayoutManager(view.context)
        layoutManager.stackFromEnd = true
        recyclerViewWhispersUser.layoutManager = layoutManager
        recyclerViewWhispersUser.adapter = whispersUserAdapter

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
    }


    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            return
        }
        if (CurrentUser.privateMessages != null && CurrentUser.tempWhisperUser != null) {
            whispersUserAdapter.clear()
            whispersUserAdapter.notifyDataSetChanged()
            CurrentUser.privateMessages!!.forEach {
                if (it.getNick() == CurrentUser.tempWhisperUser!!) {
                    whispersUserAdapter.add(it)
                }

            }

            // showWhispers()
        }
        recyclerViewWhispersUser.scrollToPosition(whispersUserAdapter.itemCount - 1)
        whispersUserAdapter.notifyDataSetChanged()
        usernameWhispersUser.text = CurrentUser.tempWhisperUser!!

    }


}

