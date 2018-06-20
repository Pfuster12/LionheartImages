package com.lionheart.android.lionheartimages.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import com.lionheart.android.lionheartimages.pojo.LionheartImage

@Database(entities = [LionheartImage::class], version = 1)
abstract class ImageDatabase: RoomDatabase() {
    abstract fun imageDao(): ImageDao

    /**
     * @googledocs "You should follow the singleton design pattern when instantiating an
     * AppDatabase object, as each RoomDatabase instance is fairly expensive, and you rarely need
     * access to multiple instances."
     * @synchronized() provides protection by ensuring that a crucial section of the code is never
     * executed concurrently by two different threads, ensuring data consistency.
     */
    companion object {
        private var INSTANCE: ImageDatabase? = null

        fun getInstance(context: Context) =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: Room.databaseBuilder(context.applicationContext,
                            ImageDatabase::class.java,
                            "images")
                            .build()
                }
    }

    fun destroyInstance() {
        INSTANCE = null
    }
}