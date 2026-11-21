package ch.lauzhand.helpinghand.map

import org.jetbrains.skiko.wasm.onWasmReady
import org.maplibre.compose.browser.installMaplibreCompose

actual val isLiveMapSupported: Boolean = true

actual fun installMapSupport(content: () -> Unit) {
    // installMaplibreCompose needs the Skiko wasm module, so it must run inside onWasmReady
    onWasmReady {
        installMaplibreCompose()
        content()
    }
}
