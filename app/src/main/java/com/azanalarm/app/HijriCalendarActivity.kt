package com.azanalarm.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.azanalarm.app.data.api.RetrofitClient
import com.azanalarm.app.databinding.FragmentHijriCalendarBinding
import com.azanalarm.app.utils.LocationManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HijriCalendarFragment : androidx.fragment.app.Fragment() {
    
    private var _binding: FragmentHijriCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var locationManager: LocationManager
    
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View {
        _binding = FragmentHijriCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        locationManager = LocationManager(requireContext())
        fetchHijriDate()
    }
    
    private fun fetchHijriDate() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val location = if (locationManager.hasLocationPermission()) {
                    locationManager.getCurrentLocation()
                } else {
                    val savedLocation = locationManager.getSavedLocation()
                    if (savedLocation != null) {
                        android.location.Location("").apply {
                            latitude = savedLocation.first
                            longitude = savedLocation.second
                        }
                    } else {
                        android.location.Location("").apply {
                            latitude = 0.0
                            longitude = 0.0
                        }
                    }
                }
                
                val response = RetrofitClient.azanApiService.getPrayerTimes(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    method = 1
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val hijriDate = response.body()!!.data.date.hijri
                    val gregorianDate = response.body()!!.data.date.gregorian
                    
                    binding.tvHijriDate.text = hijriDate.date
                    binding.tvHijriDay.text = hijriDate.day
                    binding.tvHijriMonth.text = hijriDate.month["en"] ?: ""
                    binding.tvHijriYear.text = "${hijriDate.year} AH"
                    binding.tvHijriWeekday.text = hijriDate.weekday["en"] ?: ""
                    binding.tvGregorianWeekday.text = gregorianDate.weekday["en"] ?: ""
                    binding.tvGregorianDate.text = gregorianDate.date
                    
                } else {
                    Toast.makeText(requireContext(), "Failed to fetch Hijri date", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
