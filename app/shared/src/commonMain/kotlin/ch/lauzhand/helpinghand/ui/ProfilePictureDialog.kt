package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Account
import ch.lauzhand.helpinghand.model.AccountProfile
import ch.lauzhand.helpinghand.model.Gender
import ch.lauzhand.helpinghand.photo.rememberPhotoPicker
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private fun Gender.label(): StringResource = when (this) {
    Gender.FEMALE -> Res.string.gender_female
    Gender.MALE -> Res.string.gender_male
    Gender.NON_BINARY -> Res.string.gender_non_binary
    Gender.OTHER -> Res.string.gender_other
    Gender.PREFER_NOT_TO_SAY -> Res.string.gender_prefer_not
}

/**
 * "Your profile" for a patron or volunteer: a picture, an optional gender (chosen from a list) and an optional
 * city and country. Nothing is stored until Save is pressed; Cancel throws the changes away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePictureDialog(repo: HelpRepository, account: Account, onClose: () -> Unit) {
    val saved = remember(account.id) { repo.accountProfileOf(account.id) }
    var photo by remember(account.id) { mutableStateOf(repo.accountPhotoOf(account.id)) }
    var gender by remember(account.id) { mutableStateOf(saved.gender) }
    var city by remember(account.id) { mutableStateOf(saved.city) }
    var country by remember(account.id) { mutableStateOf(saved.country) }
    var genderOpen by remember { mutableStateOf(false) }
    val picker = rememberPhotoPicker { photo = it }

    AlertDialog(
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(Res.string.profile_picture_title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountAvatar(account.name, photo, 120.dp)
                if (picker.supported) {
                    AppButton(onClick = picker.pick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(stringResource(if (photo == null) Res.string.profile_picture_choose else Res.string.profile_picture_change))
                    }
                    if (photo != null) {
                        AppOutlinedButton(onClick = { photo = null }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                            Text(stringResource(Res.string.profile_picture_remove))
                        }
                    }
                } else {
                    Text(stringResource(Res.string.profile_picture_unsupported), style = MaterialTheme.typography.bodySmall)
                }

                // gender: a list to choose from, optional
                ExposedDropdownMenuBox(expanded = genderOpen, onExpandedChange = { genderOpen = it }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = gender?.let { stringResource(it.label()) } ?: stringResource(Res.string.profile_gender_none),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.profile_gender)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderOpen) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(expanded = genderOpen, onDismissRequest = { genderOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.profile_gender_none)) },
                            onClick = { gender = null; genderOpen = false },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                        Gender.entries.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(stringResource(g.label())) },
                                onClick = { gender = g; genderOpen = false },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    city, { city = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.profile_city)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    country, { country = it }, Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.profile_country)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Text(
                    stringResource(Res.string.profile_picture_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            AppButton(
                onClick = {
                    repo.setAccountPhoto(account.id, photo)
                    repo.setAccountProfile(account.id, AccountProfile(gender, city, country))
                    onClose()
                },
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(stringResource(Res.string.save)) }
        },
        dismissButton = { TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.cancel_action)) } },
    )
}
