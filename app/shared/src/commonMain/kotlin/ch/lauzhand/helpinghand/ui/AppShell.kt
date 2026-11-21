package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Role
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Simple state-based navigation, no library. */
sealed interface Screen {
    data object Home : Screen
    data class Profile(val personId: String) : Screen
    data object Onboarding : Screen
    data class AddNeed(val personId: String) : Screen
}

private fun Role.label(): StringResource = when (this) {
    Role.VOLUNTEER -> Res.string.role_volunteer
    Role.DONOR -> Res.string.role_donor
    Role.NGO -> Res.string.role_ngo
}

@Composable
fun AppShell(repo: HelpRepository) {
    var role by remember { mutableStateOf(Role.VOLUNTEER) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeContentPadding().fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(Res.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                Role.entries.forEach { r ->
                    FilterChip(
                        selected = role == r,
                        onClick = {
                            role = r
                            if (r != Role.VOLUNTEER && (screen is Screen.Onboarding || screen is Screen.AddNeed)) screen = Screen.Home
                        },
                        label = { Text(stringResource(r.label())) },
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        repo, role,
                        onOpenPerson = { screen = Screen.Profile(it) },
                        onNewPerson = { screen = Screen.Onboarding },
                    )
                    is Screen.Profile -> ProfileScreen(
                        repo, role, s.personId,
                        onBack = { screen = Screen.Home },
                        onAddNeed = { screen = Screen.AddNeed(s.personId) },
                    )
                    Screen.Onboarding -> OnboardingScreen(
                        repo,
                        onBack = { screen = Screen.Home },
                        onSaved = { screen = Screen.Profile(it) },
                    )
                    is Screen.AddNeed -> AddNeedScreen(
                        repo, s.personId,
                        onDone = { screen = Screen.Profile(s.personId) },
                    )
                }
            }
        }
    }
}
