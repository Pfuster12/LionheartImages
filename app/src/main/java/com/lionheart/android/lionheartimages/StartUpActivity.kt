package com.lionheart.android.lionheartimages

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.android.synthetic.main.activity_start_up.*
import java.util.*

/**
 * Activity that handles the welcome screen and social media login functionality.
 */
class StartUpActivity : AppCompatActivity() {

    /*
     / Global variables
     */
    // log tag
    val LOG_TAG = StartUpActivity::class.java.simpleName

    // create the Facebook callbackManager to handle login responses
    private lateinit var mCallbackManager: CallbackManager
    // fb permission tag
    private val USER_PHOTOS = "user_photos"

    private val REDIRECT_URI = "http://android.lionheart.com"
    private val INSTA_AUTH_URL = "https://api.instagram.com/oauth/authorize/?client_id="+
            BuildConfig.INSTA_CLIENT_ID +
            "&redirect_uri=" +
            REDIRECT_URI + "&response_type=token"

    companion object {
        val INTENT_INSTA_URL_KEY = "com.lionheart.android.lionheartimages.INSTA_URL_KEY"
        val INSTA_AUTH_RESULT_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)

        // set the facebook login button
        setFBButton()

        setInstaButton()

        // check if app is logged in to fb through the access token
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
    }

    /**
     * Helper fun to set up facebook login button
     */
    private fun setFBButton() {
        // set the user_photos permission in the login button.
        fb_login_button.setReadPermissions(arrayListOf(USER_PHOTOS))

        // init the manager
        mCallbackManager = CallbackManager.Factory.create()
        // to respond to a login result, you need to register a callback with LoginManager
        LoginManager.getInstance()
                .registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult?) {
                // success login, capture the access token
            }

            override fun onCancel() {
                // user cancels
            }

            override fun onError(error: FacebookException?) {
                // error login in
            }
        })
    }

    /**
     * Helper fun to set up instagram login button
     */
    private fun setInstaButton() {
        instagram_login_button.setOnClickListener {
            // launch an intent for the webview with the url as data
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra(INTENT_INSTA_URL_KEY, INSTA_AUTH_URL)
            startActivityForResult(intent, INSTA_AUTH_RESULT_CODE)
        }
    }

    /**
     * override to receive login results from social media apps
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // callback to the facebook manager
        mCallbackManager.onActivityResult(requestCode, resultCode, data)
        if (resultCode == INSTA_AUTH_RESULT_CODE) {
            val authCode = data?.getStringExtra(WebViewActivity.RESULT_AUTH_INTENT_KEY)
            Log.e(LOG_TAG, authCode.toString())
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
