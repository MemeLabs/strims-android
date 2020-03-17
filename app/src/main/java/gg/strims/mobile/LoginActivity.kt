package gg.strims.mobile

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient

class LoginActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Log.d("TAG", "Login opened")

        /***Creates WebView (Browser***/
        val webView = findViewById<WebView>(R.id.loginWebView)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("https://strims.gg/login")

        retrieveCookie(webView, "https://strims.gg")
    }

    /**Gets cookies from WebView**/
    private fun retrieveCookie(view: WebView, url: String) {
        val cookies = CookieManager.getInstance().getCookie(url)
        Log.d("TAG", "cookie: $cookies")
    }
}
