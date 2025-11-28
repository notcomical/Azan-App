package com.azanalarm.app.data.model

import com.google.gson.annotations.SerializedName

data class AzanTimesResponse(
    @SerializedName("code")
    val code: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("data")
    val data: AzanData
)

data class AzanData(
    @SerializedName("timings")
    val timings: Timings,
    @SerializedName("date")
    val date: DateInfo,
    @SerializedName("meta")
    val meta: Meta
)

data class Timings(
    @SerializedName("Fajr")
    val fajr: String,
    @SerializedName("Sunrise")
    val sunrise: String,
    @SerializedName("Dhuhr")
    val dhuhr: String,
    @SerializedName("Asr")
    val asr: String,
    @SerializedName("Sunset")
    val sunset: String,
    @SerializedName("Maghrib")
    val maghrib: String,
    @SerializedName("Isha")
    val isha: String,
    @SerializedName("Imsak")
    val imsak: String,
    @SerializedName("Midnight")
    val midnight: String,
    @SerializedName("Firstthird")
    val firstthird: String,
    @SerializedName("Lastthird")
    val lastthird: String
)

data class DateInfo(
    @SerializedName("readable")
    val readable: String,
    @SerializedName("timestamp")
    val timestamp: String,
    @SerializedName("hijri")
    val hijri: HijriDate,
    @SerializedName("gregorian")
    val gregorian: GregorianDate
)

data class HijriDate(
    @SerializedName("date")
    val date: String,
    @SerializedName("format")
    val format: String,
    @SerializedName("day")
    val day: String,
    @SerializedName("weekday")
    val weekday: Map<String, String>,
    @SerializedName("month")
    val month: Map<String, String>,
    @SerializedName("year")
    val year: String
)

data class GregorianDate(
    @SerializedName("date")
    val date: String,
    @SerializedName("format")
    val format: String,
    @SerializedName("day")
    val day: String,
    @SerializedName("weekday")
    val weekday: Map<String, String>,
    @SerializedName("month")
    val month: Map<String, String>,
    @SerializedName("year")
    val year: String
)

data class Meta(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("timezone")
    val timezone: String,
    @SerializedName("method")
    val method: Map<String, Any>
)
