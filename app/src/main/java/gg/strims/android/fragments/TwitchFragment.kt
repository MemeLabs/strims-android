package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import gg.strims.android.databinding.FragmentTwitchBinding
import gg.strims.android.hideChildFragment
import gg.strims.android.viewBinding
import gg.strims.android.viewmodels.TwitchViewModel
import io.ktor.util.*

@SuppressLint("SetJavaScriptEnabled")
@KtorExperimentalAPI
class TwitchFragment: Fragment() {

    val binding by viewBinding(FragmentTwitchBinding::bind)

    private lateinit var twitchViewModel: TwitchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return FragmentTwitchBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideChildFragment(requireParentFragment(), this)

        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)

        binding.webViewTwitch.settings.domStorageEnabled = true
        binding.webViewTwitch.settings.javaScriptEnabled = true

        binding.twitchClose.setOnClickListener {
            binding.webViewTwitch.loadUrl("")
            twitchViewModel.channel.value = null
            twitchViewModel.vod = false

            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                (requireParentFragment() as ChatFragment).binding.constraintLayoutStreamFragment?.visibility =
                    View.GONE
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (twitchViewModel.channel.value != null && !hidden) {
            if (twitchViewModel.vod) {
                binding.webViewTwitch.loadUrl("https://player.twitch.tv/?video=${twitchViewModel.channel.value}&parent=strims.gg")
            } else {
                binding.webViewTwitch.loadUrl("https://player.twitch.tv/?channel=${twitchViewModel.channel.value}&parent=strims.gg")
            }
        } else {
            binding.webViewTwitch.loadUrl("")
        }
    }
}