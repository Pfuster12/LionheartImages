package com.lionheart.android.lionheartimages.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.transition.Fade
import android.transition.TransitionInflater
import android.util.Log
import com.facebook.AccessToken
import com.lionheart.android.lionheartimages.R
import com.lionheart.android.lionheartimages.model.ImagesViewModel
import com.lionheart.android.lionheartimages.pojo.LionheartImage

/**
 * Activity handling display of the main list of images stored in the app for editing and sharing.
 */
class MainActivity : AppCompatActivity() {

    /*
     / global variables
     */

    // shared prefs for a startup activity check
    private lateinit var mSharedPreferences: SharedPreferences
    // shared prefs key for boolean
    private val START_UP_IS_SHOWN = "com.lionheart.android.lionheartimages.START_UP_IS_SHOWN"
    // lateinit
    private lateinit var viewModel: ImagesViewModel
    private var accessToken = "nope"

    /*
    / functions
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set the transition anim
        val fade = Fade()
        fade.duration = 500
        fade.startDelay = 500
        window.enterTransition = fade
        setContentView(R.layout.activity_main)


        if (isFBLoggedIn()) {
            accessToken = AccessToken.getCurrentAccessToken().token
        }
        // load images and observe the livedata
        observeViewModel()
    }

    /**
     * Helper fun to check fb token is current
     */
    private fun isFBLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    /**
     * Helper fun to init the view model and observe the live data object.
     */
    private fun observeViewModel() {
        // create the view model
        viewModel = ViewModelProviders.of(this).get(ImagesViewModel::class.java)
        // observe changes of the LiveData object
        // load the images from the db or fetch from the apis
        viewModel.init(this,
                Pair("fields", "photos.limit(6){height,id,images,link,name,width}"),
                Pair("access_token", accessToken))
                .getImages()?.observe(this, Observer { images ->
            // TODO fill list with dummies if it is null
            if (images != null) {
                for (image in images.iterator()) {
                    Log.e("HEYY I'M HERE", image.imageLink)
                }
            }
        })
    }

    /**
     * Helper fun to check if start up screen has been shown.
     */
    private fun checkStartUpScreen() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // grab the boolean with a false default
        val isStartUpShown = mSharedPreferences.getBoolean(START_UP_IS_SHOWN, false)

        when (isStartUpShown) {
            false -> {
                mSharedPreferences.edit().putBoolean(START_UP_IS_SHOWN, true).apply()
                // send the activity intent
                val intent = Intent(this, StartUpActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}
