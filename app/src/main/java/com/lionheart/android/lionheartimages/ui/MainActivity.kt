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
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.face.FaceDetector
import com.lionheart.android.lionheartimages.R
import com.lionheart.android.lionheartimages.model.ImagesViewModel
import com.lionheart.android.lionheartimages.pojo.LionheartImage
import kotlinx.android.synthetic.main.activity_main.*
import android.net.Uri
import android.os.Environment
import android.transition.Slide
import android.support.v4.content.ContextCompat
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.Gravity
import com.google.android.gms.vision.face.Face
import com.google.android.gms.vision.face.Landmark
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

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

    // recycler view adapter and list data
    private lateinit var adapter: LionheartRecyclerViewAdapter
    private lateinit var images: MutableList<LionheartImage>

    // executor service for bitmap ops
    private var executor: ExecutorService? = Executors.newSingleThreadExecutor()
    var selectedBitmap: Bitmap? = null

    /*
    / functions
    */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // set the transition anim
        val fade = Fade()
        fade.duration = 500
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

        // load images and observe the live data
        observeViewModel()

        // set a recycler view
        setUpRecyclerView()

        // show progress bar
        with(progress_bar) {
            visibility = View.VISIBLE
            background = ContextCompat.getDrawable(this@MainActivity,
                    R.color.colorBackgroundWhite)
        }

        // set buttons
        setActions()
        setFireButton()
        setEmojiButton()
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
                    // when the livedata is updated from the database
                    // the result is called through here
                    // hide progress bar
                    with(progress_bar) {
                        visibility = View.INVISIBLE
                        background = ContextCompat.getDrawable(this@MainActivity,
                                android.R.color.transparent)
                    }
                    if (imagesData != null && imagesData.isNotEmpty()) {
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
                        recycler_view.animate().alpha(1f).setStartDelay(400).start()
                    } else {
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
    private fun setActions() {
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
                    // rotate the icon
                    return_selection_action.animate().setDuration(600).rotation(360f)
                    fire_action_button.visibility = View.VISIBLE
                    fire_emoji_button.visibility = View.VISIBLE
                    select_info.visibility = View.VISIBLE
                    main_toolbar_title.visibility = View.GONE
                    recycler_view.visibility = View.GONE
                    log_in_return_action.visibility = View.GONE
                    main_toolbar_next_action.text = getString(R.string.share_action)
                }
                // share
                getString(R.string.share_action) -> {
                    // share action to launch a chooser and send the image
                    val share = Intent(Intent.ACTION_SEND)
                    share.type = "image/jpeg"
                    val bytes = ByteArrayOutputStream()
                    selectedBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_fire_emoji)
                    val bitmap = selectedBitmap
                    bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                    val f = File(Environment.getExternalStorageDirectory().path + File.separator + "temporary_file.jpg")
                    try {
                        f.createNewFile()
                        val fo = FileOutputStream(f)
                        fo.write(bytes.toByteArray())
                        fo.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    share.putExtra(Intent.EXTRA_STREAM, Uri.parse(Environment.getExternalStorageDirectory().path + File.separator + "temporary_file.jpg"))
                    startActivity(Intent.createChooser(share, "Share Image"))
                }
            }

            // return icon on toolbar action to go back to image selection
            return_selection_action.setOnClickListener {
                // set up the transition mgr to do anims
                TransitionManager.beginDelayedTransition(main_transitions_container)
                recycler_view.visibility = View.VISIBLE
                log_in_return_action.visibility = View.VISIBLE
                main_toolbar_title.visibility = View.VISIBLE
                fire_action_button.visibility = View.GONE
                fire_emoji_button.visibility = View.GONE
                fire_emoji_eyes.visibility = View.GONE
                fire_emoji_eyes_2.visibility = View.GONE
                main_toolbar_next_action.text = getString(R.string.next_Action)
                select_info.visibility = View.GONE
                return_selection_action.visibility = View.GONE

                thug_glasses.visibility = View.INVISIBLE
                thug_glasses.x = 0f - thug_glasses.width

                // Set the image views to gray scale
                val matrix = ColorMatrix()
                matrix.setSaturation(1f)
                val filter = ColorMatrixColorFilter(matrix)
                selected_image.colorFilter = filter
                }
            }
    }

    private fun setEmojiButton() {
        fire_emoji_button.setOnClickListener {
            // grab the screen info
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels

            var leftCx = 0
            var leftCy = 0
            var rightCx = 0
            var rightCy = 0

            val detector = FaceDetector.Builder(this)
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build()

            // init the future to grab the executor task
            var future: Future<Unit>? = null
            // submit a task and assign to the future
            future = executor?.submit<Unit> {
                val url = URL(images[0].imageLink)
                if (images != null && images.isNotEmpty()) {
                    selectedBitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())

                    val frame = Frame.Builder().setBitmap(selectedBitmap).build()
                    val faces: SparseArray<Face> = detector.detect(frame)
                    for (i in 0 until faces.size()) {
                        val face = faces[i]

                        if (face != null) {
                            for (landmark in face.landmarks) {
                                when (landmark.type) {
                                    Landmark.LEFT_EYE -> {
                                        leftCx = landmark.position.x.toInt()
                                        leftCy = landmark.position.y.toInt()
                                        Log.e("LEFT", leftCx.toString() + " " + leftCy.toString())
                                    }
                                    Landmark.RIGHT_EYE -> {
                                        rightCx = landmark.position.x.toInt()
                                        rightCy = landmark.position.y.toInt()
                                        Log.e("RIGHT", rightCx.toString() + " " + rightCx.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // run the future and save to database
            future?.get()
            if (future!!.isDone) {
                fire_emoji_eyes.visibility = View.VISIBLE
                fire_emoji_eyes_2.visibility = View.VISIBLE
                fire_emoji_eyes.y = selected_image.y + leftCy
                fire_emoji_eyes_2.y  = selected_image.y + rightCy

                // get the middle point of the two eyes
                fire_emoji_eyes.x =  selected_image.x + leftCx
                fire_emoji_eyes_2.x = selected_image.x + rightCx
                detector.release()
            }
        }
    }

    private fun setFireButton() {
        fire_action_button.setOnClickListener {
            // grab the screen info
            val metrics = DisplayMetrics()
            windowManager.defaultDisplay.getMetrics(metrics)
            val screenWidth = metrics.widthPixels

            var leftCx = 0
            var leftCy = 0
            var rightCx = 0
            var rightCy = 0

            var selectedBitmap: Bitmap? = null
            val detector = FaceDetector.Builder(this)
                    .setTrackingEnabled(false)
                    .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                    .build()

            // init the future to grab the executor task
            var future: Future<Unit>? = null
            // submit a task and assign to the future
            future = executor?.submit<Unit> {
                val url = URL(images[0].imageLink)
                if (images != null && images.isNotEmpty()) {
                    selectedBitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())

                    val frame = Frame.Builder().setBitmap(selectedBitmap).build()
                    val faces: SparseArray<Face> = detector.detect(frame)
                    for (i in 0 until faces.size()) {
                        val face = faces[i]

                        if (face != null) {
                            for (landmark in face.landmarks) {
                                when (landmark.type) {
                                    Landmark.LEFT_EYE -> {
                                        leftCx = landmark.position.x.toInt()
                                        leftCy = landmark.position.y.toInt()
                                        Log.e("LEFT", leftCx.toString() + " " + leftCy.toString())
                                    }
                                    Landmark.RIGHT_EYE -> {
                                        rightCx = landmark.position.x.toInt()
                                        rightCy = landmark.position.y.toInt()
                                        Log.e("RIGHT", rightCx.toString() + " " + rightCx.toString())
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // run the future and save to database
            future?.get()
            if (future!!.isDone) {
                thug_glasses.visibility = View.VISIBLE
                thug_glasses.y = selected_image.y + leftCy + (thug_glasses.height*1.5f)

                // get the middle point of the two eyes
                val thirdEye = leftCx - rightCx
                thug_glasses.animate().x(selected_image.x + thirdEye + leftCx - (thug_glasses.width/3)).setDuration(1000).start()
                // Set the image views to gray scale
                val matrix = ColorMatrix()
                matrix.setSaturation(0f)
                val filter = ColorMatrixColorFilter(matrix)
                selected_image.colorFilter = filter
                detector.release()
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

    /**
     * Helper fun to check if start up screen has been shown.
     */
    private fun checkStartUpScreen() {
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // grab the boolean with a false default
        val isStartUpShown = mSharedPreferences.getBoolean(WELCOME_SCREEN_SHOWN, false)

        when (isStartUpShown) {
            false -> {
                mSharedPreferences.edit().putBoolean(WELCOME_SCREEN_SHOWN, true).apply()
                // send the activity intent
                val intent = Intent(this, WelcomeScreenActivity::class.java)
                startActivity(intent)
            }
        }
    }

    override fun onBackPressed() {
        when (return_selection_action.visibility) {
            View.VISIBLE -> {
                // set up the transition mgr to do anims
                TransitionManager.beginDelayedTransition(main_transitions_container)
                recycler_view.visibility = View.VISIBLE
                fire_action_button.visibility = View.GONE
                fire_emoji_button.visibility = View.GONE
                fire_emoji_eyes.visibility = View.GONE
                fire_emoji_eyes_2.visibility = View.GONE
                select_info.visibility = View.GONE

                main_toolbar_title.visibility = View.VISIBLE
                return_selection_action.visibility = View.GONE

                thug_glasses.visibility = View.INVISIBLE
                thug_glasses.x = 0f - thug_glasses.width

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
