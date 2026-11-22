package ch.lauzhand.helpinghand.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

// Defined in index.html: opens the browser's file dialog and hands back a small base64 JPEG.
private fun pickInBrowser(maxSide: Int, onDone: (String) -> Unit) {
    js("window.helpingHandPickPhoto(maxSide, onDone)")
}

/** Web (JS): the browser's file dialog. */
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPicker {
    val latest by rememberUpdatedState(onPicked)
    return remember { PhotoPicker(supported = true) { pickInBrowser(PHOTO_MAX_SIDE) { latest(it) } } }
}
