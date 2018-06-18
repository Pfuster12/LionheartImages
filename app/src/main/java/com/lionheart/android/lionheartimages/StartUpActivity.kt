package com.lionheart.android.lionheartimages

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Activity that handles the welcome screen and social media login functionality.
 */
class StartUpActivity : AppCompatActivity() {

    val LOG_TAG = StartUpActivity::class.java.simpleName

    // create the Facebook callbackManager to handle login responses
    private lateinit var mCallbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_up)

        // set the facebook login button
        setFBButton()
    }

    /**
     * Helper fun to set up facebook login button
     */
    private fun setFBButton() {
        // init the manager
        mCallbackManager = CallbackManager.Factory.create()
        // to respond to a login result, you need to register a callback with LoginManager
        LoginManager.getInstance()
                .registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult?) {
                // success login, capture the access token
                Log.e(LOG_TAG, result.toString())
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
     * override to receive login results from social media apps
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // callback to the facebook manager
        mCallbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }
}
