package com.lu4p.fokuslauncher.ui.home

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.location.LocationManagerCompat
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

internal class WeatherLocationSource(private val manager: LocationManager) {
    @SuppressLint("MissingPermission")
    suspend fun getLocation(): Location? {
        val current = try {
            withTimeoutOrNull(10_000L) {
                if (!manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return@withTimeoutOrNull null
                suspendCancellableCoroutine<Location?> { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(
                        manager,
                        LocationManager.NETWORK_PROVIDER,
                        signal,
                        Executor { it.run() },
                    ) { location -> continuation.resume(location) }
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
        return current ?: lastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: lastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lastKnownLocation(LocationManager.PASSIVE_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(provider: String): Location? = try {
        manager.getLastKnownLocation(provider)
    } catch (_: SecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
