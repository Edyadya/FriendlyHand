package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.location.UserLocation
import ch.lauzhand.helpinghand.map.LausanneMap
import ch.lauzhand.helpinghand.map.isLiveMapSupported
import ch.lauzhand.helpinghand.model.City
import ch.lauzhand.helpinghand.model.Role
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * A map to pick the EXACT spot where a volunteer met someone: tap to place it, then confirm.
 * The real map is used where it works (Android, web); elsewhere the drawn map, where a tap is turned back into
 * latitude and longitude. [start] is shown as the first pin. Only volunteers ever see the result.
 */
@Composable
fun SpotPickerDialog(
    repo: HelpRepository,
    city: City,
    start: UserLocation?,
    onPick: (lat: Double, lng: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var picked by remember { mutableStateOf(start) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.padding(16.dp).fillMaxWidth().widthIn(max = 640.dp),
            shape = AppShape.card,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(Res.string.spot_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.spot_tap_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Box(Modifier.fillMaxWidth().height(340.dp).clip(RoundedCornerShape(16.dp))) {
                    if (isLiveMapSupported) {
                        LausanneMap(
                            zones = emptyList(), people = emptyList(), needs = emptyList(), role = Role.VOLUNTEER,
                            onZoneClick = {}, onPersonClick = {}, modifier = Modifier.fillMaxSize(),
                            userLocation = picked, centreOnUser = 0, city = city,
                            onMapClick = { lat, lng -> picked = UserLocation(lat, lng, 0f, isDemo = false) },
                        )
                    } else {
                        // drawn map: turn the tap position back into latitude and longitude
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val w = constraints.maxWidth.toFloat()
                            val h = constraints.maxHeight.toFloat()
                            Box(
                                Modifier.fillMaxSize().pointerInput(city.id) {
                                    detectTapGestures { tap ->
                                        val lng = city.west + (tap.x / w) * city.lonSpan
                                        val lat = city.north - (tap.y / h) * city.latSpan
                                        picked = UserLocation(lat, lng, 0f, isDemo = false)
                                    }
                                },
                            ) {
                                ZoneMap(repo, null, {}, Modifier.fillMaxSize(), role = Role.VOLUNTEER, userLocation = picked, city = city, showBubbles = false)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, androidx.compose.ui.Alignment.End)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.cancel_action)) }
                    AppButton(onClick = { picked?.let { onPick(it.lat, it.lng) } }, enabled = picked != null) {
                        Text(stringResource(Res.string.spot_use_this))
                    }
                }
            }
        }
    }
}
