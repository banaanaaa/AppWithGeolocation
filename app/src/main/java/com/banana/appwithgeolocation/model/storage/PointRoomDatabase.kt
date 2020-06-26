package com.banana.appwithgeolocation.model.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.model.storage.dao.PointDao

@Database(entities = [Point::class], version = 1, exportSchema = false)
abstract class PointRoomDatabase : RoomDatabase() {

    abstract fun pointDao(): PointDao

    companion object {
        @Volatile
        private var INSTANCE: PointRoomDatabase? = null

        fun getDatabase(
            context: Context
        ): PointRoomDatabase {
            return INSTANCE
                ?: synchronized(this) {
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        PointRoomDatabase::class.java,
                        "point_database"
                    ).build()
                    INSTANCE = instance
                    instance
                }
        }
    }
}