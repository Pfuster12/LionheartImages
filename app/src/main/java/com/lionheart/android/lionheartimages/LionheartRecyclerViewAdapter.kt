package com.lionheart.android.lionheartimages

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LionheartRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    private val IMAGE_ITEM_TYPE = 101

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

    class ImageViewHolder(itemView: View) : ViewHolder(itemView) {}
}