package com.azanalarm.app

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.azanalarm.app.databinding.ActivityAlarmBinding

class AlarmActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityAlarmBinding
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val prayerName = intent.getStringExtra("prayer_name") ?: "Prayer"
        val prayerTime = intent.getStringExtra("prayer_time") ?: ""
        val allTimes = intent.getStringExtra("all_times") ?: ""
        
        binding.tvPrayerName.text = "$prayerName Time"
        binding.tvPrayerTime.text = prayerTime
        binding.tvAllTimes.text = allTimes
        
        binding.btnDismiss.setOnClickListener {
            stopAlarm()
            finish()
        }
        
        binding.btnSnooze.setOnClickListener {
            stopAlarm()
            finish()
        }
        
        startAlarm()
    }
    
    private fun startAlarm() {
        try {
            val soundUri = getSavedSoundUri() ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmActivity, soundUri)
                isLooping = true
                prepare()
                start()
            }
            
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 1000, 500, 1000),
                    0
                )
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 1000, 500, 1000), 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAlarm() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        
        vibrator?.cancel()
        vibrator = null
    }
    
    private fun getSavedSoundUri(): Uri? {
        val prefs = getSharedPreferences("AzanAlarmPrefs", MODE_PRIVATE)
        val uriString = prefs.getString("custom_sound_uri", null)
        return uriString?.let { Uri.parse(it) }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
    }
}
