package ch.lauzhand.helpinghand.photo

import androidx.compose.runtime.Composable

/** [supported] is false where this platform cannot pick a photo yet; then [pick] does nothing. */
class PhotoPicker(val supported: Boolean, val pick: () -> Unit)

/** Largest side of a stored photo, in pixels. Small on purpose: it is shown as an avatar and sent to the server. */
const val PHOTO_MAX_SIDE = 480

/**
 * Lets the user choose a photo. [onPicked] receives it already shrunk to at most [PHOTO_MAX_SIDE] pixels,
 * as a base64 JPEG (what Person.photo holds). Platform-specific: Android's photo picker, Desktop's file
 * dialog. The web build does not support it yet.
 */
@Composable
expect fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPicker
