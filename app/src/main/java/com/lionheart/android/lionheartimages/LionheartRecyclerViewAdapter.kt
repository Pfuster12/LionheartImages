package com.lionheart.android.lionheartimages

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class LionheartRecyclerViewAdapter(private val context: Context) : RecyclerView.Adapter<ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // create view holder
        return ImageViewHolder(inflateViewHolder(R.layout.image_item, parent))
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
        return 0
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // bind view holder
    }

    class ImageViewHolder(itemView: View) : ViewHolder(itemView) {}
}