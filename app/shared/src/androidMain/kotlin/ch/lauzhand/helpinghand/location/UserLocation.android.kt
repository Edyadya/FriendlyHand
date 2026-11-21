package ch.lauzhand.helpinghand.location

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * Android: the real position from the system LocationManager (no Google Play services needed).
 * Asks for the location permission once, the first time it is used; if the user says no, the map keeps
 * working and [LocationState.status] is DENIED so the screen can show a hint.
 */
@Composable
actual fun rememberUserLocation(): LocationState {
    val context = LocalContext.current
    fun hasPermission() = PERMISSIONS.any { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    var permitted by remember { mutableStateOf(hasPermission()) }
    var answered by remember { mutableStateOf(permitted) }
    var location by remember { mutableStateOf<UserLocation?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permitted = result.values.any { it }
        answered = true
    }
    LaunchedEffect(Unit) { if (!permitted) launcher.launch(PERMISSIONS) }

    DisposableEffect(permitted) {
        if (!permitted) return@DisposableEffect onDispose {}
        val manager = context.getSystemService(LocationManager::class.java)
        val listener = object : LocationListener {
            override fun onLocationChanged(l: Location) {
                location = UserLocation(l.latitude, l.longitude, l.accuracy, isDemo = false)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        try {
            listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { manager.isProviderEnabled(it) }
                .forEach { provider ->
                    manager.getLastKnownLocation(provider)?.let { listener.onLocationChanged(it) }
                    manager.requestLocationUpdates(provider, 2_000L, 5f, listener, Looper.getMainLooper())
                }
        } catch (_: SecurityException) {
            // permission was withdrawn while the screen was open: keep the map working without a dot
        }
        onDispose { manager.removeUpdates(listener) }
    }

    val status = when {
        location != null -> LocationStatus.LOCATED
        answered && !permitted -> LocationStatus.DENIED
        else -> LocationStatus.SEARCHING
    }
    return LocationState(location, status) { launcher.launch(PERMISSIONS) }
}
