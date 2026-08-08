package com.aldef.launcher.core

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * @param primary   baris utama — sedetail mungkin (nama jalan / tempat)
 * @param secondary baris pendukung — kelurahan, kota
 * @param accuracyM akurasi fix GPS dalam meter, null kalau tidak diketahui
 */
data class Place(
    val latitude: Double,
    val longitude: Double,
    val primary: String,
    val secondary: String = "",
    val accuracyM: Int? = null,
) {
    val coordinateText: String
        get() = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
}

/**
 * Lokasi lewat LocationManager (tanpa Play Services agar aplikasi tetap ringan).
 *
 * Berbeda dari versi awal yang hanya membaca last-known-location sekali, kelas
 * ini bisa berlangganan pembaruan berkala dan menerjemahkan koordinat menjadi
 * alamat sedetail mungkin — nama jalan, bukan sekadar kecamatan.
 */
class LocationRepository(private val context: Context) {

    private val fallback = Place(-6.2088, 106.8456, "Jakarta", "Indonesia")

    private val locationManager: LocationManager?
        get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Fix terakhir yang tersedia — dipakai supaya HUD langsung terisi saat dibuka. */
    fun lastKnownLocation(): Location? {
        if (!hasPermission()) return null
        val lm = locationManager ?: return null
        return try {
            listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
                .asSequence()
                .filter { lm.allProviders.contains(it) }
                .mapNotNull { lm.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        } catch (_: SecurityException) {
            null
        }
    }

    /**
     * Berlangganan pembaruan lokasi. GPS diminta memberi fix tiap [intervalMs];
     * provider jaringan dipakai sebagai cadangan di dalam ruangan.
     *
     * @return fungsi untuk menghentikan langganan.
     */
    fun startUpdates(intervalMs: Long, onLocation: (Location) -> Unit): () -> Unit {
        if (!hasPermission()) return {}
        val lm = locationManager ?: return {}

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onLocation(location)

            // Wajib di-override untuk API lama; tanpa ini sebagian ROM melempar
            // AbstractMethodError saat memanggil balik listener.
            @Deprecated("Tidak dipakai sejak API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { lm.allProviders.contains(it) }

        try {
            providers.forEach { provider ->
                lm.requestLocationUpdates(provider, intervalMs, 0f, listener)
            }
        } catch (_: SecurityException) {
            return {}
        }

        return { runCatching { lm.removeUpdates(listener) } }
    }

    suspend fun currentPlace(): Place = withContext(Dispatchers.IO) {
        val location = lastKnownLocation() ?: return@withContext fallback
        describe(location)
    }

    /** Terjemahkan koordinat menjadi alamat sedetail mungkin. */
    suspend fun describe(location: Location): Place = withContext(Dispatchers.IO) {
        val accuracy = if (location.hasAccuracy()) location.accuracy.toInt() else null
        val address = try {
            @Suppress("DEPRECATION")
            Geocoder(context, Locale.getDefault())
                .getFromLocation(location.latitude, location.longitude, 1)
                ?.firstOrNull()
        } catch (_: Exception) {
            null
        } ?: return@withContext Place(
            latitude = location.latitude,
            longitude = location.longitude,
            primary = "Koordinat",
            secondary = "",
            accuracyM = accuracy,
        )

        // Baris utama: seakurat mungkin — nomor + nama jalan, atau nama tempat.
        val street = listOfNotNull(address.thoroughfare, address.subThoroughfare)
            .takeIf { it.isNotEmpty() }
            ?.let { parts ->
                val road = address.thoroughfare
                val number = address.subThoroughfare
                if (road != null && number != null) "$road No. $number" else parts.first()
            }

        val primary = street
            ?: address.featureName?.takeIf { it.isNotBlank() && it != address.subLocality }
            ?: address.subLocality
            ?: address.locality
            ?: "Koordinat"

        // Baris pendukung: kelurahan → kota, tanpa pengulangan baris utama.
        val secondary = listOfNotNull(
            address.subLocality,
            address.locality?.takeIf { !it.startsWith("Kecamatan", ignoreCase = true) },
            address.subAdminArea,
        )
            .distinct()
            .filter { it != primary }
            .take(2)
            .joinToString(", ")

        Place(
            latitude = location.latitude,
            longitude = location.longitude,
            primary = primary,
            secondary = secondary,
            accuracyM = accuracy,
        )
    }
}
