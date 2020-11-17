package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import gg.strims.android.ChatActivity
import gg.strims.android.R
import io.ktor.util.*
import kotlinx.android.synthetic.main.fragment_login.*

@SuppressLint("SetJavaScriptEnabled")
@KtorExperimentalAPI
class LoginFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        loginWebView.settings.javaScriptEnabled = true
        loginWebView.settings.domStorageEnabled = true
        loginWebView.loadUrl("https://strims.gg/login")
        loginWebView.webViewClient = object: WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                    val activity = requireActivity() as ChatActivity
                    activity.onBackPressed()
                    activity.stopService(activity.chatSocketIntent)
                    activity.startService(activity.chatSocketIntent)
                }
            }
        }
    }
}