package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import ch.lauzhand.helpinghand.model.Person
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/** Decodes a base64 JPEG (Person.photo), or null if there is none or it cannot be read. */
@OptIn(ExperimentalEncodingApi::class)
@Composable
fun rememberPhotoBitmap(photo: String?): ImageBitmap? = remember(photo) {
    photo?.let { runCatching { Base64.decode(it).decodeToImageBitmap() }.getOrNull() }
}

/** The person's photo, or null: only shown when they gave their own consent for it. */
@Composable
fun rememberPersonPhoto(person: Person): ImageBitmap? =
    rememberPhotoBitmap(person.photo.takeIf { person.photoConsent })

/** A round avatar: the photo if there is one (with consent), otherwise their initial on a soft background. */
@Composable
fun PersonAvatar(person: Person, size: Dp, modifier: Modifier = Modifier) {
    val photo = rememberPersonPhoto(person)
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (photo != null) {
            Image(photo, contentDescription = null, modifier = Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Text(person.firstName.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}


/** A round avatar for the logged-in account: their own profile picture, or their initial. */
@Composable
fun AccountAvatar(name: String, photo: String?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = rememberPhotoBitmap(photo)
    Box(
        modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.size(size), contentScale = ContentScale.Crop)
        } else {
            Text(name.trim().take(1).uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
