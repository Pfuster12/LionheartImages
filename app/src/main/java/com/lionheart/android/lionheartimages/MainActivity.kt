package com.lionheart.android.lionheartimages

import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // run the start up check
        checkStartUpScreen()
    }

    /**
     * Helper fun to check if start up screen has been shown.
     */
    private fun checkStartUpScreen() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // grab the boolean with a false default
        val isStartUpShown = mSharedPreferences.getBoolean(START_UP_IS_SHOWN, false)

        when (isStartUpShown) {
            true -> {
                // do nothing
            }
            false -> {
                // send the activity intent
                val intent = Intent(this, StartUpActivity::class.java)
                startActivity(intent)
            }
        }
    }
}
