package com.example.mausam.Models

import java.io.Serializable


data class WeatherResponse (
    val coord:Coord,
    val weather:List<Weather>,
    val base:String,
    val main:Main,
    val visibility:Int,
    val wind:Wind,
    val rain:Rain,
    val cloud:Cloud,
    val dt:Int,
    val sys:Sys,
    val timeZone:Int,
    val id:Int,
    val name:String,
    val cod:Int
):Serializable