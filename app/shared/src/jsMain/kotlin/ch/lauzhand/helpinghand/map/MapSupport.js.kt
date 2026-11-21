package ch.lauzhand.helpinghand.map

import org.jetbrains.skiko.wasm.onWasmReady
import org.maplibre.compose.browser.installMaplibreCompose

// Web JS: MapLibre loads and renders its first frame, but cannot composite into Compose's WebGL
// surface ("Compose has no current WebGL renderer"), so the map stays blank in the browser.
// The drawn ZoneMap is used on the web until that works. Set to true to try the live map again
// (MapPreview at /?map still shows it).
actual val isLiveMapSupported: Boolean = false

actual fun installMapSupport(content: () -> Unit) {
    // installMaplibreCompose needs the Skiko wasm module, so it must run inside onWasmReady
    onWasmReady {
        installMaplibreCompose()
        content()
    }
}
