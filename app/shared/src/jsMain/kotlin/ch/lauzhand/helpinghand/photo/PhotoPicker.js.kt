package ch.lauzhand.helpinghand.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Web: choosing a photo is not supported yet, so the screens show a short note instead of the button.
@Composable
actual fun rememberPhotoPicker(onPicked: (String) -> Unit): PhotoPicker = remember { PhotoPicker(supported = false) {} }
