package com.lionheart.android.lionheartimages.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.transition.Slide
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.lionheart.android.lionheartimages.BuildConfig
import com.lionheart.android.lionheartimages.R
import kotlinx.android.synthetic.main.activity_welcome_screen.*

/**
 * Welcome screen greeting the user and handling log in buttons presentation
 */
class WelcomeScreenActivity : AppCompatActivity() {

    /*
    / Global variables
    */

    // log tag
    val LOG_TAG = WelcomeScreenActivity::class.java.simpleName

    companion object {
        val INTENT_INSTA_URL_KEY = "com.lionheart.android.lionheartimages.INSTA_URL_KEY"
        val INSTA_AUTH_RESULT_CODE_OK = 101
        val INSTA_AUTH_RESULT_CODE_ERROR = 102
    }

    // create the Facebook callbackManager to handle login responses
    private lateinit var mCallbackManager: CallbackManager
    // fb permission tag
    private val USER_PHOTOS = "user_photos"

    private val REDIRECT_URI = "http://android.lionheart.com"
    private val INSTA_AUTH_URL = "https://api.instagram.com/oauth/authorize/?client_id="+
            BuildConfig.INSTA_CLIENT_ID +
            "&redirect_uri=" +
            REDIRECT_URI + "&response_type=token"
    private lateinit var accessToken: AccessToken

    private val loggedCheck = object {
        var instaLogged = false
        var fbLogged = false
    }

    /*
     / functions
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set the exit transition for the activity
        val slide = Slide(Gravity.TOP)
        slide.duration = 900
        window.exitTransition = slide
        setContentView(R.layout.activity_welcome_screen)

        // start the gif
        val gif = gif_image.drawable as AnimationDrawable
        gif.start()

        // set the buttons and animations
        setAnimations()
        setFBButton()
        setInstaButton()
        setProceedToMainButton()
    }

    /**
     * Helper fun to check fb token is current
     */
    private fun isFBLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    /**
     * Helper fun to set up facebook login button
     */
    private fun setFBButton() {
        // set the user_photos permission in the login button.
        // check if auth is already given
        if (!isFBLoggedIn()) {
            // set the title and color back
            with(facebook_login_button_welcome) {
                text = getString(R.string.facebook_title)
                background
                        .setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorFacebookBlue))
            }
            // if not then log in with the click
            facebook_login_button_welcome.setOnClickListener {
                LoginManager.getInstance().logInWithReadPermissions(this, arrayListOf(USER_PHOTOS))
            }
        } else {
            // set the logger check to true for fb
            loggedCheck.fbLogged = true


            // set the text and color to log out
            with(facebook_login_button_welcome) {
                background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
                text = getString(R.string.fb_log_out)
            }

            // set a new on click to log out using the manager
            facebook_login_button_welcome.setOnClickListener {
                LoginManager.getInstance().logOut()

                // set the title and color back
                with(facebook_login_button_welcome) {
                    text = getString(R.string.facebook_title)
                    background
                            .setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorFacebookBlue))
                }
            }
        }

        // init the manager
        mCallbackManager = CallbackManager.Factory.create()
        // to respond to a login result, you need to register a callback with LoginManager
        LoginManager.getInstance()
                .registerCallback(mCallbackManager, object : FacebookCallback<LoginResult> {

                    override fun onSuccess(result: LoginResult?) {
                        // success login, capture the access token
                        // check if app is logged in to fb through the access token
                        accessToken = result!!.accessToken
                        Toast.makeText(this@WelcomeScreenActivity,
                        "User logged in", Toast.LENGTH_SHORT)
                        .show()

                        // set log check to true
                        loggedCheck.fbLogged = true

                        // set the text and color to log out
                        with(facebook_login_button_welcome) {
                            background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
                            text = getString(R.string.fb_log_out)
                        }

                        // show the proceed text if not shown already
                        with(next_activity_text) {
                            if (visibility == View.INVISIBLE) {
                                visibility = View.VISIBLE
                                animate().alpha(1f).start()
                            }
                        }

                        // set a new on click on the button to log out using the mngr
                        facebook_login_button_welcome.setOnClickListener {
                            LoginManager.getInstance().logOut()

                            // set the title and color back
                            with(facebook_login_button_welcome) {
                                text = getString(R.string.facebook_title)
                                background
                                        .setTint(ContextCompat
                                                .getColor(this@WelcomeScreenActivity,
                                                R.color.colorFacebookBlue))
                            }
                        }
                    }

                    override fun onCancel() {
                        // user cancels
                        Toast.makeText(this@WelcomeScreenActivity,
                                "User cancelled", Toast.LENGTH_SHORT)
                                .show()
                        // check if instagram is logged out
                        if (!loggedCheck.instaLogged) {
                            // hide the proceed text
                            with(next_activity_text) {
                                if (visibility == View.VISIBLE) {
                                    visibility = View.INVISIBLE
                                    animate().alpha(0f).start()
                                }
                            }
                        }
                    }

                    override fun onError(error: FacebookException?) {
                        // error login in
                        Toast.makeText(this@WelcomeScreenActivity,
                                "Error logging in", Toast.LENGTH_SHORT)
                                .show()
                        // check if instagram is logged out
                        if (!loggedCheck.instaLogged) {
                            // hide the proceed text
                            with(next_activity_text) {
                                if (visibility == View.VISIBLE) {
                                    visibility = View.INVISIBLE
                                    animate().alpha(0f).start()
                                }
                            }
                        }
                    }
                })
    }

    /**
     * Helper fun to set up instagram login button
     */
    private fun setInstaButton() {
        instagram_login_button_welcome.setOnClickListener {
            // launch an intent for the web view with the url as data
            val intent = Intent(this, WebViewActivity::class.java)
            intent.putExtra(INTENT_INSTA_URL_KEY, INSTA_AUTH_URL)
            startActivityForResult(intent, INSTA_AUTH_RESULT_CODE_OK)
        }
    }

    private fun setProceedToMainButton() {
        next_activity_text.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    /**
     * Set the on click to show buttons and animate the layout changes with
     * transition manager.
     */
    private fun setAnimations() {
        // set the continue onclick
        continue_text.setOnClickListener {

            if (continue_text.text == getString(R.string.return_start_editing)) {
                // change login text
                login_prompt_text.text = getString(R.string.return_login_prompt)
            }
            // grab the screen info
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels

            // set up the transition mgr to do anims
            TransitionManager.beginDelayedTransition(transitions_container)
            // change visibilities
            gif_image.visibility = View.GONE
            continue_text.visibility = View.GONE
            login_prompt_text.visibility = View.VISIBLE
            instagram_login_button_welcome.visibility = View.VISIBLE
            facebook_login_button_welcome.visibility = View.VISIBLE
            return_text.visibility = View.VISIBLE

            // insta translation
            val xTranslation = screenWidth.div(2) - instagram_login_button_welcome.layoutParams.width.div(2)
            instagram_login_button_welcome.animate()
                    .x(xTranslation.toFloat())
                    .setDuration(250)
                    .setStartDelay(800)
                    .start()
            // fb translation
            facebook_login_button_welcome.animate()
                    .x(xTranslation.toFloat())
                    .setDuration(250)
                    .setStartDelay(1100)
                    .start()
            facebook_login_button_welcome.visibility = View.VISIBLE

            if (loggedCheck.fbLogged || loggedCheck.instaLogged) {
                // show the proceed text if not shown already
                with(next_activity_text) {
                    if (visibility == View.INVISIBLE) {
                        visibility = View.VISIBLE
                        animate().alpha(1f).start()
                    }
                }
            }
        }

        // set the return on click
        return_text.setOnClickListener {
            // grab the screen info
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels

            // insta translation
            instagram_login_button_welcome.animate()
                    .x(screenWidth.toFloat())
                    .start()
            // fb translation
            facebook_login_button_welcome.animate()
                    .x(screenWidth.toFloat())
                    .start()

            // set up the transition mgr to do anims
            TransitionManager.beginDelayedTransition(transitions_container)
            continue_text.text = getString(R.string.return_start_editing)
            // change visibilities
            login_prompt_text.visibility = View.GONE
            gif_image.visibility = View.VISIBLE
            continue_text.visibility = View.VISIBLE
            return_text.visibility = View.GONE
            instagram_login_button_welcome.visibility = View.INVISIBLE
            facebook_login_button_welcome.visibility = View.INVISIBLE

            // hide the proceed text
            with(next_activity_text) {
                if (visibility == View.VISIBLE) {
                    visibility = View.INVISIBLE
                    animate().alpha(0f).start()
                }
            }
        }
    }

    /**
     * override to receive login results from social media apps
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // callback to the facebook manager
        mCallbackManager.onActivityResult(requestCode, resultCode, data)

        // check if the instagram auth flow returns and access token and retrieve it
        when (resultCode) {
            INSTA_AUTH_RESULT_CODE_OK -> {
                // set the insta log check to true
                loggedCheck.instaLogged = true

                // show the proceed text if not shown already
                with(next_activity_text) {
                    if (visibility == View.INVISIBLE) {
                        visibility = View.VISIBLE
                        animate().alpha(1f).start()
                    }
                }

                // grab the url fragment
                val fragment = data?.getStringExtra(WebViewActivity.RESULT_AUTH_INTENT_KEY)
                // cull the text parameter
                val authCode = fragment?.drop(13)
                Log.e(LOG_TAG, authCode.toString())
                Toast.makeText(this@WelcomeScreenActivity,
                        "Insta yes", Toast.LENGTH_SHORT)
                        .show()

                // change the button to greyed out
                with(instagram_login_button_welcome) {
                    text = getString(R.string.insta_logged_in)
                    background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
                }
            }
            INSTA_AUTH_RESULT_CODE_ERROR -> {
                Toast.makeText(this@WelcomeScreenActivity,
                        "Insta cancelled", Toast.LENGTH_SHORT)
                        .show()
                // check if facebook is logged out
                if (!loggedCheck.fbLogged) {
                    // hide the proceed text
                    with(next_activity_text) {
                        if (visibility == View.VISIBLE) {
                            visibility = View.INVISIBLE
                            animate().alpha(0f).start()
                        }
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
