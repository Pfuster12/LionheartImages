package com.lionheart.android.lionheartimages.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimationDrawable
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.transition.TransitionManager
import android.support.v4.content.ContextCompat
import android.transition.Slide
import android.util.Base64
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
import com.lionheart.android.lionheartimages.R
import kotlinx.android.synthetic.main.activity_welcome_screen.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

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
        val INSTA_AUTH_EXTRA_KEY = "com.lionheart.android.lionheartimages.INSTA_AUTH_EXTRA_KEY"
    }
    // create the Facebook callbackManager to handle login responses
    private lateinit var mCallbackManager: CallbackManager
    // fb permission tag
    private val USER_PHOTOS = "user_photos"

    private val REDIRECT_URI = "http://android.lionheart.com"
    private val INSTA_AUTH_URL = "https://api.instagram.com/oauth/authorize/?client_id="+
            "545d40491e7e42f6ab16ccc15731e9e9" +
            "&redirect_uri=" +
            REDIRECT_URI + "&response_type=token"
    private lateinit var accessTokenFB: AccessToken
    private var accessTokenInsta: String? = "none_insta"

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
        slide.duration = 800
        window.exitTransition = slide
        val slideEnter = Slide(Gravity.BOTTOM)
        window.enterTransition = slideEnter
        // set content after anim set up
        setContentView(R.layout.activity_welcome_screen)

        // start the gif
        val gif = gif_image.drawable as AnimationDrawable
        gif.start()

        // set the buttons and animations
        setAnimations()
        setFBButton()
        setInstaButton()
        setProceedToMainButton()

        if (!isNetworkConnected()) {
            Snackbar.make(transitions_container, getString(R.string.no_connection), Snackbar.LENGTH_SHORT)
        }
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
            loggedCheck.fbLogged = false
            setFBButtonLogIn()
            // if not then log in with the click
            facebook_login_button_welcome.setOnClickListener {
                LoginManager.getInstance().logInWithReadPermissions(this, arrayListOf(USER_PHOTOS))
            }
            // hide the proceed text if not shown already
            if (!loggedCheck.instaLogged) {
                with(next_activity_text) {
                    visibility = View.INVISIBLE
                    animate().alpha(0f).start()
                }
            }
        } else {
            // set the logger check to true for fb
            loggedCheck.fbLogged = true

            // set the text and color to log out
            setFBButtonLogOut()

            // set a new on click to log out using the manager
            facebook_login_button_welcome.setOnClickListener {
                LoginManager.getInstance().logOut()
                setFBButton()
                setFBButtonLogIn()
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
                        accessTokenFB = result!!.accessToken
                        Toast.makeText(this@WelcomeScreenActivity,
                        "User logged in", Toast.LENGTH_SHORT)
                        .show()

                        // set log check to true
                        loggedCheck.fbLogged = true

                        // set the text and color to log out
                        setFBButtonLogOut()
                        setFBButton()

                        // show the proceed text if not shown already
                        with(next_activity_text) {
                            if (visibility == View.INVISIBLE) {
                                visibility = View.VISIBLE
                                animate().alpha(1f).start()
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
                                visibility = View.INVISIBLE
                                animate().alpha(0f).start()
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
        checkInstaToken()
        // set the button appearance
        if (loggedCheck.instaLogged) {
            // change the button to greyed out
            with(instagram_login_button_welcome) {
                text = getString(R.string.fb_log_out)
                background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
            }
        } else {
            //hide  the proceed text if not shown already
            with(next_activity_text) {
                visibility = View.INVISIBLE
                animate().alpha(0f).start()
            }
            // set button look
            with(instagram_login_button_welcome) {
                text = getString(R.string.instagram_title)
                background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorInsta))
            }
        }
        // set the on click listener
        instagram_login_button_welcome.setOnClickListener {
            if (loggedCheck.instaLogged) {
                // set logged to false
                loggedCheck.instaLogged = false

                // set button look
                with(instagram_login_button_welcome) {
                    text = getString(R.string.instagram_title)
                    background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorInsta))
                }
                // write out the token
                getPreferences(Context.MODE_PRIVATE)
                        .edit()
                        .remove(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY)
                        .apply()
            } else {
                // launch an intent for the web view with the url as data
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra(INTENT_INSTA_URL_KEY, INSTA_AUTH_URL)
                startActivityForResult(intent, INSTA_AUTH_RESULT_CODE_OK)
            }
        }
    }

    // check for insta auth
    private fun checkInstaToken() {
        // check the prefs
        val prefs = getPreferences(Context.MODE_PRIVATE)
        if (prefs.contains(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY)) {
            // grab the token
            val token = prefs.getString(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY, accessTokenInsta)
            if (token.isNotEmpty()) {
                loggedCheck.instaLogged = true
                accessTokenInsta = token
            }
        } else {
            loggedCheck.instaLogged = false
        }
    }

    /**
     * Helper fun to set on click to start next activity and pick a photo
     */
    private fun setProceedToMainButton() {
        next_activity_text.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // pass the instagram access token to the next activity if its received
            if (accessTokenInsta != "none_insta") {
                intent.putExtra(INSTA_AUTH_EXTRA_KEY, accessTokenInsta)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        lion_kon_2.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // pass the instagram access token to the next activity if its received
            if (accessTokenInsta != "none_insta") {
                intent.putExtra(INSTA_AUTH_EXTRA_KEY, accessTokenInsta)
            }
            startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }
    }

    /**
     * Set the on click to show buttons and animate the layout changes with
     * transition manager.
     */
    private fun setAnimations() {
        // grab the screen info
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val screenWidth = metrics.widthPixels

        // animate initial alphas for welcome screen
        logo_text.animate().alpha(1f).setStartDelay(500).setDuration(500).start()
        lion_kon.animate().alpha(1f).setStartDelay(600).setDuration(300).start()
        continue_text.animate().alpha(1f).setStartDelay(700).setDuration(300).start()

        // set the continue onclick
        continue_text.setOnClickListener {

            if (continue_text.text == getString(R.string.return_start_editing)) {
                // change login text
                login_prompt_text.text = getString(R.string.return_login_prompt)
            }

            // set up the transition mgr to do anims
            TransitionManager.beginDelayedTransition(transitions_container)
            // change visibilities
            gif_image.visibility = View.GONE
            continue_text.visibility = View.GONE
            lion_kon.visibility = View.GONE
            lion_kon_2.visibility = View.VISIBLE
            login_prompt_text.visibility = View.VISIBLE
            facebook_login_button_welcome.visibility = View.VISIBLE
            instagram_login_button_welcome.visibility = View.VISIBLE
            return_text.visibility = View.VISIBLE

            // insta translation
            val xTranslation = screenWidth.div(2) - instagram_login_button_welcome.layoutParams.width.div(2)
            instagram_login_button_welcome.animate()
                    .x(xTranslation.toFloat())
                    .setDuration(400)
                    .setStartDelay(900)
                    .start()

            // fb translation right after
            facebook_login_button_welcome.animate()
                    .x(xTranslation.toFloat())
                    .setDuration(500)
                    .setStartDelay(900)
                    .start()

            if (loggedCheck.fbLogged || loggedCheck.instaLogged) {
                // show the proceed text if not shown
                with(next_activity_text) {
                    visibility = View.VISIBLE
                    animate().alpha(1f).start()
                }
            }
        }

        // set the return on click
        return_text.setOnClickListener {
            // insta translation
            instagram_login_button_welcome.x = screenWidth.toFloat()
            // fb translation
            facebook_login_button_welcome.x = screenWidth.toFloat()

            // set up the transition mgr to do anims
            TransitionManager.beginDelayedTransition(transitions_container)
            continue_text.text = getString(R.string.return_start_editing)
            // change visibilities
            login_prompt_text.visibility = View.GONE
            gif_image.visibility = View.VISIBLE
            continue_text.visibility = View.VISIBLE
            lion_kon_2.visibility = View.GONE
            return_text.visibility = View.GONE
            lion_kon.visibility = View.VISIBLE
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

    // Helper fun to set fb button to prompt log in fb blue
    private fun setFBButtonLogIn() {
        // set the title and color back
        with(facebook_login_button_welcome) {
            text = getString(R.string.facebook_title)
            background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorFacebookBlue))
            alpha = 1.0f
        }
    }

    // Helper fun to set fb button to show log out in grey
    private fun setFBButtonLogOut() {
        // set the title and color back
        with(facebook_login_button_welcome) {
            text = getString(R.string.fb_log_out)
            background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
            alpha = 0.8f
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

                with(instagram_login_button_welcome) {
                    text = getString(R.string.fb_log_out)
                    background.setTint(ContextCompat.getColor(this@WelcomeScreenActivity, R.color.colorTextGray))
                }

                // show the proceed text if not shown already
                with(next_activity_text) {
                    visibility = View.VISIBLE
                    animate().alpha(1f).start()
                }

                // grab the url fragment
                val fragment = data?.getStringExtra(WebViewActivity.RESULT_AUTH_INTENT_KEY)
                // cull the text parameter
                val authCode = fragment?.drop(13)
                // set gloal var to the authcode
                accessTokenInsta = authCode
                Log.e("INSTA_TOKEN", accessTokenInsta)
                // save the code to preferences if user leaves app
                instaAuthPreferenceSave(accessTokenInsta)
                Toast.makeText(this@WelcomeScreenActivity,
                        "Instagram logged in", Toast.LENGTH_SHORT)
                        .show()

            }
            INSTA_AUTH_RESULT_CODE_ERROR -> {
                // set the log to false
                loggedCheck.instaLogged = false
                // show toast
                Toast.makeText(this@WelcomeScreenActivity,
                        "Instagram Log-in Failed", Toast.LENGTH_SHORT)
                        .show()
                // check if facebook is logged out
                if (!loggedCheck.fbLogged) {
                    // hide the proceed text
                    with(next_activity_text) {
                        visibility = View.INVISIBLE
                        animate().alpha(0f).start()
                    }
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Helper fun for network check
     */
    private fun isNetworkConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return activeNetwork?.isConnectedOrConnecting == true
    }

    /**
     * function to store the instagram authorisation token
     */
    private fun instaAuthPreferenceSave(token: String?) {
        val preferences = getPreferences(Context.MODE_PRIVATE)
        preferences.edit()
                .putString(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY, token)
                .apply()
    }

    override fun onBackPressed() {
        finishAfterTransition()
        super.onBackPressed()
    }
}
