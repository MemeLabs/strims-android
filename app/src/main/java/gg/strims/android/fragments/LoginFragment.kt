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
import gg.strims.android.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.app_bar_main.*
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
        requireActivity().toolbar.title = "Login"

        loginWebView.settings.javaScriptEnabled = true
        loginWebView.settings.domStorageEnabled = true
        loginWebView.loadUrl("https://strims.gg/login")
        loginWebView.webViewClient = object: WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                    val activity = requireActivity() as ChatActivity
                    activity.stopService(activity.chatSocketIntent)
                    activity.startService(activity.chatSocketIntent)
                    parentFragmentManager.beginTransaction()
                        .remove(this@LoginFragment)
                        .commit()
                    parentFragmentManager.popBackStack()
                    activity.toolbar.title = "Chat"
                    activity.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }
}