package gg.strims.android.fragments

import android.content.res.Configuration
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
import gg.strims.android.*
import gg.strims.android.databinding.FragmentEmoteMenuBinding
import io.ktor.util.*
import kotlinx.android.synthetic.main.emote_menu_item.view.*
import pl.droidsonroids.gif.GifDrawable
import java.util.*

@KtorExperimentalAPI
class EmotesMenuFragment : Fragment() {

    private val binding by viewBinding(FragmentEmoteMenuBinding::bind)

    private val emoteMenuAdapter = GroupAdapter<GroupieViewHolder>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentEmoteMenuBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireParentFragment().childFragmentManager.beginTransaction()
            .hide(this)
            .commit()

        binding.recyclerViewEmoteMenu.layoutManager = GridLayoutManager(
            view.context,
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 8 else 5
        )
        binding.recyclerViewEmoteMenu.adapter = emoteMenuAdapter

        binding.closeEmoteMenuButton.setOnClickListener {
            requireParentFragment().childFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                .hide(this)
                .commit()
        }

        binding.emoteMenuSearch.addTextChangedListener(object :
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
                    if (binding.emoteMenuSearch.text.isNotEmpty()) {
                        if (it.key.toLowerCase(Locale.ROOT).contains(
                                binding.emoteMenuSearch.text.toString().toLowerCase(
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

                CurrentUser.gifMemoryCache.forEach {
                    if (binding.emoteMenuSearch.text.isNotEmpty()) {
                        if (it.key.toLowerCase(Locale.ROOT).contains(
                                binding.emoteMenuSearch.text.toString().toLowerCase(
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

            CurrentUser.gifMemoryCache.forEach {
                emoteMenuAdapter.add(EmoteMenuItem(it.key, it.value))
            }
        }
    }

    inner class EmoteMenuItem<T>(val name: String, private val emote: T) :
        Item<GroupieViewHolder>() {

        override fun getLayout(): Int = R.layout.emote_menu_item

        override fun bind(viewHolder: GroupieViewHolder, position: Int) {
            if (emote is Bitmap) {
                viewHolder.itemView.imageViewEmote.setImageBitmap(emote as Bitmap)
            } else {
                viewHolder.itemView.imageViewEmote.setImageDrawable(emote as GifDrawable)
            }

            viewHolder.itemView.imageViewEmote.setOnClickListener {
                val parentFragment = requireParentFragment() as ChatFragment
                parentFragment.binding.sendMessageText.append("$name ")
                keyRequestFocus(
                    parentFragment.binding.sendMessageText,
                    context!!
                )
                requireParentFragment().childFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.fragment_open_enter, R.anim.fragment_open_exit)
                    .hide(this@EmotesMenuFragment)
                    .commit()
                parentFragment.binding.sendMessageText.setSelection(parentFragment.binding.sendMessageText.text.length)
            }
        }
    }

}