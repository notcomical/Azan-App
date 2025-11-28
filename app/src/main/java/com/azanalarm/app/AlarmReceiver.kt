package com.azanalarm.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class AlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "azan_alarm_channel"
        private const val CHANNEL_NAME = "Azan Alarms"
        private const val NOTIFICATION_ID = 100
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""
        
        Log.d(TAG, "Alarm received for $prayerName at $prayerTime")
        
        val allTimes = getAllPrayerTimes(context)
        
        val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("prayer_name", prayerName)
            putExtra("prayer_time", prayerTime)
            putExtra("all_times", allTimes)
        }
        context.startActivity(alarmIntent)
        
        val soundUri = getSavedSoundUri(context)
        createNotificationChannel(context, soundUri)
        showNotification(context, prayerName, prayerTime, soundUri, allTimes)
    }
    
    private fun getAllPrayerTimes(context: Context): String {
        val prefs = context.getSharedPreferences("azan_prefs", Context.MODE_PRIVATE)
        val fajr = prefs.getString("prayer_fajr", "--")
        val dhuhr = prefs.getString("prayer_dhuhr", "--")
        val asr = prefs.getString("prayer_asr", "--")
        val maghrib = prefs.getString("prayer_maghrib", "--")
        val isha = prefs.getString("prayer_isha", "--")
        val tahajjud = prefs.getString("prayer_tahajjud", "--")
        
        return "Fajr: $fajr\nDhuhr: $dhuhr\nAsr: $asr\nMaghrib: $maghrib\nIsha: $isha\nTahajjud: $tahajjud"
    }
    
    private fun getSavedSoundUri(context: Context): android.net.Uri? {
        val prefs = context.getSharedPreferences("AzanAlarmPrefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("custom_sound_uri", null)
        return uriString?.let { android.net.Uri.parse(it) }
    }
    
    private fun createNotificationChannel(context: Context, customSoundUri: android.net.Uri?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Azan prayer times"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000)
                setSound(soundUri, audioAttributes)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun showNotification(context: Context, prayerName: String, prayerTime: String, customSoundUri: android.net.Uri?, allTimes: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = customSoundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        
        val bigText = "It's time for $prayerName prayer ($prayerTime)\n\nToday's Prayer Times:\n$allTimes"
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$prayerName Prayer Time")
            .setContentText("It's time for $prayerName prayer ($prayerTime)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setContentIntent(pendingIntent)
            .build()
        
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Notification permission not granted", e)
        }
    }
}
