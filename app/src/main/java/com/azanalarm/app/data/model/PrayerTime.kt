package com.azanalarm.app.data.model

data class PrayerTime(
    val name: String,
    val time: String,
    val enabled: Boolean = true
)

enum class PrayerName {
    FAJR,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA
}
