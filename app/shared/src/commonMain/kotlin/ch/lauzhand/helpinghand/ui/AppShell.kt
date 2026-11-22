package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.Role
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Where the user is in the app (screen, donor tab) and the state of the settings menu.
 * It lives ABOVE the language switch in App(), so changing the language rebuilds the texts but keeps the
 * user on the same screen with the menu still open.
 */
class ShellState {
    var screen by mutableStateOf<Screen>(Screen.Home)
    var tab by mutableStateOf(DonorTab.MAP)
    var city by mutableStateOf(Cities.LAUSANNE)
    var menuOpen by mutableStateOf(false)
    var settingsPage by mutableStateOf<SettingsPage?>(null)
}

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
fun AppShell(repo: HelpRepository, shell: ShellState = remember { ShellState() }) {
    val account = repo.currentAccount
    if (account == null) {
        LoginScreen(repo) // draws its own full-screen gradient and handles the safe areas itself
        return
    }
    // The role comes from the logged-in account; logging out returns to LoginScreen.
    val role = account.role
    var screen by shell::screen
    // Donors get three tabs in a bottom bar (all screen sizes); Home shows the selected one.
    var tab by shell::tab
    val selectTab: (DonorTab) -> Unit = { tab = it; screen = Screen.Home }

    // Settings menu: open state and the page (partners / terms / about) shown as a dialog
    var menuOpen by shell::menuOpen
    var settingsPage by shell::settingsPage

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.safeContentPadding().fillMaxSize()) {
        // The one check that picks the menu variant: a drop-down on wide windows, a right-hand drawer on phones.
        val wide = maxWidth >= SettingsDropdownMinWidth
        val menuContent: @Composable () -> Unit = {
            SettingsMenuContent(
                userLine = stringResource(Res.string.logged_in_as, account.name, stringResource(role.label())),
                onOpenPage = { menuOpen = false; settingsPage = it },
                onLogout = { menuOpen = false; repo.logout() },
            )
        }
        Column(Modifier.fillMaxSize()) {
            AppTopBar(onMenuClick = { menuOpen = !menuOpen }) {
                if (wide) {
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, modifier = Modifier.width(300.dp)) {
                        menuContent()
                    }
                }
            }
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
                                city = shell.city,
                                onCityChange = { shell.city = it },
                                onOpenPerson = { screen = Screen.Profile(it) },
                                onNewPerson = { screen = Screen.Onboarding },
                                onRewards = { selectTab(DonorTab.ACHIEVEMENTS) },
                            )
                            DonorTab.CONTRIBUTIONS -> ContributionsScreen(repo, onOpenPerson = { screen = Screen.Profile(it) })
                            DonorTab.CHARITIES -> CharitiesScreen(shell.city)
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
        if (!wide) SettingsDrawer(open = menuOpen, onDismiss = { menuOpen = false }) { menuContent() }
        settingsPage?.let { SettingsPageDialog(it) { settingsPage = null } }
        }
    }
}
