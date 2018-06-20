package com.lionheart.android.lionheartimages.ui

import android.animation.Animator
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.AnimationDrawable
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.transition.Transition
import android.support.transition.TransitionManager
import android.transition.Explode
import android.transition.Fade
import android.transition.Slide
import android.transition.TransitionInflater
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import com.lionheart.android.lionheartimages.R
import kotlinx.android.synthetic.main.activity_welcome_screen.*

class WelcomeScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val slide = Slide(Gravity.TOP)
        slide.duration = 900
        window.exitTransition = slide
        setContentView(R.layout.activity_welcome_screen)

        // star the gif
        val gif = gif_image.drawable as AnimationDrawable
        gif.start()

        // set the continue onclick
        continue_text.setOnClickListener {
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
                    .setDuration(200)
                    .setStartDelay(800)
                    .start()
            // fb translation
            facebook_login_button_welcome.animate()
                    .x(xTranslation.toFloat())
                    .setDuration(200)
                    .setStartDelay(1100)
                    .start()
            //val intent = Intent(this, MainActivity::class.java)
            //startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
        }

        // set the return on click
        return_text.setOnClickListener {
            continue_text.text = getString(R.string.return_start_editing)
            // set up the transition mgr to do anims
            TransitionManager.beginDelayedTransition(transitions_container)
            // grab the screen info
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels

            // insta translation
            instagram_login_button_welcome.animate()
                    .x(screenWidth.toFloat())
                    .setDuration(200)
                    .start()
            // fb translation
            facebook_login_button_welcome.animate()
                    .x(screenWidth.toFloat())
                    .setDuration(300)
                    .start()

            // change visibilities
            instagram_login_button_welcome.visibility = View.INVISIBLE
            facebook_login_button_welcome.visibility = View.INVISIBLE
            gif_image.visibility = View.VISIBLE
            continue_text.visibility = View.VISIBLE
            login_prompt_text.visibility = View.GONE
            return_text.visibility = View.GONE
        }
    }
}
