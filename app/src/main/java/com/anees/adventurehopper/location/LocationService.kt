package com.anees.adventurehopper.location

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

data class ApproximateLocation(
    val latitude: Double,
    val longitude: Double,
    val areaName: String
)

object LocationService {
    private const val TAG = "LocationService"
    private const val LOCATION_TIMEOUT_MS = 20_000L

    fun requestCurrentApproximateLocation(
        context: Context,
        onResult: (ApproximateLocation?) -> Unit
    ): () -> Unit {
        val coarsePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val finePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        Log.d(TAG, "Permission state: coarse=$coarsePermission, fine=$finePermission")

        if (!coarsePermission && !finePermission) {
            Log.e(TAG, "Location request failed: no location permission granted")
            onResult(null)
            return {}
        }

        val mainHandler = Handler(Looper.getMainLooper())
        var completed = false
        val cancellationTokenSource = CancellationTokenSource()
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val priority = if (finePermission) {
            Priority.PRIORITY_HIGH_ACCURACY
        } else {
            Priority.PRIORITY_BALANCED_POWER_ACCURACY
        }
        Log.d(TAG, "Fused location request: priority=$priority")

        fun finish(location: Location?, reason: String) {
            if (completed) return
            completed = true
            mainHandler.removeCallbacksAndMessages(null)
            cancellationTokenSource.cancel()
            if (location == null) {
                Log.e(TAG, "Fused location acquisition failed: reason=$reason")
            } else {
                Log.d(
                    TAG,
                    "Location received: provider=${location.provider}, latitude=${location.latitude}, longitude=${location.longitude}, time=${location.time}"
                )
            }
            onResult(
                location?.let {
                    val areaName = getAreaName(context, it)
                    Log.d(TAG, "Geocoder result: coordinates=${it.latitude},${it.longitude}, area=${areaName ?: "null"}")
                    ApproximateLocation(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        areaName = areaName.orEmpty()
                    )
                }
            )
        }

        fun useLastLocation(reason: String) {
            if (completed) return
            Log.w(TAG, "Trying fused lastLocation fallback: reason=$reason")
            fusedClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location == null) {
                        finish(null, "$reason; lastLocation=null")
                    } else {
                        finish(location, "$reason; lastLocation fallback")
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Fused lastLocation fallback failed", error)
                    finish(null, "$reason; lastLocation exception=${error.message}")
                }
            }

        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
            .setMaxUpdateAgeMillis(0L)
            .setDurationMillis(LOCATION_TIMEOUT_MS)
            .build()

        try {
            Log.d(TAG, "Requesting fused current location")
            fusedClient.getCurrentLocation(request, cancellationTokenSource.token)
                .addOnSuccessListener { location ->
                    if (location == null) {
                        Log.w(TAG, "Fused current location returned null")
                        useLastLocation("currentLocation=null")
                    } else {
                        finish(location, "fused current location")
                    }
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, "Fused current location failed", error)
                    useLastLocation("currentLocation exception=${error.message}")
                }
        } catch (error: SecurityException) {
            Log.e(TAG, "Fused current location permission error", error)
            useLastLocation("permission exception=${error.message}")
        } catch (error: RuntimeException) {
            Log.e(TAG, "Fused current location request exception", error)
            useLastLocation("request exception=${error.message}")
        }

        mainHandler.postDelayed({
            Log.w(TAG, "Fused location acquisition timeout after ${LOCATION_TIMEOUT_MS}ms")
            cancellationTokenSource.cancel()
            useLastLocation("timeout")
        }, LOCATION_TIMEOUT_MS)

        return {
            if (!completed) {
                Log.d(TAG, "Location request cancelled by caller")
                completed = true
                mainHandler.removeCallbacksAndMessages(null)
                cancellationTokenSource.cancel()
            }
        }
    }

    private fun legacyRequestCurrentApproximateLocation(
        context: Context,
        onResult: (ApproximateLocation?) -> Unit
    ): () -> Unit {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            onResult(null)
            return {}
        }

        return requestCurrentApproximateLocation(context, onResult)
    }

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
        }.onFailure { error ->
            Log.e(
                TAG,
                "Geocoder failed for latitude=${location.latitude}, longitude=${location.longitude}",
                error
            )
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