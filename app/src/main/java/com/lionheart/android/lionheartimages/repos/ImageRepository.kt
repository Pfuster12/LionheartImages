package com.lionheart.android.lionheartimages.repos

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
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

/**
 * Repository module responsible for handling data operations. Provides a clean API
 * to the rest of the app. Knows where to get the data from and what API calls to make when
 * data is updated. Mediates between different data sources (persistent
 * model, web service, etc.).
 */
class ImageRepository private constructor(private vararg val queryParams: Pair<String, String>) {

    /*
     / global variables & constants
     */
    private val hostURL = "https://graph.facebook.com/v3.0/me"

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
         fun getInstance(context: Context, vararg queryParams: Pair<String, String>): ImageRepository {
             // check if queue exists already
             queue = queue ?: Volley.newRequestQueue(context)
             imageDao = imageDao ?: ImageDatabase.getInstance(context).imageDao()
             executor = executor ?: Executors.newSingleThreadExecutor()
             return INSTANCE ?: synchronized(this) {
                 INSTANCE ?: ImageRepository(*queryParams)
             }
         }
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
     * Check whether database has images saved or needs to re-fetch from the internet.
     */
    private fun refreshImages() {
        //val hasImages = imageDao?.hasImages() == true
        fetchImagesFromAPI()
        /*if (!hasImages) {
            // refresh the data through a volley call
            fetchImagesFromAPI()
        }*/
    }

    /**
     * API call function fetching images from a url
     */
    private fun fetchImagesFromAPI() {
        // pass the varargs to the function with the spread operator '*'
        addVolleyRequest(buildURLQuery(*queryParams))
    }

    /**
     * Add volley request to the queue and receive JSON response.
     */
    private fun addVolleyRequest(url: String) {
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                Response.Listener { response ->
                    saveImagesToDatabase(response)
                },
                Response.ErrorListener { error ->
                    //TODO handle error
                    Log.e("JSONTAG", "Response: %s".format(error.toString()))
                })

        queue?.add(jsonObjectRequest)
    }

    private fun saveImagesToDatabase(response: JSONObject) {
        // init the future to grab the executor task
        var future: Future<Unit>? = null
        // submit a task and assign to the future
        future = executor?.submit<Unit> {
            val rows: List<Long>? = imageDao?.saveAll(LionheartImage(0, response.toString(),
                    0, 0, "No"))
        }

        // run the future and save to database
        future?.get()
    }

    /**
     * Build the url query from a number of query parameters input
     */
    private fun buildURLQuery(vararg queryParams: Pair<String, String>): String {
        val builder = Uri.Builder()

        // add the host path
        builder.encodedPath(hostURL)

        // build the query by adding the parameters given
        for (queryParam in queryParams) {
            builder.appendQueryParameter(queryParam.first, queryParam.second)
        }

        // build the url
        return builder.build().toString()
    }

}