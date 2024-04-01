package com.example.weather_assignment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WeatherHistoryDao {

    @Query("SELECT * FROM weather_history WHERE location = :location AND date = :date")
    suspend fun getWeatherData(location: String, date: String): WeatherHistory?
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(weatherHistory: WeatherHistory)
}
