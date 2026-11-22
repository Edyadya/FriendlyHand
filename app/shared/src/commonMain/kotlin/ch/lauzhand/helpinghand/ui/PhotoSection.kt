package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.photo.rememberPhotoPicker
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The photo controls, used when a person is added and on their profile (volunteers only).
 * A photo needs the person's OWN, separate consent: the checkbox comes first, the photo buttons only appear
 * once it is ticked, and unticking it removes the photo.
 */
@Composable
fun PhotoSection(
    consent: Boolean,
    photo: String?,
    onConsentChange: (Boolean) -> Unit,
    onPhotoChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberPhotoPicker { onPhotoChange(it) }
    val bitmap = rememberPhotoBitmap(photo)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.photo_title), style = MaterialTheme.typography.titleSmall)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(consent, onCheckedChange = onConsentChange)
            Text(stringResource(Res.string.photo_consent), style = MaterialTheme.typography.bodyLarge)
        }
        Text(
            stringResource(Res.string.photo_consent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (consent) {
            if (bitmap != null) {
                Image(bitmap, contentDescription = null, modifier = Modifier.size(96.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            }
            if (picker.supported) {
                AppOutlinedButton(onClick = picker.pick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (photo == null) Res.string.photo_add else Res.string.photo_replace))
                }
                if (photo != null) {
                    AppOutlinedButton(onClick = { onPhotoChange(null) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.photo_remove))
                    }
                }
            } else {
                Text(stringResource(Res.string.photo_unsupported), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
