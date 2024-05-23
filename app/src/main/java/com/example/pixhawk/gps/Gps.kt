package com.example.pixhawk.gps

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.pixhawk.viewModel.LogViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class Gps( private val context: Context, private val logViewModel: LogViewModel) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val handler = Handler(Looper.getMainLooper())

    private val locationRunnable = object : Runnable {
        override fun run() {
            getLastLocation()
            handler.postDelayed(this, 10000) // 10 seconds
        }
    }
    init {
        handler.post(locationRunnable)
    }
    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 101)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val latitudeStr = latitude.toString()

                    val newLatitudeStr = latitudeStr.dropLast(1)
                    val newLatitude = newLatitudeStr.toDouble()

                    val longitudeStr = longitude.toString()
                    val newLongitudeStr = longitudeStr.dropLast(1)
                    val newLongitude = newLongitudeStr.toDouble()

                    Log.i("mohammed","$newLatitude,$newLongitude")
                }
            }
            .addOnFailureListener { e ->
                Log.e(ContentValues.TAG, "Failed to get location: ${e.message}")
            }
    }
}