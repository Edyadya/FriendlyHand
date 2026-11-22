package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Account
import ch.lauzhand.helpinghand.photo.rememberPhotoPicker
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** A patron or volunteer chooses their own profile picture. It is their own picture, so no extra consent is needed. */
@Composable
fun ProfilePictureDialog(repo: HelpRepository, account: Account, onClose: () -> Unit) {
    val photo = repo.accountPhotoOf(account.id)
    val picker = rememberPhotoPicker { repo.setAccountPhoto(account.id, it) }
    AlertDialog(
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(Res.string.profile_picture_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountAvatar(account.name, photo, 120.dp)
                Text(stringResource(Res.string.profile_picture_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (picker.supported) {
                    AppButton(onClick = picker.pick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(if (photo == null) Res.string.profile_picture_choose else Res.string.profile_picture_change))
                    }
                    if (photo != null) {
                        AppOutlinedButton(onClick = { repo.setAccountPhoto(account.id, null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(Res.string.profile_picture_remove))
                        }
                    }
                } else {
                    Text(stringResource(Res.string.profile_picture_unsupported), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.settings_close)) } },
    )
}
