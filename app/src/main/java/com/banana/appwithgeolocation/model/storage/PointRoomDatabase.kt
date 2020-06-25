package com.banana.appwithgeolocation.model.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.banana.appwithgeolocation.model.entity.Point
import com.banana.appwithgeolocation.model.storage.dao.PointDao
import kotlinx.coroutines.CoroutineScope

@Database(entities = [Point::class], version = 1, exportSchema = false)
abstract class PointRoomDatabase : RoomDatabase() {

    abstract fun pointDao(): PointDao

    private class PointDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
    }

    companion object {
        @Volatile
        private var INSTANCE: PointRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): PointRoomDatabase {
            return INSTANCE
                ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PointRoomDatabase::class.java,
                    "point_database"
                )
                    .addCallback(PointDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}