package com.azanalarm.app.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.azanalarm.app.AlarmReceiver
import com.azanalarm.app.data.model.PrayerTime
import java.text.SimpleDateFormat
import java.util.*

class AlarmScheduler(private val context: Context) {
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val prefs = context.getSharedPreferences("AzanAlarmPrefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "AlarmScheduler"
        private const val FAJR_REQUEST_CODE = 1001
        private const val DHUHR_REQUEST_CODE = 1002
        private const val ASR_REQUEST_CODE = 1003
        private const val MAGHRIB_REQUEST_CODE = 1004
        private const val ISHA_REQUEST_CODE = 1005
    }
    
    fun scheduleAllAlarms(prayerTimes: List<PrayerTime>) {
        prayerTimes.forEach { prayerTime ->
            if (prayerTime.enabled) {
                scheduleAlarm(prayerTime)
            }
        }
        savePrayerTimes(prayerTimes)
    }
    
    private fun scheduleAlarm(prayerTime: PrayerTime) {
        val requestCode = getRequestCodeForPrayer(prayerTime.name)
        val alarmTime = parseTimeToCalendar(prayerTime.time)
        
        if (alarmTime == null) {
            Log.e(TAG, "Failed to parse time: ${prayerTime.time}")
            return
        }
        
        val now = Calendar.getInstance()
        if (alarmTime.before(now)) {
            alarmTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.azanalarm.app.AZAN_ALARM"
            putExtra("prayer_name", prayerTime.name)
            putExtra("prayer_time", prayerTime.time)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.timeInMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled alarm for ${prayerTime.name} at ${prayerTime.time}")
                } else {
                    Log.e(TAG, "Cannot schedule exact alarms. Permission required.")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled alarm for ${prayerTime.name} at ${prayerTime.time}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm: ${e.message}", e)
        }
    }
    
    fun cancelAllAlarms() {
        val requestCodes = listOf(
            FAJR_REQUEST_CODE,
            DHUHR_REQUEST_CODE,
            ASR_REQUEST_CODE,
            MAGHRIB_REQUEST_CODE,
            ISHA_REQUEST_CODE
        )
        
        requestCodes.forEach { requestCode ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.azanalarm.app.AZAN_ALARM"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        
        Log.d(TAG, "All alarms cancelled")
    }
    
    fun cancelAlarm(prayerName: String) {
        val requestCode = getRequestCodeForPrayer(prayerName)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.azanalarm.app.AZAN_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        
        Log.d(TAG, "Alarm cancelled for $prayerName")
    }
    
    private fun parseTimeToCalendar(timeString: String): Calendar? {
        return try {
            val cleanTime = timeString.split(" ")[0].trim()
            
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val date = sdf.parse(cleanTime)
            
            val calendar = Calendar.getInstance()
            val timeCalendar = Calendar.getInstance()
            timeCalendar.time = date ?: return null
            
            calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            
            calendar
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing time: $timeString", e)
            null
        }
    }
    
    private fun getRequestCodeForPrayer(prayerName: String): Int {
        return when (prayerName.uppercase()) {
            "FAJR" -> FAJR_REQUEST_CODE
            "DHUHR" -> DHUHR_REQUEST_CODE
            "ASR" -> ASR_REQUEST_CODE
            "MAGHRIB" -> MAGHRIB_REQUEST_CODE
            "ISHA" -> ISHA_REQUEST_CODE
            else -> 1000
        }
    }
    
    private fun savePrayerTimes(prayerTimes: List<PrayerTime>) {
        val editor = prefs.edit()
        prayerTimes.forEach { prayer ->
            editor.putString("${prayer.name}_time", prayer.time)
            editor.putBoolean("${prayer.name}_enabled", prayer.enabled)
        }
        editor.putLong("last_updated", System.currentTimeMillis())
        editor.apply()
    }
    
    fun getSavedPrayerTimes(): List<PrayerTime>? {
        val prayerNames = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val prayerTimes = mutableListOf<PrayerTime>()
        
        prayerNames.forEach { name ->
            val time = prefs.getString("${name}_time", null)
            val enabled = prefs.getBoolean("${name}_enabled", true)
            
            if (time != null) {
                prayerTimes.add(PrayerTime(name, time, enabled))
            } else {
                return null
            }
        }
        
        return if (prayerTimes.size == 5) prayerTimes else null
    }
}
