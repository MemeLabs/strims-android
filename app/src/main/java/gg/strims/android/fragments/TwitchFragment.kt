package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideChildFragment
import gg.strims.android.viewmodels.TwitchViewModel
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_chat.*
import kotlinx.android.synthetic.main.fragment_twitch.*

@SuppressLint("SetJavaScriptEnabled")
@KtorExperimentalAPI
class TwitchFragment: Fragment() {

    private lateinit var twitchViewModel: TwitchViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_twitch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideChildFragment(requireParentFragment(), this)

        twitchViewModel = ViewModelProvider(requireActivity()).get(TwitchViewModel::class.java)

        webViewTwitch.settings.domStorageEnabled = true
        webViewTwitch.settings.javaScriptEnabled = true

        twitchClose.setOnClickListener {
            webViewTwitch.loadUrl("")
            twitchViewModel.channel.value = null
            twitchViewModel.vod = false

            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                (requireParentFragment() as ChatFragment).constraintLayoutStreamFragment.visibility =
                    View.GONE
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (twitchViewModel.channel.value != null && !hidden) {
            if (twitchViewModel.vod) {
                webViewTwitch.loadUrl("https://player.twitch.tv/?video=${twitchViewModel.channel.value}&parent=strims.gg")
            } else {
                webViewTwitch.loadUrl("https://player.twitch.tv/?channel=${twitchViewModel.channel.value}&parent=strims.gg")
            }
        } else {
            webViewTwitch.loadUrl("")
        }
    }
}