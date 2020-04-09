package gg.strims.android.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import gg.strims.android.*
import io.ktor.util.KtorExperimentalAPI
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
        hideFragment(activity!!, this)
        loginWebView.settings.javaScriptEnabled = true
        loginWebView.settings.domStorageEnabled = true
        loginWebView.loadUrl("https://strims.gg/login")
        loginWebView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                    fragmentManager!!.beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .hide(this@LoginFragment)
                        .commit()
                }
            }
        }
    }
}