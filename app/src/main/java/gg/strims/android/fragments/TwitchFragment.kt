package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import gg.strims.android.CurrentUser
import gg.strims.android.R
import gg.strims.android.hideFragment
import kotlinx.android.synthetic.main.fragment_twitch.*

@SuppressLint("SetJavaScriptEnabled")
class TwitchFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_twitch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        hideFragment(activity!!, this)
        webViewTwitch.settings.domStorageEnabled = true
        webViewTwitch.settings.javaScriptEnabled = true
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (CurrentUser.tempTwitchUrl != null && !hidden) {
            webViewTwitch.loadUrl("https://player.twitch.tv/?channel=${CurrentUser.tempTwitchUrl}")
        } else {
            CurrentUser.tempTwitchUrl = null
            webViewTwitch.loadUrl("")
        }
    }
}