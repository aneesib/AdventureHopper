package com.anees.adventurehopper.location

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

data class ApproximateLocation(
    val latitude: Double,
    val longitude: Double,
    val areaName: String
)

object LocationService {
    fun getLastKnownApproximateLocation(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { location -> location.time }
    }

    fun getAreaName(context: Context, location: Location): String? {
        if (!Geocoder.isPresent()) return null

        return runCatching {
            Geocoder(context).getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
                ?.let { address ->
                    address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                }
        }.getOrNull()
    }

    fun getApproximateLocation(context: Context): ApproximateLocation? {
        val location = getLastKnownApproximateLocation(context) ?: return null
        return ApproximateLocation(
            latitude = location.latitude,
            longitude = location.longitude,
            areaName = getAreaName(context, location).orEmpty()
        )
    }
}