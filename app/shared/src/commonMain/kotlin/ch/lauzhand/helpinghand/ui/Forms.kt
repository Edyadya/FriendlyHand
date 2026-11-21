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
) {
    var firstName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var zoneId by remember { mutableStateOf<String?>(null) }
    var usualTime by remember { mutableStateOf("") }
    var ownWords by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }

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
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repo.zones.forEach { z ->
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
