package com.lionheart.android.lionheartimages.repos

import android.arch.lifecycle.LiveData
import android.content.Context
import android.net.Uri
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.lionheart.android.lionheartimages.database.ImageDao
import com.lionheart.android.lionheartimages.database.ImageDatabase
import com.lionheart.android.lionheartimages.pojo.LionheartImage
import org.json.JSONObject
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException

/**
 * Repository module responsible for handling data operations. Provides a clean API
 * to the rest of the app. Knows where to get the data from and what API calls to make when
 * data is updated. Mediates between different data sources (persistent
 * model, web service, etc.).
 */
class ImageRepository private constructor(private val queryParamsFB: Map<String, String>,
                                          private val queryParamsInsta: Map<String, String>) {

    /*
     / global variables & constants
     */
     companion object {
         // init a queue and dao objects
         private var queue: RequestQueue? = null
         private var imageDao: ImageDao? = null

         // instance of repo to hold
         private var INSTANCE: ImageRepository? = null

         // executor service for database operations
         private var executor: ExecutorService? = null

         /**
          * instance singleton function handling queue and database objects creation
          */
         fun getInstance(context: Context,
                         queryParamsFB: Map<String, String>,
                         queryParamsInsta: Map<String, String>): ImageRepository {
             // check if queue exists already
             queue = queue ?: Volley.newRequestQueue(context)
             // instance of db dao
             imageDao = imageDao ?: ImageDatabase.getInstance(context).imageDao()
             // executor for db operations on worker thread
             executor = executor ?: Executors.newSingleThreadExecutor()
             // return instance of repo if not created yet, synchronized with threads
             return INSTANCE ?: synchronized(this) {
                 INSTANCE ?: ImageRepository(queryParamsFB, queryParamsInsta)
             }
         }
     }

    private val hosts = object {
        // host for any access to user data
        val hostFBUrl = "https://graph.facebook.com/v3.0/me"
        // grabs the most recent user uploads
        val hostInstaUrl = "https://api.instagram.com/v1/users/self/media/recent/"
    }

    /*
     / functions
    */

    /**
     * Main function to grab the images from the database. Refreshes db from the web if necessary.
     */
    fun getImages(): LiveData<List<LionheartImage>>? {
        refreshImages()
        // return a LiveData directly from the database.
        return imageDao?.getAll()
    }

    /**
     * direct to api call function for user initiated refresh
     */
    fun directAPICall(): LiveData<List<LionheartImage>>? {
        // delete images and call the api directly
        deleteImages()
        // return the dao livedata
        return imageDao?.getAll()
    }

    /**
     * Check whether database has images saved or needs to fetched from the internet.
     */
    private fun refreshImages() {
        var isEmpty: Boolean? = true
        // init the future to grab the executor task
        var future: Future<Boolean>? = null
        // submit a task and assign to the future
        try {
            future = executor?.submit<Boolean> {
                // check if the db is empty before making api call
                val list = imageDao?.getIds()
                return@submit list?.size == 0
            }
        } catch (e: RejectedExecutionException) {
            Log.e("REPO_GETIDS", e.toString())
        }
        // run the future and see if database does not hold images
        isEmpty = try {
            future?.get()
        } catch (e: Exception) {
            Log.e("REPO_GETIDS", e.toString())
            false
        }
        // if db is empty do api call
        if (isEmpty!!) {
            fetchImagesFromAPI()
        }
    }

    /**
     * Check whether database has images saved or needs to fetched from the internet.
     */
    private fun deleteImages() {
        // init the future to grab the executor task
        var future: Future<Unit>? = null
        // submit a task and assign to the future
        try {
            future = executor?.submit<Unit> {
                // delete all the images
                imageDao?.deleteAll()
                fetchImagesFromAPI()
            }
        } catch (e: RejectedExecutionException) {
            Log.e("REPO_GETIDS", e.toString())
        }
        // run the future and delete images
        try {
            future?.get()
        } catch (e: Exception) {
            Log.e("REPO_GETIDS", e.toString())
        }

    }

    /**
     * API call function fetching images from a url
     */
    private fun fetchImagesFromAPI() {
        // if fb token exists
        // pass the query params to build the url and start a request
        if (queryParamsFB["access_token"] != "nope") {
            addFBVolleyRequest(buildURLQuery(queryParamsFB))
        }
        // if insta token exists
        // add another request to the queue for instagram
        if (queryParamsInsta["access_token"] != "nope") {
            addInstaVolleyRequest(buildURLQuery(queryParamsInsta))
        }
    }

    /**
     * Add volley request to the queue and receive JSON response.
     */
    private fun addFBVolleyRequest(url: String) {
        // create the json request
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    // if response is good parse json and save images to database
                    val images = parseFBImageListJson(response)
                    saveImagesToDatabase(images)
                },
                Response.ErrorListener { error ->
                    //TODO handle error
                    Log.e("JSONTAG", "Response: %s".format(error.toString()))
                })

        // add it to the queue
        queue?.add(jsonObjectRequest)
    }

    private fun addInstaVolleyRequest(url: String) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    // handle insta response
                    Log.e("JSONTAG", "Response: %s".format(response.toString()))
                    val images = parseInstaImageListJson(response)
                    saveImagesToDatabase(images)
                },
                Response.ErrorListener { error ->
                // handle the error
                    Log.e("JSONTAG", "Response: %s".format(error.toString()))
                })

        // add it to the queue
        queue?.add(jsonObjectRequest)
    }

    /**
     * Save the image data to the database
     */
    private fun saveImagesToDatabase(images: List<LionheartImage>) {
        // init the future to grab the executor task
        var future: Future<Unit>? = null
        // submit a task and assign to the future
        try {
            future = executor?.submit<Unit> {
                // save the new images
                imageDao?.saveAll(*images.toTypedArray())
            }
        } catch (e: RejectedExecutionException) {
            Log.e("REPO_SAVEALL", e.toString())
        }

        // run the future and save to database
        try {
            future?.get()
        } catch (e: Exception) {
            Log.e("REPO_SAVEALL", e.toString())
        }
    }

    /**
     * Helper fun to parse json response from fb api call and
     * returning the list of image pojos to save into the database
     */
    private fun parseFBImageListJson(response: JSONObject): List<LionheartImage> {
        // init a list to add objects
        val images = mutableListOf<LionheartImage>()

        // grab the photos object
        val photoObject = response.getJSONObject("photos")
        // grab the data array with the image list
        val imageJsonArray = photoObject.getJSONArray("data")

        // iterate through the array
        for (i in 0 until imageJsonArray.length()) {
            // grab the current image
            val image: JSONObject = imageJsonArray[i] as JSONObject

            // grab the array of thumbnail objects
            val thumbnails = image.getJSONArray("images")

            // grab the second image for a phone sized image
            val imageJson = thumbnails[1] as JSONObject
            val imageLink = imageJson.getString("source")
            // get the one before last which seems to be the smallest
            // for some reason
            val thumbnailIndex = thumbnails.length() - 1
            val thumbnailJson = thumbnails[thumbnailIndex] as JSONObject
            val thumbnailLink = thumbnailJson.getString("source")

            // grab the data from the image and create the image pojo
            val imagePojo = image.let {
                LionheartImage(it.getInt("id"), imageLink,
                        it.getInt("height"), it.getInt("width"), thumbnailLink)
            }

            // add pojo into list
            images.add(imagePojo)
        }

        // return the list once populated
        return images
    }

    /**
     * Helper fun to parse json response from fb api call and
     * returning the list of image pojos to save into the database
     */
    private fun parseInstaImageListJson(response: JSONObject): List<LionheartImage> {
        // init a list to add objects
        val images = mutableListOf<LionheartImage>()

        // grab the photos object
        val dataJsonArray = response.getJSONArray("data")
        for (i in 0 until dataJsonArray.length()) {
            val currentImage = dataJsonArray[i] as JSONObject

            // grab properties
            val id = currentImage.getString("id")
            val imagesJsonObject = currentImage.getJSONObject("images")
            if (imagesJsonObject != null) {
                val thumbnailJsonObject = imagesJsonObject.getJSONObject("thumbnail")
                val thumbnailUrl = thumbnailJsonObject.getString("url")

                val standardImagejsonObject = imagesJsonObject.getJSONObject("standard_resolution")

                // init a image pojo
                val imagePojo = standardImagejsonObject.let { LionheartImage(
                        id.dropLast(22).toInt(), it.getString("url"), it.getInt("height"),
                        it.getInt("width"), thumbnailUrl) }
                // add it to the list
                images.add(imagePojo)
            } else {
                images.add(LionheartImage(-1, "", 0, 0, ""))
            }
        }

        // return the list
        return images
    }

    /**
     * Build the url query from a number of query parameters input
     */
    private fun buildURLQuery(queryParams: Map<String, String>): String {
        // init the builder
        val builder = Uri.Builder()

        // add the host path
        if (queryParams.containsKey("fields")) {
            // use fb host
            builder.encodedPath(hosts.hostFBUrl)
        } else {
            // use insta host
            builder.encodedPath(hosts.hostInstaUrl)
        }

        // build the query by adding the parameters given
        for (queryParam in queryParams) {
            builder.appendQueryParameter(queryParam.key, Uri.decode(queryParam.value))
        }

        // build the url
        return builder.build().toString()
    }

}