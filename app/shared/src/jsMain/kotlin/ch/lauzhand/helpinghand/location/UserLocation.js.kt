package ch.lauzhand.helpinghand.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// No location API here: use the fixed demo position (see DemoLocation).
@Composable
actual fun rememberUserLocation(): LocationState = remember { demoLocationState() }
