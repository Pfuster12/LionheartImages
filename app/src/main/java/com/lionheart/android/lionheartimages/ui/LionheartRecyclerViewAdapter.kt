package com.lionheart.android.lionheartimages.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.lionheart.android.lionheartimages.R

class LionheartRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    /*
     / global variables & constants
     */

    private val IMAGE_ITEM_TYPE = 101

    /*
    / functions
    */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            IMAGE_ITEM_TYPE -> ImageViewHolder(inflateViewHolder(R.layout.image_item, parent))
            else -> ImageViewHolder(inflateViewHolder(R.layout.image_item, parent))
        }

    /**
     * Helper fun to inflate view
     */
    private fun inflateViewHolder(layoutId: Int, parent: ViewGroup?): View {
        // return a layout inflater
        return LayoutInflater.from(context).inflate(layoutId, parent, false)
    }

    override fun getItemCount(): Int {
        // get item count
        return 1
    }

    override fun getItemViewType(position: Int): Int {
        return IMAGE_ITEM_TYPE
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // bind view holder
    }

    class ImageViewHolder(itemView: View) : ViewHolder(itemView), View.OnTouchListener {

        /**
         * override on touch to control the onclick and drag of the viewholder
         */
        override fun onTouch(view: View?, motionEvent: MotionEvent?): Boolean {
            // TODO on touch functionality.
            return when (motionEvent?.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    view?.performClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // TODO move func
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // TODO up func
                    true
                }
                else -> true
            }
        }
    }
}