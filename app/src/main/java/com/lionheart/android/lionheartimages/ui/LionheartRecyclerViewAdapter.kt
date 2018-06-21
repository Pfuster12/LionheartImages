package com.lionheart.android.lionheartimages.ui

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.lionheart.android.lionheartimages.R
import com.lionheart.android.lionheartimages.pojo.LionheartImage

/**
 * My recyclerview adapter with an onclick listener and the image list
 */
class LionheartRecyclerViewAdapter(private val context: Context,
                                   private val images: List<LionheartImage>,
                                   private val listener: (image: LionheartImage) -> Unit)
    : RecyclerView.Adapter<ViewHolder>() {

    /*
     / global variables & constants
     */

    private val IMAGE_ITEM_TYPE = 101

    /*
    / functions
    */

    /**
     * Override function that creates the view holder object.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            IMAGE_ITEM_TYPE -> ImageViewHolder(inflateViewHolder(R.layout.image_item, parent),
                    images, listener)
            else -> ImageViewHolder(inflateViewHolder(R.layout.image_item, parent),
                    images, listener)
        }

    /**
     * Helper fun to inflate view
     */
    private fun inflateViewHolder(layoutId: Int, parent: ViewGroup?): View {
        // return a layout inflater
        return LayoutInflater.from(context).inflate(layoutId, parent, false)
    }


    /**
     * Override function to get the total items in the adapter.
     */
    override fun getItemCount(): Int {
        // get item count
        return images.size
    }

    /**
     * Override function to get the items type in case it is a header or footer
     */
    override fun getItemViewType(position: Int): Int {
        return IMAGE_ITEM_TYPE
    }

    /**
     * Override function that bind views in the view holder,
     * images to image views.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        // bind view holder
        when (getItemViewType(position)) {
            IMAGE_ITEM_TYPE -> {
                // cast the holder to an image holder
                val imageViewHolder = holder as ImageViewHolder
                // bind the views
                bindImageItem(imageViewHolder, position)
            }
        }
    }

    /**
     * Helper fun to bind views to the view holder
     */
    private fun bindImageItem(holder: ImageViewHolder, position: Int) {
        // get current image
        val currentImage = images[position]

        // set the image
        Glide.with(context)
                .load(currentImage.thumbnailLink)
                .transition(withCrossFade())
                .into(holder.image)
    }

    /**
     * Viewholder class to hold the image
     */
    class ImageViewHolder(itemView: View,
                          private val images: List<LionheartImage>,
                          private val listener: (image: LionheartImage) -> Unit)
        : ViewHolder(itemView) {

        init {
            // Set the view onClick listener to the function passed to the adapter's
            // constructor, i.e. defined in the activity. make sure the adapter position
            // is within the extracts and not a header or footer.
            itemView.setOnClickListener{
                listener(images[adapterPosition])
            }
        }

        val image: ImageView = itemView.findViewById(R.id.image)
    }
}