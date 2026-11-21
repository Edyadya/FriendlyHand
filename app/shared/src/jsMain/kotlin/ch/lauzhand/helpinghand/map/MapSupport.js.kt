package ch.lauzhand.helpinghand.map

import org.maplibre.compose.browser.installMaplibreCompose

actual val isLiveMapSupported: Boolean = true

actual fun installMapSupport() = installMaplibreCompose()
