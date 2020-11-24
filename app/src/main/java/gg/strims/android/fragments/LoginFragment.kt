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
import gg.strims.android.MainActivity
import gg.strims.android.databinding.FragmentLoginBinding
import gg.strims.android.viewBinding
import io.ktor.util.*

@SuppressLint("SetJavaScriptEnabled")
@KtorExperimentalAPI
class LoginFragment: Fragment() {

    private val binding by viewBinding(FragmentLoginBinding::bind)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return FragmentLoginBinding.inflate(layoutInflater).root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        with (binding) {
            loginWebView.settings.javaScriptEnabled = true
            loginWebView.settings.domStorageEnabled = true
            loginWebView.loadUrl("https://strims.gg/login")
            loginWebView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                        with (requireActivity() as MainActivity) {
                            onBackPressed()
                            stopService(chatSocketIntent)
                            startService(chatSocketIntent)
                        }
                    }
                }
            }
        }
    }
}