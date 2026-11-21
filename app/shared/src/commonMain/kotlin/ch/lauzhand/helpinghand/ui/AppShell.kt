package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Image
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
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Simple state-based navigation, no library. */
sealed interface Screen {
    data object Home : Screen
    data class Profile(val personId: String) : Screen
    data object Onboarding : Screen
    data object Rewards : Screen
    data class AddNeed(val personId: String) : Screen
}

internal fun Role.label(): StringResource = when (this) {
    Role.VOLUNTEER -> Res.string.role_volunteer
    Role.DONOR -> Res.string.role_donor
}

@Composable
fun AppShell(repo: HelpRepository) {
    val account = repo.currentAccount
    if (account == null) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.safeContentPadding().fillMaxSize()) { LoginScreen(repo) }
        }
        return
    }
    // The role comes from the logged-in account; logging out returns to LoginScreen.
    val role = account.role
    var screen by remember(account) { mutableStateOf<Screen>(Screen.Home) }
    // Donors get three tabs in a bottom bar (all screen sizes); Home shows the selected one.
    var tab by remember(account) { mutableStateOf(DonorTab.MAP) }
    val selectTab: (DonorTab) -> Unit = { tab = it; screen = Screen.Home }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.safeContentPadding().fillMaxSize()) {
            AppTopBar(account.name, stringResource(role.label()), onLogout = { repo.logout() })
            val showTabs = role == Role.DONOR
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // Never show a screen the role isn't allowed on (same rule the role chips used).
                    val allowed = !((role != Role.VOLUNTEER && (screen is Screen.Onboarding || screen is Screen.AddNeed)) ||
                        (role != Role.DONOR && screen is Screen.Rewards))
                    when (val s = if (allowed) screen else Screen.Home) {
                        Screen.Home -> when (if (showTabs) tab else DonorTab.MAP) {
                            DonorTab.MAP -> HomeScreen(
                                repo, role,
                                onOpenPerson = { screen = Screen.Profile(it) },
                                onNewPerson = { screen = Screen.Onboarding },
                                onRewards = { selectTab(DonorTab.ACHIEVEMENTS) },
                            )
                            DonorTab.CONTRIBUTIONS -> ContributionsScreen(repo, onOpenPerson = { screen = Screen.Profile(it) })
                            DonorTab.ACHIEVEMENTS -> AchievementsScreen(repo)
                        }
                        is Screen.Profile -> ProfileScreen(
                            repo, role, s.personId,
                            onBack = { screen = Screen.Home },
                            onAddNeed = { screen = Screen.AddNeed(s.personId) },
                        )
                        Screen.Rewards -> RewardScreen(repo, onBack = { screen = Screen.Home })
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
                if (showTabs) DonorBottomBar(tab, selectTab)
            }
        }
    }
}
