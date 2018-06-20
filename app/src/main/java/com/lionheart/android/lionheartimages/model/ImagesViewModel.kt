package com.lionheart.android.lionheartimages.model

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import com.lionheart.android.lionheartimages.pojo.LionheartImage
import com.lionheart.android.lionheartimages.repos.ImageRepository

/**
 * ViewModel provides the data for the main activity and handles the communication with the
 * business part of data handling. The ViewModel does not know about the View and is not affected
 * by configuration changes such as recreating an activity due to rotation.
 */
class ImagesViewModel : ViewModel() {

    /*
     / global variables & constants
    */
    private var images: LiveData<List<LionheartImage>>? = null
    private lateinit var imageRepo: ImageRepository

    /*
     /  functions
    */

    /**
     * init function to get instance of the repo. The function returns 'this' to chain methods
     * and get the livedata instance from the repo for the activity to observe it
     */
    fun init(context: Context, vararg queryParams: Pair<String, String>): ImagesViewModel {
        imageRepo = ImageRepository.getInstance(context, *queryParams)
        return this
    }

    fun getImages() = imageRepo.getImages()
}