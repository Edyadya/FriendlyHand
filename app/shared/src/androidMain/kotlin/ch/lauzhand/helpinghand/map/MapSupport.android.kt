package ch.lauzhand.helpinghand.map

actual val isLiveMapSupported: Boolean = true

actual fun installMapSupport(content: () -> Unit) = content()
