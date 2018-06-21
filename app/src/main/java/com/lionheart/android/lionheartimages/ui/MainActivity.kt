package com.lionheart.android.lionheartimages.ui

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
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Environment
import android.util.DisplayMetrics
import android.util.SparseArray
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
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
    private val START_UP_IS_SHOWN = "com.lionheart.android.lionheartimages.START_UP_IS_SHOWN"
    // lateinit
    private lateinit var viewModel: ImagesViewModel
    // init a fb accessToken, with nope so its non-null
    private var accessToken = "nope"
    private lateinit var adapter: LionheartRecyclerViewAdapter
    private lateinit var images: MutableList<LionheartImage>
    // executor service for database operations
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
        fade.startDelay = 500
        window.enterTransition = fade
        setContentView(R.layout.activity_main)

        // check if fb is logged in and grab the accesToken
        if (isFBLoggedIn()) {
            accessToken = AccessToken.getCurrentAccessToken().token
        }

        // load images and observe the livedata
        observeViewModel()

        // set a recycler view
        setUpRecyclerView()

        setNextAction()

        setFireButton()
        setEmojiButton()
    }

    private fun setNextAction() {
        main_toolbar_next_action.setOnClickListener {
            when (main_toolbar_next_action.text) {
                getString(R.string.next_Action) -> {
                    // set up the transition mgr to do anims
                    TransitionManager.beginDelayedTransition(main_transitions_container)
                    recycler_view.visibility = View.GONE
                    fire_action_button.visibility = View.VISIBLE
                    fire_emoji_button.visibility = View.VISIBLE
                    main_toolbar_title.visibility = View.GONE
                    return_selection_action.visibility = View.VISIBLE
                    main_toolbar_next_action.text = getString(R.string.share_action)
                    select_info.visibility = View.VISIBLE
                }
                getString(R.string.share_action) -> {
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

            return_selection_action.setOnClickListener {
                // set up the transition mgr to do anims
                TransitionManager.beginDelayedTransition(main_transitions_container)
                recycler_view.visibility = View.VISIBLE
                fire_action_button.visibility = View.GONE
                fire_emoji_button.visibility = View.GONE
                fire_emoji_eyes.visibility = View.GONE
                fire_emoji_eyes_2.visibility = View.GONE
                main_toolbar_next_action.text = getString(R.string.next_Action)
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
     * init recycler view, the image list and set the on click
     */
    private fun setUpRecyclerView() {
        // init images list
        images = mutableListOf()
        // init the layout to a grid
        recycler_view.layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.column_span))
        //
        adapter = LionheartRecyclerViewAdapter(this, images, listener = {image ->
            // send the clicked image through the edit activity
            Glide.with(this@MainActivity)
                    .load(image.imageLink)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(selected_image)
        })
        recycler_view.adapter = adapter
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
                Pair("fields", "photos.limit(15){height,id,images,link,name,width}"),
                Pair("access_token", accessToken))
                .getImages()?.observe(this, Observer { imagesData ->
                    // when the livedata is updated from the database
                    // the result is called through here
            // TODO fill list with dummies if it is null
            if (imagesData != null && imagesData.isNotEmpty()) {
                // clear the list
                images.clear()
                // add the observed image data to the list handled by the adapter
                images.addAll(imagesData)
                adapter.notifyDataSetChanged()

                // select the first image automatically
                if (selected_image.drawable != null) {
                    Glide.with(this@MainActivity)
                            .load(images[0].imageLink)
                            .transition(DrawableTransitionOptions.withCrossFade())
                            .into(selected_image)
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
            View.GONE -> finish()
        }
        super.onBackPressed()
    }
}
