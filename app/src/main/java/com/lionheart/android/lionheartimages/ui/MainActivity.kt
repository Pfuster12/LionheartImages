package com.lionheart.android.lionheartimages.ui

import android.animation.Animator
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.transition.TransitionManager
import android.support.v7.widget.GridLayoutManager
import android.transition.Fade
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.facebook.AccessToken
import com.lionheart.android.lionheartimages.R
import com.lionheart.android.lionheartimages.model.ImagesViewModel
import com.lionheart.android.lionheartimages.pojo.LionheartImage
import kotlinx.android.synthetic.main.activity_main.*
import android.net.Uri
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.transition.Slide
import android.view.Gravity
import android.widget.ImageView
import com.bumptech.glide.load.resource.bitmap.BitmapDrawableDecoder
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
    private val WELCOME_SCREEN_SHOWN = "com.lionheart.android.lionheartimages.WELCOME_SCREEN_SHOWN"
    // late init a view model in oncreate
    private lateinit var viewModel: ImagesViewModel
    // init a fb accessTokenFB, with nope so its non-null
    private var accessTokenFB = "nope"
    private var accessTokenInsta = "nope"

    private var CACHE_AUTHORITY = "com.lionheart.android.lionheartimages"

    private val FIRE_EMOJI = "fire"
    private val THUG_EMOJI = "thug"

    // recycler view adapter and list data
    private lateinit var adapter: LionheartRecyclerViewAdapter
    private lateinit var images: MutableList<LionheartImage>

    var selectedBitmap: Bitmap? = null

    /*
    / functions
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set the transition anim
        val fade = Fade()
        fade.duration = resources.getInteger(R.integer.fade_duration).toLong()
        window.enterTransition = fade
        val slide = Slide(Gravity.END)
        window.exitTransition = slide
        setContentView(R.layout.activity_main)

        // check if fb is logged in and grab the access Token
        if (isFBLoggedIn()) {
            accessTokenFB = AccessToken.getCurrentAccessToken().token
        }

        // grab the instagram token too
        if (intent != null && intent.hasExtra(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY)) {
            accessTokenInsta = intent.getStringExtra(WelcomeScreenActivity.INSTA_AUTH_EXTRA_KEY)
        }

        val snack = Snackbar.make(main_transitions_container,
                getString(R.string.swipe_prompt), Snackbar.LENGTH_LONG)
        snack.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
        snack.show()

        // load images and observe the live data
        observeViewModel()

        // set a recycler view
        setUpRecyclerView()

        // set buttons
        setToolbarActions()
        setFireEmojiButton()
        setThugEmojiButton()

        swipe_refresh.setOnRefreshListener {
            swipeToRefresh()
        }
    }

    /**
     * Helper fun to init the view model and observe the live data object.
     */
    private fun observeViewModel() {
        // create the view model
        viewModel = ViewModelProviders.of(this).get(ImagesViewModel::class.java)
        // observe changes of the LiveData object
        // load the images from the db or fetch from the apis
        // map the fb query params
        val fbQueryMap = mutableMapOf(Pair("fields", "photos.limit(6){height,id,images,link,name,width}"),
                Pair("access_token", accessTokenFB))
        // map the insta params
        val instaQueryMap = mutableMapOf(Pair("access_token", accessTokenInsta))

        // observe the livedata in the viewmodel
        viewModel.init(this, fbQueryMap, instaQueryMap)
                .getImages()?.observe(this, Observer { imagesData ->
                    swipe_refresh.isRefreshing = false
                    // when the livedata is updated from the database
                    // the result is called through here
                    if (imagesData != null && imagesData.isNotEmpty()) {
                        // fade the recycler view out
                        recycler_view.animate().alpha(0f).start()
                        // clear the list
                        images.clear()
                        // add the observed image data to the list handled by the adapter
                        images.addAll(imagesData)
                        adapter.notifyDataSetChanged()

                        // select the first image automatically
                        Glide.with(this@MainActivity)
                                .load(images[0].imageLink)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(selected_image)

                        // fade it in with a delay
                        recycler_view.animate().alpha(1f).setStartDelay(400).start()
                    } else {
                        // notify of placeholder image use
                        adapter.notifyDataSetChanged()

                        // select the first image automatically
                        Glide.with(this@MainActivity)
                                .load(LionheartRecyclerViewAdapter.placeholderImages[0])
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .into(selected_image)
                    }
                })
    }

    /**
     * Helper fun to set up swipe to refresh functionality.
     * Swipe refresh will call the api no matter what, even if
     * there are images in the database
     */
    private fun swipeToRefresh() {
        viewModel.refreshImages()
    }

    /**
     * init recycler view, the image list and set the on click
     */
    private fun setUpRecyclerView() {
        // init images list
        images = mutableListOf()
        // init the layout to a grid
        recycler_view.layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.column_span))
        // set the adapter with an onclick for the items
        adapter = LionheartRecyclerViewAdapter(this, images, listener = {image, placeholder ->
            // send the clicked image through the edit activity
            Glide.with(this@MainActivity)
                    .load(if (images.isNotEmpty()) image.imageLink else placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(selected_image)
        })
        recycler_view.adapter = adapter
    }

    /**
     * Set the toolbar icons actions, next and share and return
     */
    private fun setToolbarActions() {

        // if share action window is open, hide the log in return action
        when (main_toolbar_next_action.text) {
            // return button is properly hidden or shown
            getString(R.string.share_action) -> {
                log_in_return_action.visibility = View.GONE
                return_selection_action.visibility = View.VISIBLE
            }
        }

        log_in_return_action.setOnClickListener {
            finishAfterTransition()
        }

        main_toolbar_next_action.setOnClickListener {
            when (main_toolbar_next_action.text) {
                // next action button
                getString(R.string.next_Action) -> {
                    // set up the transition mgr to do anims
                    TransitionManager.beginDelayedTransition(main_transitions_container)
                    return_selection_action.visibility = View.VISIBLE
                    // hide buttons
                    thug_glasses_button_container.visibility = View.VISIBLE
                    fire_emoji_button_container.visibility = View.VISIBLE
                    select_info.visibility = View.VISIBLE
                    // disable the swipe refresh
                    swipe_refresh.isEnabled = false
                    main_toolbar_title.visibility = View.GONE
                    recycler_view.visibility = View.GONE
                    log_in_return_action.visibility = View.GONE
                    main_toolbar_next_action.text = getString(R.string.share_action)
                }
                // share
                getString(R.string.share_action) -> {
                    log_in_return_action.visibility = View.GONE
                    return_selection_action.visibility = View.VISIBLE
                    shareImageAction()
                }
            }

            // return icon on toolbar action to go back to image selection
            return_selection_action.setOnClickListener {
                // set up the transition mgr to do anims
                TransitionManager.beginDelayedTransition(main_transitions_container)
                recycler_view.visibility = View.VISIBLE
                log_in_return_action.visibility = View.VISIBLE
                main_toolbar_title.visibility = View.VISIBLE
                // if it was the fire emoji, hide progress bar
                progress_bar_fire.animate().alpha(0f).start()
                progress_bar_thug.animate().alpha(0f).start()
                // enable the swipe refresh
                swipe_refresh.isEnabled = true
                // hide buttons
                thug_glasses_button_container.visibility = View.GONE
                fire_emoji_button_container.visibility = View.GONE
                select_info.visibility = View.GONE

                main_toolbar_next_action.text = getString(R.string.next_Action)
                // hide the return action
                return_selection_action.visibility = View.GONE

                // remove all the emojis on the image
                deleteFireEmojis()
                deleteThugEmojis()

                // Set the image views to gray scale
                val matrix = ColorMatrix()
                matrix.setSaturation(1f)
                val filter = ColorMatrixColorFilter(matrix)
                selected_image.colorFilter = filter
                }
            }
    }

    /**
     * Helper fun to share the image through a choose. Bitmap is compressed and
     * temp saved to pass the uri
     */
    private fun shareImageAction() {
        // enable the view to be cached
        selection_container.isDrawingCacheEnabled = true
        selection_container.buildDrawingCache()
        // delete cache after bitmap is init
        selection_container.destroyDrawingCache()

        // create the bitmap
        val bitmap = Bitmap.createBitmap(selection_container.drawingCache)

        // save it into a file
        // save bitmap to cache directory
        try {
            val cachePath = File(this.cacheDir, "images")
            cachePath.mkdirs() // don't forget to make the directory
            // overwrites this image every time
            val stream = FileOutputStream(cachePath.path + "/image.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // grab the image from cache
        val imagePath = File(cacheDir, "images")
        val newFile = File(imagePath, "image.png")
        val contentUri = FileProvider.getUriForFile(this, CACHE_AUTHORITY, newFile)

        if (contentUri != null) {
            // share action to launch a chooser and send the image
            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            //share.setDataAndType(contentUri, contentResolver.getType(contentUri))
            share.putExtra(Intent.EXTRA_STREAM, contentUri)
            startActivity(Intent.createChooser(share, "Share Image"))
        }
    }

    /**
     * Helper fun to delete fire emojis from image
     */
    private fun deleteFireEmojis() {
        // get the n of emojis
        val numberOfFireEmojis = selection_container.childCount - 1
        // find the view through the tag and remove it
        for (i in 0 until numberOfFireEmojis) {
            val currentLeftTag = "fire_emoji_left_" + i.toString()
            val currentLeftView: ImageView? = selection_container.findViewWithTag<ImageView>(currentLeftTag)
            if (currentLeftView != null) {
                currentLeftView.visibility = View.GONE
                selection_container.removeView(currentLeftView)
            }
            val currentRightTag = "fire_emoji_right_" + i.toString()
            val currentRightView: ImageView? = selection_container.findViewWithTag<ImageView>(currentRightTag)
            if (currentRightView != null) {
                currentRightView.visibility = View.GONE
                selection_container.removeView(currentRightView)
            }
        }
         selection_container.invalidate()
    }

    /**
     * Helper fun to delete thug emojis from image
     */
    private fun deleteThugEmojis() {
        // get the n of emojis
        val numberOfThugGlasses = selection_container.childCount - 1
        // find the view through the tag and remove it
        for (i in 0 until numberOfThugGlasses) {
            val currentLeftTag = "thug_" + i.toString()
            val currentLeftView: ImageView? = selection_container.findViewWithTag(currentLeftTag)
            if (currentLeftView != null) {
                currentLeftView.visibility = View.GONE
                selection_container.removeView(currentLeftView)
            }
            val currentRightTag = "thug_joint" + i.toString()
            val currentRightView: ImageView? = selection_container.findViewWithTag(currentRightTag)
            if (currentRightView != null) {
                currentRightView.visibility = View.GONE
                selection_container.removeView(currentRightView)
            }
            val currentText = "thug_text" + i.toString()
            val currentTextView: ImageView? = selection_container.findViewWithTag(currentText)
            if (currentTextView != null) {
                currentTextView.visibility = View.GONE
                selection_container.removeView(currentTextView)
            }
        }
        selection_container.invalidate()
    }

    /**
     * Helper fun to set the emoji on click to carry the face detection
     */
    private fun setFireEmojiButton() {
        fire_emoji_button_container.setOnClickListener {
            // do an small alpha animation with the on click
            fire_emoji_button_container.animate().alpha(0.5f).setDuration(500).setListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                // once finish return alpha to 1
                override fun onAnimationEnd(p0: Animator?) {
                    fire_emoji_button_container.animate().alpha(1.0f).setDuration(500).start()
                }

                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {}
            }).start()
            // show progress loader
            with(progress_bar_fire) {
                visibility = View.VISIBLE
                alpha = 0f
                animate().alpha(1f).start()
                animate().alpha(0f).setDuration(6000).start()
            }
            // run the detector
            runFaceDetector(FIRE_EMOJI)
        }
    }

    /**
     * Helper fun to set the thug emoji on click to carry the face detection
     */
    private fun setThugEmojiButton() {
        thug_glasses_button_container.setOnClickListener {
            // do an small alpha animation with the on click
            thug_glasses_button_container.animate().alpha(0.5f).setDuration(500).setListener(object: Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {}

                // once finish return alpha to 1
                override fun onAnimationEnd(p0: Animator?) {
                    thug_glasses_button_container.animate().alpha(1.0f).setDuration(500).start()
                }

                override fun onAnimationCancel(p0: Animator?) {}
                override fun onAnimationStart(p0: Animator?) {}
            }).start()
            // show progress loader
            with(progress_bar_thug) {
                visibility = View.VISIBLE
                alpha = 0f
                animate().alpha(1f).start()
                // after a timeout hide the progress bar
                animate().alpha(0f).setDuration(6000).start()

            }
            // run the detector
            runFaceDetector(THUG_EMOJI)
        }
    }

    /**
     * Helper fun to set up ml kit and face detection
     */
    private fun runFaceDetector(whatButton: String) {
        // set options
        val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setModeType(FirebaseVisionFaceDetectorOptions.ACCURATE_MODE)
                .setLandmarkType(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .setClassificationType(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .setMinFaceSize(0.15f)
                .build()

        // enable the image to be cached
        selected_image.isDrawingCacheEnabled = true
        selected_image.buildDrawingCache()
        // create a bitmap from the cache
        selectedBitmap = Bitmap.createBitmap(selected_image.drawingCache)
        // delete cache after bitmap is init
        selected_image.destroyDrawingCache()
        val firebaseImage = FirebaseVisionImage.fromBitmap(selectedBitmap!!)
        val detector = FirebaseVision.getInstance().getVisionFaceDetector(options)

        // detect faces
        detector.detectInImage(firebaseImage)
                .addOnSuccessListener { faces ->
                    // if there are no faces show a snackbar
                    if (faces.isEmpty()) {
                        val snack = Snackbar.make(main_transitions_container,
                                getString(R.string.no_humans), Snackbar.LENGTH_LONG)
                        snack.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                        snack.show()
                    }

                    // on successful detection, first delete any left over emojis
                    deleteFireEmojis()
                    deleteThugEmojis()

                    // Set the image views to gray scale
                    val matrix = ColorMatrix()
                    matrix.setSaturation(1f)
                    val filter = ColorMatrixColorFilter(matrix)
                    selected_image.colorFilter = filter

                    // init a smile probability
                    var smileProbability = 0f

                    // iterate through the faces
                    for ((index, face) in faces.withIndex()) {
                        // get smile probability
                        smileProbability = face.smilingProbability
                        // set emojis
                        setEmojisFromFace(index, face, whatButton)
                    }

                    // after iterating through the faces
                    // show probability snackbar
                    if (whatButton == FIRE_EMOJI) {
                        // Set the image view to slight gray scale
                        val matrix = ColorMatrix()
                        matrix.setSaturation(0.8f)
                        val filter = ColorMatrixColorFilter(matrix)
                        selected_image.colorFilter = filter
                        // launch a snackbar showing smile probability
                        if (smileProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                            val length = smileProbability.toString().length
                            if (length > 3) {
                                val snack = Snackbar.make(main_transitions_container, getString(R.string.smile_probability,
                                        smileProbability.toString().substring(0..3)), Snackbar.LENGTH_LONG)
                                snack.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                                snack.show()
                            } else {
                                val snack = Snackbar.make(main_transitions_container,
                                        getString(R.string.no_humans), Snackbar.LENGTH_LONG)
                                snack.view.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimaryDark))
                                snack.show()
                            }
                        }
                    } else {
                        // Set the image views to gray scale
                        val matrix = ColorMatrix()
                        matrix.setSaturation(0f)
                        val filter = ColorMatrixColorFilter(matrix)
                        selected_image.colorFilter = filter
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FIREBASE", exception.toString())
                }.addOnCompleteListener{ it ->
                    progress_bar_fire.animate().alpha(0f).start()
                    progress_bar_thug.animate().alpha(0f).start()
                    Log.e("FIREBASE", it.toString())
                }
    }

    private fun setEmojisFromFace(index: Int, face: FirebaseVisionFace, whatButton: String) {

        // on success grab the landmarks
        val leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EYE)
        val mouth = face.getLandmark(FirebaseVisionFaceLandmark.BOTTOM_MOUTH)
        var angleY = face.headEulerAngleY
        if (angleY < 0) {
            angleY += 360
        }

        // check what button was used to run the face detector
        /*
        Fire emoji logic
         */
        if (whatButton == FIRE_EMOJI) {
            // if it was the fire emoji, hide progress bar
            progress_bar_fire.animate().alpha(0f).start()

            if (leftEye != null && rightEye != null) {
                // get the positions relative to the image
                val leftEyePos = leftEye.position
                val rightEyePos = rightEye.position
                val diff = leftEyePos.x - rightEyePos.x

                /*
                / Left eye
                 */
                // create a new imageview to make the left eye emojis
                val fireLeftEmoji = ImageView(this@MainActivity)
                selection_container.addView(fireLeftEmoji)
                with(fireLeftEmoji) {
                    // set properties
                    // set a tag to find later
                    tag = "fire_emoji_left_" + index.toString()
                    setImageDrawable(getDrawable(R.drawable.ic_fire_emoji))
                    //layoutParams.height = resources.getDimension(R.dimen.return_text_bottom_margin).toInt()
                    //layoutParams.width = resources.getDimension(R.dimen.return_text_bottom_margin).toInt()
                    layoutParams.height = diff.toInt()
                    layoutParams.width = diff.toInt()
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    // set the rotation according to the face detection
                    rotation = angleY
                    alpha = 0f
                    // show and set positions. Add a small margin to lower the emoji more
                    animate().alpha(1f).start()
                    x = leftEyePos.x - layoutParams.width.div(2)
                    y = leftEyePos.y - layoutParams.height +
                            resources.getDimension(R.dimen.grid_margin).times(1.5f)
                }

                /*
                 / Right eye
                 */
                // create an imageview to set the right eye emojis
                val fireRightEmoji = ImageView(this@MainActivity)
                selection_container.addView(fireRightEmoji)
                with(fireRightEmoji) {
                    // set properties
                    // set a tag to find later
                    tag = "fire_emoji_right_" + index.toString()
                    setImageDrawable(getDrawable(R.drawable.ic_fire_emoji))
                    layoutParams.height = diff.toInt()
                    layoutParams.width = diff.toInt()
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    // set the rotation according to the face detection
                    rotation = angleY
                    Log.e("ROTATION", angleY.toString())
                    alpha = 0f

                    // show and set positions Add a small margin to lower the emoji more
                    animate().alpha(1f).start()
                    x = rightEyePos.x - layoutParams.width.div(2)
                    y = rightEyePos.y - layoutParams.height +
                            resources.getDimension(R.dimen.grid_margin).times(1.5f)
                }
            }
            /*
            Thug glasses emoji logic
             */
        } else if (whatButton == THUG_EMOJI) {
            // if it was the thug emoji, hide progress bar
            progress_bar_thug.animate().alpha(0f).start()

            // if eyes are detected, start glass animation
            if (leftEye != null && rightEye != null && mouth != null) {
                // get the position
                val leftEyePos = leftEye.position
                val rightEyePos = rightEye.position

                val diff = leftEyePos.x - rightEyePos.x

                /*
                / glasses
                 */
                // create a new imageview to make the glass emojis
                val glassEmoji = ImageView(this@MainActivity)
                selection_container.addView(glassEmoji)
                with(glassEmoji) {
                    // set properties
                    // set a tag to find later
                    tag = "thug_" + index.toString()
                    setImageDrawable(getDrawable(R.drawable.ic_thug_glasses))
                    layoutParams.height = diff.toInt().times(4)
                    layoutParams.width = diff.toInt().times(4)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    alpha = 0f
                    rotation = angleY
                    x = 0f - layoutParams.width
                    y = leftEyePos.y - layoutParams.height.div(2)

                    // show and set positions. Add a small margin to lower the emoji more
                    animate().alpha(1f).start()
                    animate().x(rightEyePos.x - layoutParams.width.div(2))
                            .setDuration(800).start()
                }
                /*
                 / joint
                 */
                // create a new imageview to make the joint emojis
                val jointEmoji = ImageView(this@MainActivity)
                selection_container.addView(jointEmoji)
                with(jointEmoji) {
                    // set properties
                    // set a tag to find later
                    tag = "thug_joint" + index.toString()
                    setImageDrawable(getDrawable(R.drawable.thug_cig1))
                    layoutParams.height = resources.getDimension(R.dimen.cig_size).toInt()
                    layoutParams.width = resources.getDimension(R.dimen.cig_size).toInt()
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    alpha = 0f
                    rotation = angleY
                    x = 0f
                    y = mouth.position?.y!!.toFloat() - jointEmoji.layoutParams.height.div(2)

                    // show and set positions. Add a small margin to lower the emoji more
                    animate().alpha(1f).start()
                    animate().x(mouth.position!!.x - layoutParams.width).setDuration(800).start()
                }

                /*
                / text
                 */
                // create a new imageview to make the text emojis
                val textEmoji = ImageView(this@MainActivity)
                selection_container.addView(textEmoji)
                with(textEmoji) {
                    // set properties
                    // set a tag to find later
                    tag = "thug_text" + index.toString()
                    setImageDrawable(getDrawable(R.drawable.thug_life_text))
                    layoutParams.height = resources.getDimension(R.dimen.thug_life_text).toInt()
                    layoutParams.width = resources.getDimension(R.dimen.thug_life_text).toInt()
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    alpha = 0f
                    x = 0f
                    y = 0 + resources.getDimension(R.dimen.general_margin_dimen)

                    // show and set positions. Add a small margin to lower the emoji more
                    animate().alpha(1f).start()
                    animate().x(resources.getDimension(R.dimen.general_margin_dimen)).setDuration(800).start()
                }
            }
        }
    }

    /**
     * Helper fun to check fb token is current
     */
    private fun isFBLoggedIn(): Boolean {
        val accessToken = AccessToken.getCurrentAccessToken()
        return accessToken != null && !accessToken.isExpired
    }

    override fun onBackPressed() {
        when (thug_glasses_button_container.visibility) {
            View.VISIBLE -> {
                // set up the transition mgr to do anims
                TransitionManager.beginDelayedTransition(main_transitions_container)
                recycler_view.visibility = View.VISIBLE
                log_in_return_action.visibility = View.VISIBLE
                main_toolbar_title.visibility = View.VISIBLE
                // if it was the fire emoji, hide progress bar
                progress_bar_fire.animate().alpha(0f).start()
                progress_bar_thug.animate().alpha(0f).start()
                // enable the swipe refresh
                swipe_refresh.isEnabled = true
                // hide buttons
                thug_glasses_button_container.visibility = View.GONE
                fire_emoji_button_container.visibility = View.GONE
                select_info.visibility = View.GONE

                main_toolbar_next_action.text = getString(R.string.next_Action)
                // hide the return action
                return_selection_action.visibility = View.GONE

                // remove all the emojis on the image
                deleteFireEmojis()
                deleteThugEmojis()

                // Set the image views to gray scale
                val matrix = ColorMatrix()
                matrix.setSaturation(1f)
                val filter = ColorMatrixColorFilter(matrix)
                selected_image.colorFilter = filter
            }
            View.GONE -> finishAfterTransition()
        }
        super.onBackPressed()
    }
}
