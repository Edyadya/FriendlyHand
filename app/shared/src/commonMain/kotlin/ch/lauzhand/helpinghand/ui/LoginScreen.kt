package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.AccessCodes
import ch.lauzhand.helpinghand.model.Role
import helpinghand.app.shared.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Order shown on the login screen. */
private val LoginRoles = listOf(Role.DONOR, Role.VOLUNTEER)

private fun Role.info(): StringResource = when (this) {
    Role.DONOR -> Res.string.login_role_donor_info
    Role.VOLUNTEER -> Res.string.login_role_volunteer_info
}

/** Demo hackathon codes, shown as a hint under the code field. */
private fun Role.demoCode(): String? = when (this) {
    Role.DONOR -> null
    Role.VOLUNTEER -> AccessCodes.VOLUNTEER
}

private enum class LoginError { WRONG_CODE, NO_CONNECTION }

/**
 * First screen: pick a role, enter a name and (for Volunteer) the access code.
 * On success the repository's currentAccount is set, and AppShell shows the app.
 */
@Composable
fun LoginScreen(repo: HelpRepository) {
    var role by remember { mutableStateOf(Role.DONOR) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<LoginError?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val needsCode = role != Role.DONOR
    val canSubmit = !busy && name.isNotBlank() && (!needsCode || code.isNotBlank())

    fun submit() {
        if (!canSubmit) return
        busy = true
        error = null
        scope.launch {
            repo.login(name.trim(), role, code.trim().takeIf { needsCode })
                .onFailure {
                    // FakeRepository and RemoteRepository both report a wrong code as IllegalArgumentException
                    error = if (it is IllegalArgumentException) LoginError.WRONG_CODE else LoginError.NO_CONNECTION
                }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary)
            Text(stringResource(Res.string.login_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(Res.string.login_subtitle), style = MaterialTheme.typography.bodyLarge)

            // Two minimal icon tiles side by side; the selected role's one-line description sits below.
            Row(Modifier.selectableGroup().fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LoginRoles.forEach { r ->
                    RoleTile(r, selected = role == r, modifier = Modifier.weight(1f)) {
                        role = r
                        code = ""
                        error = null
                    }
                }
            }
            Text(
                stringResource(role.info()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(Res.string.login_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = if (needsCode) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )

            if (needsCode) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it; error = null },
                    label = { Text(stringResource(Res.string.login_code)) },
                    singleLine = true,
                    isError = error == LoginError.WRONG_CODE,
                    supportingText = {
                        role.demoCode()?.let { Text(stringResource(Res.string.login_code_hint, it)) }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            error?.let {
                Text(
                    stringResource(if (it == LoginError.WRONG_CODE) Res.string.login_wrong_code else Res.string.login_no_connection),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Button(
                onClick = { submit() },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text(stringResource(Res.string.login_button))
            }
        }
    }
}

@Composable
private fun RoleTile(role: Role, selected: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)
    val container by animateColorAsState(if (selected) colors.primaryContainer else Color.Transparent)
    val accent by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant)
    Column(
        modifier
            .heightIn(min = 120.dp)
            .clip(shape)
            .background(container)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .selectable(selected = selected, onClick = onSelect, role = SemanticsRole.RadioButton)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
    ) {
        RoleIcon(role, accent)
        Text(stringResource(role.label()), style = MaterialTheme.typography.titleMedium, color = accent)
    }
}
