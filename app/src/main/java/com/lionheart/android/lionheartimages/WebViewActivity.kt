package com.lionheart.android.lionheartimages

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.android.synthetic.main.activity_web_view.*

class WebViewActivity : AppCompatActivity() {

    // key strings in companion object
    companion object {
        val RESULT_AUTH_INTENT_KEY = "com.lionheart.android.lionheartimages.RESULT_AUTH_KEY"
        val INSTA_REDIRECT_URI = "android.lionheart.com"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        setWebView()
    }

    private fun setWebView() {
        // grab the url data that launched this activity
        val webViewUrl = intent?.extras?.getString(StartUpActivity.INTENT_INSTA_URL_KEY)

        Log.e("TAG", webViewUrl.toString())
        webview.settings.javaScriptEnabled = true
        // set the client to capture the url at each step
        webview.webViewClient = object : WebViewClient() {
            /**
             * Override fun to check the url loaded and finish webview when reached the auth.
             */
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?) =
                    when (request?.url?.host.equals(INSTA_REDIRECT_URI)) {
                        true -> {
                            val resultIntent = Intent()
                            resultIntent.putExtra(WebViewActivity.RESULT_AUTH_INTENT_KEY,
                                    request?.url?.fragment)
                            setResult(StartUpActivity.INSTA_AUTH_RESULT_CODE, resultIntent)
                            finish()
                            true
                        }
                        false -> false
                    }
        }

        // set the webview to load the url captured in the intent extra
        webview.loadUrl(webViewUrl)
    }
}
