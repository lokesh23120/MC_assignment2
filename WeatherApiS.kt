package com.example.weather_assignment

import retrofit2.http.GET
import retrofit2.http.Path

interface WeatherApiS {
    @GET("VisualCrossingWebServices/rest/services/timeline/{location}/{date}/?key=YD9YR9EDUG7293XM2TCL3GYAC")
    suspend fun getWeather(
        @Path("location") location: String,
        @Path("date") date: String
    ): String

}