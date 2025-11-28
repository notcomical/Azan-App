package com.azanalarm.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.azanalarm.app.utils.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Device booted, rescheduling alarms")
            
            val alarmScheduler = AlarmScheduler(context)
            val savedPrayerTimes = alarmScheduler.getSavedPrayerTimes()
            
            if (savedPrayerTimes != null) {
                alarmScheduler.scheduleAllAlarms(savedPrayerTimes)
                Log.d(TAG, "Alarms rescheduled successfully")
            } else {
                Log.d(TAG, "No saved prayer times found")
            }
        }
    }
}
