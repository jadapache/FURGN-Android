package com.furgnbi.android.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.os.Looper
import com.furgnbi.android.domain.model.LocationReading
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface GpsRepository {
    fun observeLocation(deviceId: String): Flow<LocationReading>
    suspend fun getLastKnownLocation(deviceId: String): LocationReading?
    val isGpsAvailable: StateFlow<Boolean>
}

@Singleton
class GpsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : GpsRepository {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _isGpsAvailable = MutableStateFlow(checkGpsEnabled())
    override val isGpsAvailable: StateFlow<Boolean> = _isGpsAvailable.asStateFlow()

    private fun checkGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    override fun observeLocation(deviceId: String): Flow<LocationReading> = callbackFlow {
        _isGpsAvailable.value = checkGpsEnabled()

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5_000L)
            .setMinUpdateIntervalMillis(2_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                _isGpsAvailable.value = true
                val reading = LocationReading(
                    deviceId = deviceId,
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else null,
                    heading = if (loc.hasBearing()) loc.bearing else null,
                    gpsAvailable = true,
                    capturedAt = loc.time
                )
                trySend(reading)
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                _isGpsAvailable.value = availability.isLocationAvailable
                if (!availability.isLocationAvailable) {
                    trySend(LocationReading(
                        deviceId = deviceId,
                        latitude = null,
                        longitude = null,
                        speedKmh = null,
                        heading = null,
                        gpsAvailable = false,
                        capturedAt = System.currentTimeMillis()
                    ))
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose { fusedClient.removeLocationUpdates(callback) }
    }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(deviceId: String): LocationReading? =
        suspendCancellableCoroutine { cont ->
            fusedClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(LocationReading(
                            deviceId = deviceId,
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            speedKmh = if (loc.hasSpeed()) loc.speed * 3.6f else null,
                            heading = if (loc.hasBearing()) loc.bearing else null,
                            gpsAvailable = true,
                            capturedAt = loc.time
                        ))
                    } else {
                        cont.resume(null)
                    }
                }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
}
