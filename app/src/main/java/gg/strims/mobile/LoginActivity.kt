package gg.strims.mobile

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.util.KtorExperimentalAPI

class LoginActivity : AppCompatActivity() {

    var jwt: String? = null

    @KtorExperimentalAPI
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        Log.d("TAG", "Login opened")

        /** Creates WebView, retrieves JWT and goes back to chat activity **/
        val webView = findViewById<WebView>(R.id.loginWebView)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                    retrieveCookie()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("https://strims.gg/login")
    }

    /** Gets cookies from WebView **/
    fun retrieveCookie() {
        val cookies = CookieManager.getInstance().getCookie("https://strims.gg")
        if (cookies != null) {
            Log.d("TAG", "cookie: $cookies")
            var jwt: String? = cookies.substringAfter("jwt=").substringBefore(" ")
            if (jwt == cookies) {
                jwt = null
            }
            this.jwt = jwt
            Log.d("TAG", "JWT: $jwt")
        }
    }
}
