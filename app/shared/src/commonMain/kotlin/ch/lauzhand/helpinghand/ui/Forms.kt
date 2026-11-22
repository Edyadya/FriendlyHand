package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_VOLUNTEER
import ch.lauzhand.helpinghand.data.newId
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.location.UserLocation
import ch.lauzhand.helpinghand.location.rememberUserLocation
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.City
import ch.lauzhand.helpinghand.model.nowMillis
import androidx.compose.material3.TextButton
import ch.lauzhand.helpinghand.model.NeedType
import ch.lauzhand.helpinghand.model.Person
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private fun String.digits() = filter { it.isDigit() }.take(4)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    repo: HelpRepository,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    /** The city shown on the map: the area list starts with its areas. */
    initialCity: City = Cities.LAUSANNE,
) {
    var cityId by remember { mutableStateOf(initialCity.id) }
    var firstName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf<String?>(null) }
    var usualTime by remember { mutableStateOf("") }
    var ownWords by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var photoConsent by remember { mutableStateOf(false) }
    var photo by remember { mutableStateOf<String?>(null) }
    // the exact spot where the volunteer met them (optional): their own position, or a point picked on the map
    var spot by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var pickingSpot by remember { mutableStateOf(false) }
    val me = rememberUserLocation()

    val canSave = consent && firstName.isNotBlank() && zoneId != null

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
        Text(stringResource(Res.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            firstName, { firstName = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.field_first_name)) }, singleLine = true,
        )
        OutlinedTextField(
            age, { age = it.digits() }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.field_age)) }, singleLine = true,
        )
        Text(stringResource(Res.string.field_zone), style = MaterialTheme.typography.titleMedium)
        // the areas of one city at a time (switch city here)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Cities.all.forEach { c ->
                FilterChip(
                    selected = cityId == c.id,
                    onClick = {
                        cityId = c.id
                        if (repo.zones.firstOrNull { it.id == zoneId }?.cityId != c.id) zoneId = null
                    },
                    label = { Text(c.name) },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repo.zones.filter { it.cityId == cityId }.forEach { z ->
                FilterChip(
                    selected = zoneId == z.id,
                    onClick = { zoneId = z.id },
                    label = { Text(z.name) },
                )
            }
        }
        OutlinedTextField(
            usualTime, { usualTime = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.field_usual_time)) }, singleLine = true,
        )
        OutlinedTextField(
            ownWords, { ownWords = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.field_own_words)) }, minLines = 2,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(consent, { consent = it })
            Text(stringResource(Res.string.consent_text), style = MaterialTheme.typography.bodyLarge)
        }
        // the exact spot (optional); only volunteers can ever see it
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.spot_section), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppOutlinedButton(
                    onClick = { me.location?.let { spot = it.lat to it.lng } },
                    enabled = me.location != null,
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(Res.string.spot_use_my_position)) }
                AppOutlinedButton(onClick = { pickingSpot = true }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.spot_pick_on_map))
                }
            }
            if (spot != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.spot_saved), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    TextButton(onClick = { spot = null }, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.spot_clear)) }
                }
            }
        }
        if (pickingSpot) {
            val city = repo.zones.firstOrNull { it.id == zoneId }?.let { Cities.byId(it.cityId) } ?: Cities.LAUSANNE
            SpotPickerDialog(
                repo, city, start = spot?.let { UserLocation(it.first, it.second, 0f, false) } ?: me.location,
                onPick = { lat, lng -> spot = lat to lng; pickingSpot = false },
                onDismiss = { pickingSpot = false },
            )
        }
        // a photo has its own, separate consent
        PhotoSection(
            consent = photoConsent,
            photo = photo,
            onConsentChange = { photoConsent = it; if (!it) photo = null },
            onPhotoChange = { photo = it },
        )
        AppButton(
            onClick = {
                val id = newId("p")
                repo.addPerson(
                    Person(
                        id = id,
                        firstName = firstName.trim(),
                        age = age.toIntOrNull(),
                        zoneId = zoneId!!,
                        usualTime = usualTime.trim(),
                        ownWords = ownWords.trim().ifBlank { null },
                        consentGiven = true,
                        createdByVolunteerId = ME_VOLUNTEER,
                        photo = photo.takeIf { photoConsent },
                        photoConsent = photoConsent,
                        lastSeenLat = spot?.first,
                        lastSeenLng = spot?.second,
                        lastSeenAtMillis = if (spot != null) nowMillis() else null,
                    )
                )
                onSaved(id)
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(Res.string.save)) }
    }
}

@Composable
fun AddNeedScreen(
    repo: HelpRepository,
    personId: String,
    onDone: () -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var big by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("") }

    val targetValue = target.toIntOrNull() ?: 0
    val canSave = title.isNotBlank() && (!big || targetValue > 0)

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onDone) { Text(stringResource(Res.string.back)) }
        Text(stringResource(Res.string.add_need), style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            title, { title = it }, Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.need_title_field)) }, singleLine = true,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(big, { big = it })
            Text(stringResource(Res.string.need_big_goal), style = MaterialTheme.typography.bodyLarge)
        }
        if (big) {
            OutlinedTextField(
                target, { target = it.digits() }, Modifier.fillMaxWidth(),
                label = { Text(stringResource(Res.string.need_target_chf)) }, singleLine = true,
            )
        }
        AppButton(
            onClick = {
                repo.addNeed(
                    Need(
                        id = newId("n"),
                        personId = personId,
                        type = if (big) NeedType.BIG else NeedType.SMALL,
                        title = title.trim(),
                        targetAmountChf = if (big) targetValue else null,
                    )
                )
                onDone()
            },
            enabled = canSave,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text(stringResource(Res.string.save)) }
    }
}
