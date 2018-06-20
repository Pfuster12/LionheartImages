package com.lionheart.android.lionheartimages.database

import android.arch.lifecycle.LiveData
import android.arch.persistence.room.*
import android.arch.persistence.room.OnConflictStrategy.REPLACE
import com.lionheart.android.lionheartimages.pojo.LionheartImage

@Dao
interface ImageDao {

    @Query("SELECT * FROM images")
    fun getAll(): LiveData<List<LionheartImage>>

    @Insert(onConflict = REPLACE)
    fun saveAll(vararg images: LionheartImage): List<Long>

    @Delete
    fun deleteImages(vararg images: LionheartImage)
}