package com.azanalarm.app.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationManager(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(): Location = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            continuation.resumeWithException(SecurityException("Location permission not granted"))
            return@suspendCancellableCoroutine
        }
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    continuation.resume(location)
                } else {
                    requestNewLocation(continuation)
                }
            }.addOnFailureListener { exception ->
                requestNewLocation(continuation)
            }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }
    
    private fun requestNewLocation(continuation: kotlinx.coroutines.CancellableContinuation<Location>) {
        if (!hasLocationPermission()) {
            continuation.resumeWithException(SecurityException("Location permission not granted"))
            return
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L // 10 seconds
        ).apply {
            setMinUpdateIntervalMillis(5000L) // 5 seconds
            setMaxUpdates(1)
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            
            continuation.invokeOnCancellation {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
    }
    
    fun saveLocation(latitude: Double, longitude: Double) {
        val prefs = context.getSharedPreferences("AzanAlarmPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("latitude", latitude.toString())
            putString("longitude", longitude.toString())
            apply()
        }
    }
    
    fun getSavedLocation(): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences("AzanAlarmPrefs", Context.MODE_PRIVATE)
        val lat = prefs.getString("latitude", null)?.toDoubleOrNull()
        val lon = prefs.getString("longitude", null)?.toDoubleOrNull()
        
        return if (lat != null && lon != null) {
            Pair(lat, lon)
        } else {
            null
        }
    }
}
