package com.example.weather_assignment

import com.example.weather_assignment.WeatherDatabase

open class WeatherRepository(private val database: WeatherDatabase) {
    suspend fun insertWeatherData(weatherEntity: WeatherHistory)
    {
        database.weatherDao().insert(weatherEntity)
    }

    suspend fun getWeatherData(location: String, date: String): WeatherHistory?
    {
        return database.weatherDao().getWeatherData(location, date)
    }


}