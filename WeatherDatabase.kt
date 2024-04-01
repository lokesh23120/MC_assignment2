package com.example.weather_assignment


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.weather_assignment.WeatherHistory
import com.example.weather_assignment.WeatherHistoryDao

@Database(entities = [WeatherHistory::class], version = 1, exportSchema = false)
abstract class WeatherDatabase : RoomDatabase() {

    abstract fun weatherDao(): WeatherHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getInstance(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}