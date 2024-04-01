package com.example.weather_assignment

import androidx.room.Entity


@Entity(tableName = "weather_history", primaryKeys = ["location", "date"])
data class WeatherHistory(
    val location: String,
    val date: String,
    val maxTemp: Double,
    val minTemp: Double,
    val weatherDetails: String
)