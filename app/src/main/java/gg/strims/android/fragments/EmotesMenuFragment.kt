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
import com.xwray.groupie.viewbinding.BindableItem
import gg.strims.android.*
import gg.strims.android.databinding.EmoteMenuItemBinding
import gg.strims.android.databinding.FragmentEmoteMenuBinding
import gg.strims.android.singletons.CurrentUser
import io.ktor.util.*
import pl.droidsonroids.gif.GifDrawable
import java.util.*

@KtorExperimentalAPI
class EmotesMenuFragment : Fragment() {

    private val binding by viewBinding(FragmentEmoteMenuBinding::bind)

    private var emoteMenuAdapter: GroupAdapter<GroupieViewHolder>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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
                emoteMenuAdapter?.clear()
                CurrentUser.bitmapMemoryCache.forEach {
                    if (binding.emoteMenuSearch.text.isNotEmpty()) {
                        if (it.key.toLowerCase(Locale.getDefault()).contains(
                                binding.emoteMenuSearch.text.toString().toLowerCase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                        }
                    } else {
                        emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                    }
                }

                CurrentUser.gifMemoryCache.forEach {
                    if (binding.emoteMenuSearch.text.isNotEmpty()) {
                        if (it.key.toLowerCase(Locale.getDefault()).contains(
                                binding.emoteMenuSearch.text.toString().toLowerCase(
                                    Locale.getDefault()
                                )
                            )
                        ) {
                            emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                        }
                    } else {
                        emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                    }
                }
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            emoteMenuAdapter = null
        } else {
            emoteMenuAdapter = GroupAdapter<GroupieViewHolder>()
            if (CurrentUser.bitmapMemoryCache.isNotEmpty() && emoteMenuAdapter != null) {
                CurrentUser.bitmapMemoryCache.forEach {
                    emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                }

                CurrentUser.gifMemoryCache.forEach {
                    emoteMenuAdapter?.add(EmoteMenuItem(it.key, it.value))
                }
            }
            binding.recyclerViewEmoteMenu.adapter = emoteMenuAdapter
        }
    }

    inner class EmoteMenuItem<T>(val name: String, private val emote: T) :
        BindableItem<EmoteMenuItemBinding>() {

        override fun getLayout(): Int = R.layout.emote_menu_item

        override fun bind(viewBinding: EmoteMenuItemBinding, position: Int) {
            if (emote is Bitmap) {
                viewBinding.imageViewEmote.setImageBitmap(emote as Bitmap)
            } else {
                viewBinding.imageViewEmote.setImageDrawable(emote as GifDrawable)
            }

            viewBinding.imageViewEmote.setOnClickListener {
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

        override fun initializeViewBinding(view: View): EmoteMenuItemBinding {
            return EmoteMenuItemBinding.bind(view)
        }
    }
}