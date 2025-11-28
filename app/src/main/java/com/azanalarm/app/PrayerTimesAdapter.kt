package com.azanalarm.app

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.azanalarm.app.data.model.PrayerTime
import com.azanalarm.app.databinding.ItemPrayerTimeBinding
import java.text.SimpleDateFormat
import java.util.*

class PrayerTimesAdapter(
    private val context: Context,
    private val prayerTimes: List<PrayerTime>,
    private val onToggle: (Int, Boolean) -> Unit,
    private val onPrayerClick: (Int, String, String) -> Unit
) : RecyclerView.Adapter<PrayerTimesAdapter.PrayerTimeViewHolder>() {
    
    inner class PrayerTimeViewHolder(private val binding: ItemPrayerTimeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(prayerTime: PrayerTime, position: Int) {
            binding.tvPrayerName.text = prayerTime.name
            binding.tvPrayerTime.text = formatTime(prayerTime.time)
            binding.switchAlarm.isChecked = prayerTime.enabled
            
            binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
                onToggle(position, isChecked)
            }
            
            binding.root.setOnClickListener {
                onPrayerClick(position, prayerTime.name, prayerTime.time)
            }
        }
        
        private fun formatTime(time: String): String {
            val prefs = context.getSharedPreferences("AzanAlarmPrefs", Context.MODE_PRIVATE)
            val is24Hour = prefs.getBoolean("time_format_24h", false)
            
            return try {
                val cleanTime = time.split(" ")[0].trim()
                val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = inputFormat.parse(cleanTime) ?: return time
                
                val outputFormat = if (is24Hour) {
                    SimpleDateFormat("HH:mm", Locale.getDefault())
                } else {
                    SimpleDateFormat("hh:mm a", Locale.getDefault())
                }
                outputFormat.format(date)
            } catch (e: Exception) {
                time
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PrayerTimeViewHolder {
        val binding = ItemPrayerTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PrayerTimeViewHolder(binding)
    }
    
    override fun onBindViewHolder(holder: PrayerTimeViewHolder, position: Int) {
        holder.bind(prayerTimes[position], position)
    }
    
    override fun getItemCount(): Int = prayerTimes.size
}
