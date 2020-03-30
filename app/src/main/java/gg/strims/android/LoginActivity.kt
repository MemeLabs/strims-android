package gg.strims.android

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import io.ktor.util.KtorExperimentalAPI

class LoginActivity : AppCompatActivity() {

    @KtorExperimentalAPI
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        supportActionBar?.hide()

        Log.d("TAG", "Login opened")

        val webView = findViewById<WebView>(R.id.loginWebView)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url == "https://strims.gg/" || url == "https://chat.strims.gg/") {
                    startActivity(Intent(this@LoginActivity, ChatActivity::class.java))
                    finish()
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.loadUrl("https://strims.gg/login")
    }
}
