package com.lionheart.android.lionheartimages.pojo

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Image pojo annotated as an entity to mark it as a table in the Room database
 */
@Entity(tableName = "images")
data class LionheartImage(var imageId: Int,
                     var imageLink: String,
                     var imageHeight: Int,
                     var width: Int,
                     var thumbnailLink: String) {

    // create an id outside the constructor so we don't need to pass it.
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null
}