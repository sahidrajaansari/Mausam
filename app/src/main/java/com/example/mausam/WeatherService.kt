package com.example.mausam

import com.example.mausam.Models.WeatherResponse
import retrofit.Call
import retrofit.http.GET
import retrofit.http.Query

interface WeatherService {
    @GET("2.5/weather")

    fun getWeather(
        @Query("lat")lat:Double,
        @Query("lon")lon:Double,
        @Query("units")units:String,
        @Query("appid")appid:String?
    ):Call<WeatherResponse>
}