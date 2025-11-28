package com.azanalarm.app

import android.Manifest
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.azanalarm.app.data.api.RetrofitClient
import com.azanalarm.app.data.model.PrayerTime
import com.azanalarm.app.databinding.ActivityMainBinding
import com.azanalarm.app.utils.AlarmScheduler
import com.azanalarm.app.utils.LocationManager
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var locationManager: LocationManager
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var adapter: PrayerTimesAdapter
    
    private val prayerTimes = mutableListOf<PrayerTime>()
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            fetchPrayerTimes()
        } else {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Notification permission is recommended", Toast.LENGTH_SHORT).show()
        }
    }
    
    private val audioPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            saveCustomSound(it)
            updateSoundDisplay(it)
            Toast.makeText(this, "Custom alarm sound selected", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isDarkMode = getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES 
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        locationManager = LocationManager(this)
        alarmScheduler = AlarmScheduler(this)
        updateSoundDisplay()
        updateTopDatesInitial()
        if (locationManager.hasLocationPermission() || locationManager.getSavedLocation() != null) {
            fetchPrayerTimes()
        }
        
        setupRecyclerView()
        setupListeners()
        setupDrawer()
        requestPermissions()
        
        loadSavedPrayerTimes()
    }

    private fun setupDrawer() {
        binding.tvTitle.setOnClickListener {
            if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
                binding.drawerLayout.closeDrawer(GravityCompat.END)
            } else {
                binding.drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        menu.findItem(R.id.action_time_format).isChecked = is24HourFormat()
        menu.findItem(R.id.action_dark_mode).isChecked = isDarkMode()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_fetch_times -> {
                fetchPrayerTimes()
                true
            }
            R.id.action_select_sound -> {
                openAudioPicker()
                true
            }
            R.id.action_time_format -> {
                item.isChecked = !item.isChecked
                save24HourFormat(item.isChecked)
                adapter.notifyDataSetChanged()
                true
            }
            R.id.action_dark_mode -> {
                item.isChecked = !item.isChecked
                saveDarkMode(item.isChecked)
                AppCompatDelegate.setDefaultNightMode(
                    if (item.isChecked) AppCompatDelegate.MODE_NIGHT_YES 
                    else AppCompatDelegate.MODE_NIGHT_NO
                )
                true
            }
            R.id.action_iqamah_times -> {
                showIqamahTimesDialog()
                true
            }
            R.id.action_calculation_method -> {
                showCalculationMethodDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        adapter = PrayerTimesAdapter(
            context = this,
            prayerTimes = prayerTimes,
            onToggle = { position, enabled ->
                onPrayerToggled(position, enabled)
            },
            onPrayerClick = { position, name, time ->
                showPrayerTimePicker(position, name, time)
            }
        )
        binding.recyclerViewPrayerTimes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPrayerTimes.adapter = adapter
    }

    private fun updateTopDatesInitial() {
        try {
            val sdf = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.getDefault())
            val today = java.util.Date()
            binding.tvGregorianTopDate.text = sdf.format(today)
            binding.tvHijriTopDate.text = "--"
        } catch (_: Exception) {
            binding.tvGregorianTopDate.text = "--"
            binding.tvHijriTopDate.text = "--"
        }
    }
    
    private fun setupListeners() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchPrayerTimes()
        }
    }
    
    private fun openAudioPicker() {
        audioPickerLauncher.launch(arrayOf("audio/*"))
    }
    
    private fun saveCustomSound(uri: Uri) {
        val prefs = getSharedPreferences("AzanAlarmPrefs", MODE_PRIVATE)
        prefs.edit().putString("custom_sound_uri", uri.toString()).apply()
    }
    
    private fun updateSoundDisplay(uri: Uri? = null) {
        val soundUri = uri ?: getSavedSoundUri()
        if (soundUri != null) {
            val fileName = getFileName(soundUri)
            binding.tvSelectedSound.text = "Sound: $fileName"
        } else {
            binding.tvSelectedSound.text = "Sound: Default Alarm"
        }
    }
    
    private fun is24HourFormat(): Boolean {
        val prefs = getSharedPreferences("AzanAlarmPrefs", MODE_PRIVATE)
        return prefs.getBoolean("time_format_24h", false)
    }
    
    private fun save24HourFormat(is24Hour: Boolean) {
        val prefs = getSharedPreferences("AzanAlarmPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("time_format_24h", is24Hour).apply()
    }
    
    private fun isDarkMode(): Boolean {
        return getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .getBoolean("dark_mode", false)
    }
    
    private fun saveDarkMode(isDark: Boolean) {
        getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .edit()
            .putBoolean("dark_mode", isDark)
            .apply()
    }
    
    private fun getSavedSoundUri(): Uri? {
        val prefs = getSharedPreferences("AzanAlarmPrefs", MODE_PRIVATE)
        val uriString = prefs.getString("custom_sound_uri", null)
        return uriString?.let { Uri.parse(it) }
    }
    
    private fun getFileName(uri: Uri): String {
        var result = "Custom Sound"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                result = cursor.getString(nameIndex)
            }
        }
        return result
    }
    
    private fun requestPermissions() {
        if (!locationManager.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
        
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    
    private fun loadSavedPrayerTimes() {
        val savedTimes = alarmScheduler.getSavedPrayerTimes()
        if (savedTimes != null) {
            prayerTimes.clear()
            prayerTimes.addAll(savedTimes)
            adapter.notifyDataSetChanged()
            updateLocationDisplay()
        }
    }
    
    private fun fetchPrayerTimes() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val coords: Pair<Double, Double>? = if (locationManager.hasLocationPermission()) {
                    val location = locationManager.getCurrentLocation()
                    Pair(location.latitude, location.longitude)
                } else {
                    locationManager.getSavedLocation()
                }
                if (coords == null) {
                    Toast.makeText(this@MainActivity, "Please grant location permission or set a location", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                val latitude = coords.first
                val longitude = coords.second
                
                val locationName = getLocationName(latitude, longitude)
                
                locationManager.saveLocation(latitude, longitude)
                saveLocationName(locationName)
                updateLocationDisplay()
                
                val selectedMethod = getSharedPreferences("azan_prefs", MODE_PRIVATE)
                    .getInt("calculation_method", 1)
                
                val response = RetrofitClient.azanApiService.getPrayerTimes(
                    latitude = latitude,
                    longitude = longitude,
                    method = selectedMethod
                )
                
                if (response.isSuccessful && response.body() != null) {
                    val azanData = response.body()!!.data
                    val timings = azanData.timings
                    val hijri = azanData.date.hijri
                    val greg = azanData.date.gregorian
                    
                    prayerTimes.clear()
                    prayerTimes.add(PrayerTime("Fajr", timings.fajr, true))
                    prayerTimes.add(PrayerTime("Dhuhr", timings.dhuhr, true))
                    prayerTimes.add(PrayerTime("Asr", timings.asr, true))
                    prayerTimes.add(PrayerTime("Maghrib", timings.maghrib, true))
                    prayerTimes.add(PrayerTime("Isha", timings.isha, true))
                    
                    val tahajjudTime = getSavedTahajjudTime()
                    prayerTimes.add(PrayerTime("Tahajjud", tahajjudTime, true))
                    
                    savePrayerTimes(timings.fajr, timings.dhuhr, timings.asr, timings.maghrib, timings.isha, tahajjudTime)
                    
                    adapter.notifyDataSetChanged()
                    
                    alarmScheduler.scheduleAllAlarms(prayerTimes)

                    val gregText = "${greg.weekday["en"] ?: ""}, ${greg.date}"
                    val hijriText = "${hijri.weekday["en"] ?: ""}, ${hijri.day} ${hijri.month["en"] ?: ""} ${hijri.year} AH"
                    binding.tvGregorianTopDate.text = gregText
                    binding.tvHijriTopDate.text = hijriText
                    
                    Toast.makeText(
                        this@MainActivity,
                        "Prayer times updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Failed to fetch prayer times",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e(TAG, "API Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                Log.e(TAG, "Error fetching prayer times", e)
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    private fun updateLocationDisplay() {
        val locationName = getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .getString("location_name", null)
        
        if (locationName != null) {
            binding.tvLocation.text = "Location: $locationName"
        } else {
            val savedLocation = locationManager.getSavedLocation()
            if (savedLocation != null) {
                val (lat, lon) = savedLocation
                binding.tvLocation.text = "Location: %.4f, %.4f".format(lat, lon)
            }
        }
    }
    
    private fun getLocationName(latitude: Double, longitude: Double): String {
        return try {
            val geocoder = Geocoder(this)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val locality = address.locality ?: address.subAdminArea
                val adminArea = address.adminArea
                
                when {
                    locality != null && adminArea != null -> "$locality, $adminArea"
                    locality != null -> locality
                    adminArea != null -> adminArea
                    else -> "%.4f, %.4f".format(latitude, longitude)
                }
            } else {
                "%.4f, %.4f".format(latitude, longitude)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error", e)
            "%.4f, %.4f".format(latitude, longitude)
        }
    }
    
    private fun saveLocationName(name: String) {
        getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .edit()
            .putString("location_name", name)
            .apply()
    }
    
    private fun savePrayerTimes(fajr: String, dhuhr: String, asr: String, maghrib: String, isha: String, tahajjud: String) {
        getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .edit()
            .putString("prayer_fajr", fajr)
            .putString("prayer_dhuhr", dhuhr)
            .putString("prayer_asr", asr)
            .putString("prayer_maghrib", maghrib)
            .putString("prayer_isha", isha)
            .putString("prayer_tahajjud", tahajjud)
            .apply()
    }
    
    private fun showTahajjudTimePicker() {
        val savedTime = getSavedTahajjudTime()
        val parts = savedTime.split(":")
        val hour = parts[0].toIntOrNull() ?: 2
        val minute = parts[1].toIntOrNull() ?: 0
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
            saveTahajjudTime(timeString)
            
            val tahajjudIndex = prayerTimes.indexOfFirst { it.name == "Tahajjud" }
            if (tahajjudIndex >= 0) {
                val currentPrayer = prayerTimes[tahajjudIndex]
                prayerTimes[tahajjudIndex] = currentPrayer.copy(time = timeString)
                adapter.notifyItemChanged(tahajjudIndex)
                
                if (currentPrayer.enabled) {
                    alarmScheduler.scheduleAllAlarms(listOf(prayerTimes[tahajjudIndex]))
                }
            }
            
            val prefs = getSharedPreferences("azan_prefs", MODE_PRIVATE)
            prefs.edit().putString("prayer_tahajjud", timeString).apply()
            
            Toast.makeText(this, "Tahajjud time set to $timeString", Toast.LENGTH_SHORT).show()
        }, hour, minute, true).show()
    }
    
    private fun showPrayerTimePicker(position: Int, prayerName: String, currentTime: String) {
        val cleanTime = currentTime.split(" ")[0].trim()
        val parts = cleanTime.split(":")
        val hour = parts[0].toIntOrNull() ?: 0
        val minute = parts[1].toIntOrNull() ?: 0
        
        TimePickerDialog(this, { _, selectedHour, selectedMinute ->
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
            
            if (position >= 0 && position < prayerTimes.size) {
                val currentPrayer = prayerTimes[position]
                prayerTimes[position] = currentPrayer.copy(time = timeString)
                adapter.notifyItemChanged(position)
                
                if (prayerName == "Tahajjud") {
                    saveTahajjudTime(timeString)
                    val prefs = getSharedPreferences("azan_prefs", MODE_PRIVATE)
                    prefs.edit().putString("prayer_tahajjud", timeString).apply()
                }
                
                if (currentPrayer.enabled) {
                    alarmScheduler.scheduleAllAlarms(listOf(prayerTimes[position]))
                }
                
                Toast.makeText(this, "$prayerName time updated to $timeString", Toast.LENGTH_SHORT).show()
            }
        }, hour, minute, true).show()
    }
    
    private fun getSavedTahajjudTime(): String {
        return getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .getString("tahajjud_custom_time", "02:00") ?: "02:00"
    }
    
    private fun saveTahajjudTime(time: String) {
        getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .edit()
            .putString("tahajjud_custom_time", time)
            .apply()
    }
    
    private fun onPrayerToggled(position: Int, enabled: Boolean) {
        if (position < prayerTimes.size) {
            val prayer = prayerTimes[position]
            prayerTimes[position] = prayer.copy(enabled = enabled)
            
            if (enabled) {
                alarmScheduler.scheduleAllAlarms(listOf(prayerTimes[position]))
                Toast.makeText(this, "${prayer.name} alarm enabled", Toast.LENGTH_SHORT).show()
            } else {
                alarmScheduler.cancelAlarm(prayer.name)
                Toast.makeText(this, "${prayer.name} alarm disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showIqamahTimesDialog() {
        val prayers = arrayOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
        val iqamahMinutes = IntArray(5) { 0 }
        
        for (i in prayers.indices) {
            val prefs = getSharedPreferences("azan_prefs", MODE_PRIVATE)
            iqamahMinutes[i] = prefs.getInt("iqamah_${prayers[i].lowercase()}", 10)
        }
        
        val message = buildString {
            append("Set minutes after Azan for Iqamah:\n\n")
            prayers.forEachIndexed { index, prayer ->
                append("$prayer: ${iqamahMinutes[index]} minutes\n")
            }
            append("\nTap on a prayer to change the time.")
        }
        
        val items = prayers.mapIndexed { index, prayer ->
            "$prayer (${iqamahMinutes[index]} min)"
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle("Iqamah Times")
            .setItems(items) { _, which ->
                showIqamahTimePicker(prayers[which], iqamahMinutes[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }
    
    private fun showIqamahTimePicker(prayerName: String, currentMinutes: Int) {
        val options = arrayOf("5 min", "10 min", "15 min", "20 min", "25 min", "30 min", "Custom")
        val minuteValues = arrayOf(5, 10, 15, 20, 25, 30)
        
        AlertDialog.Builder(this)
            .setTitle("$prayerName Iqamah Time")
            .setItems(options) { _, which ->
                if (which == options.size - 1) {
                    val input = android.widget.EditText(this)
                    input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    input.setText(currentMinutes.toString())
                    
                    AlertDialog.Builder(this)
                        .setTitle("Enter minutes after Azan")
                        .setView(input)
                        .setPositiveButton("OK") { _, _ ->
                            val minutes = input.text.toString().toIntOrNull() ?: 10
                            saveIqamahTime(prayerName, minutes)
                            Toast.makeText(this, "$prayerName Iqamah set to $minutes min after Azan", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    saveIqamahTime(prayerName, minuteValues[which])
                    Toast.makeText(this, "$prayerName Iqamah set to ${minuteValues[which]} min after Azan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun saveIqamahTime(prayerName: String, minutes: Int) {
        getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .edit()
            .putInt("iqamah_${prayerName.lowercase()}", minutes)
            .apply()
    }
    
    private fun showCalculationMethodDialog() {
        val methods = arrayOf(
            "University of Islamic Sciences, Karachi",
            "Islamic Society of North America (ISNA)",
            "Muslim World League",
            "Umm Al-Qura University, Makkah",
            "Egyptian General Authority of Survey",
            "Institute of Geophysics, University of Tehran",
            "Gulf Region",
            "Kuwait",
            "Qatar",
            "Singapore",
            "Turkey",
            "Dubai"
        )
        
        val methodCodes = arrayOf(1, 2, 3, 4, 5, 7, 8, 9, 10, 11, 12, 13)
        
        val currentMethod = getSharedPreferences("azan_prefs", MODE_PRIVATE)
            .getInt("calculation_method", 1)
        
        val currentIndex = methodCodes.indexOf(currentMethod)
        
        AlertDialog.Builder(this)
            .setTitle("Choose Calculation Method")
            .setSingleChoiceItems(methods, currentIndex) { dialog, which ->
                getSharedPreferences("azan_prefs", MODE_PRIVATE)
                    .edit()
                    .putInt("calculation_method", methodCodes[which])
                    .apply()
                
                Toast.makeText(this, "Calculation method updated. Please refresh prayer times.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.END)) {
            binding.drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
