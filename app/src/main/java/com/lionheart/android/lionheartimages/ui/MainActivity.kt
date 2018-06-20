package com.lionheart.android.lionheartimages.ui

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
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
    private lateinit var viewModel: ImagesViewModel
    private var accessToken = "nope"

    /*
    / functions
    */



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // run the start up check
        checkStartUpScreen()

        accessToken = AccessToken.getCurrentAccessToken().token
        Log.e("WHYYY", accessToken)
        // load images and observe the livedata
        observeViewModel()
    }

    private fun observeViewModel() {
        // create the viewmodel
        viewModel = ViewModelProviders.of(this).get(ImagesViewModel::class.java)
        // observe changes of the LiveData object
        // load the images from the db or fetch from the apis
        viewModel.init(this,
                Pair("fields", "photos.limit(6)"),
                Pair("access_token", accessToken))
                .getImages()?.observe(this, Observer { images ->
            // TODO fill list with dummies if it is null
            if (images != null) {
                for (image in images.iterator()) {
                    Log.e("OBSERVER", image.imageLink)
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
}
