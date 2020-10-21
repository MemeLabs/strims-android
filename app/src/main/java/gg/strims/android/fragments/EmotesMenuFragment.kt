package gg.strims.android.fragments

import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import gg.strims.android.ChatActivity
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.emote_menu_item.view.*
import kotlinx.android.synthetic.main.fragment_emote_menu.*
import java.util.*

@KtorExperimentalAPI
class EmotesMenuFragment : Fragment() {

    private val emoteMenuAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_emote_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)
        recyclerViewEmoteMenu.layoutManager = GridLayoutManager(view.context, 5)
        recyclerViewEmoteMenu.adapter = emoteMenuAdapter

        emoteMenuSearch.addTextChangedListener(object :
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
                emoteMenuAdapter.clear()
                CurrentUser.bitmapMemoryCache.forEach {
                    if (emoteMenuSearch.text.isNotEmpty()) {
                        if (it.key.toLowerCase(Locale.ROOT).contains(
                                emoteMenuSearch.text.toString().toLowerCase(
                                    Locale.ROOT
                                )
                            )
                        ) {
                            emoteMenuAdapter.add(EmoteMenuItem(it.key, it.value))
                        }
                    } else {
                        emoteMenuAdapter.add(EmoteMenuItem(it.key, it.value))
                    }
                }
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        emoteMenuAdapter.clear()
        if (CurrentUser.bitmapMemoryCache.isNotEmpty()) {
            CurrentUser.bitmapMemoryCache.forEach {
                emoteMenuAdapter.add(EmoteMenuItem(it.key, it.value))
            }
        }
    }

    inner class EmoteMenuItem(val name: String, private val bitmap: Bitmap) : Item<GroupieViewHolder>() {
        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            viewHolder.itemView.imageViewEmote.setImageBitmap(bitmap)

            viewHolder.itemView.imageViewEmote.setOnClickListener {
                val activity = requireActivity() as ChatActivity
                activity.sendMessageText.append("$name ")
                hideFragment(requireActivity(), this@EmotesMenuFragment)
            }
        }

        override fun getLayout(): Int = R.layout.emote_menu_item
    }

}