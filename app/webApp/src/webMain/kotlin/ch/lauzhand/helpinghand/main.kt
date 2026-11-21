package ch.lauzhand.helpinghand

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import ch.lauzhand.helpinghand.map.MapPreview
import ch.lauzhand.helpinghand.map.installMapSupport
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    installMapSupport() // web JS only, needed before the viewport for MapLibre
    ComposeViewport {
        // open the page with ?map to see the map preview
        if (window.location.search.contains("map")) MapPreview() else App()
    }
}
