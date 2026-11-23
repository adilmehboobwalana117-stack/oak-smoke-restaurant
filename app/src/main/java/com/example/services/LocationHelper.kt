package com.example.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.formatCoordinates
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class CapturedLocation(
    val lat: Double,
    val lng: Double,
    val label: String
)

object LocationHelper {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentLocation(context: Context): Result<CapturedLocation> {
        if (!hasLocationPermission(context)) {
            return Result.failure(
                SecurityException("Location permission is required to attach your current delivery location.")
            )
        }

        return try {
            val fused = LocationServices.getFusedLocationProviderClient(context)
            var loc: Location? = try {
                val cts = CancellationTokenSource()
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
            } catch (_: Exception) {
                null
            }

            if (loc == null) {
                loc = try {
                    fused.lastLocation.await()
                } catch (_: Exception) {
                    null
                }
            }

            if (loc == null) {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val providers = listOf(
                        LocationManager.GPS_PROVIDER,
                        LocationManager.NETWORK_PROVIDER,
                        LocationManager.PASSIVE_PROVIDER
                    )
                    loc = providers.firstNotNullOfOrNull { provider ->
                        try {
                            lm.getLastKnownLocation(provider)
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
            }

            if (loc == null) {
                return Result.failure(
                    IllegalStateException(
                        "Unable to read GPS signal. Please turn on Location/GPS in device settings and tap Retry."
                    )
                )
            }

            val lat = loc.latitude
            val lng = loc.longitude
            val label = reverseGeocodeLabel(context, lat, lng)
            Result.success(CapturedLocation(lat = lat, lng = lng, label = label))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun reverseGeocodeLabel(context: Context, lat: Double, lng: Double): String =
        withContext(Dispatchers.IO) {
            val fallback = "GPS: ${formatCoordinates(lat, lng)}"
            if (!Geocoder.isPresent()) return@withContext fallback
            try {
                val geocoder = Geocoder(context, Locale.US)
                val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Best-effort synchronous fallback via standard call when available
                    geocoder.getFromLocation(lat, lng, 1)
                } else {
                    geocoder.getFromLocation(lat, lng, 1)
                }
                val addr = addresses?.firstOrNull()
                val formatted = addr?.getAddressLine(0)?.trim()
                    ?: listOfNotNull(addr?.subLocality, addr?.locality, addr?.adminArea)
                        .filter { it.isNotBlank() }
                        .joinToString(", ")
                formatted.ifBlank { fallback }.take(180)
            } catch (_: Exception) {
                fallback
            }
        }

    fun openNavigationToLocation(context: Context, lat: Double, lng: Double, label: String): Result<Unit> {
        return try {
            val navUri = Uri.parse("google.navigation:q=$lat,$lng")
            val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                setPackage("com.google.android.apps.maps")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                return Result.success(Unit)
            }

            val encodedLabel = Uri.encode(label.ifBlank { "Oak & Smoke Customer Delivery" })
            val geoIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:$lat,$lng?q=$lat,$lng($encodedLabel)")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (geoIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(geoIntent)
                return Result.success(Unit)
            }

            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun dialPhoneNumber(context: Context, phone: String): Result<Unit> {
        val clean = phone.trim()
        if (clean.isBlank()) return Result.failure(IllegalArgumentException("No phone number available."))
        return try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(clean)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
