package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import io.ktor.util.*
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.fragment_twitch.*

@SuppressLint("SetJavaScriptEnabled")
@KtorExperimentalAPI
class TwitchFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_twitch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(requireActivity(), this)

        webViewTwitch.settings.domStorageEnabled = true
        webViewTwitch.settings.javaScriptEnabled = true

        twitchClose.setOnClickListener {
            webViewTwitch.loadUrl("")
            CurrentUser.tempTwitchVod = null
            CurrentUser.tempTwitchUrl = null
            parentFragmentManager.beginTransaction()
                .hide(this)
                .commit()

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                requireActivity().constraintLayoutStream.visibility = View.GONE
            }
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempTwitchUrl != null && !hidden) {
            if (CurrentUser.tempTwitchVod!!) {
                webViewTwitch.loadUrl("https://player.twitch.tv/?video=${CurrentUser.tempTwitchUrl}&parent=strims.gg")
            } else {
                webViewTwitch.loadUrl("https://player.twitch.tv/?channel=${CurrentUser.tempTwitchUrl}&parent=strims.gg")
            }
        } else {
            webViewTwitch.loadUrl("")
        }
    }
}