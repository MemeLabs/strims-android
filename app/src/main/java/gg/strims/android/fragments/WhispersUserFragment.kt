package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.*
import gg.strims.android.models.Stream
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.fragment_whispers.*
import kotlinx.android.synthetic.main.fragment_whispers.view.*
import kotlinx.android.synthetic.main.private_chat_message_item.view.*
import kotlinx.android.synthetic.main.whisper_item.*
import kotlinx.android.synthetic.main.whisper_user_item.view.*
import java.io.Serializable

@SuppressLint("SetTextI18n")
@KtorExperimentalAPI
class WhispersUserFragment : Fragment() {

    private val whispersAdapter = GroupAdapter<GroupieViewHolder>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_whispers, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        view.setOnTouchListener { _, _ -> return@setOnTouchListener true }


    }


    override fun onHiddenChanged(hidden: Boolean) {

    }


}

