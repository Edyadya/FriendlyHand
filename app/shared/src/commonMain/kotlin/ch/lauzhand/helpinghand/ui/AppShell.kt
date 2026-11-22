package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.communityImpact
import ch.lauzhand.helpinghand.model.deliveriesToCelebrate
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.isGuest
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
    var pictureOpen by mutableStateOf(false)
}

/** What is on screen: the screen, and for Home which bottom tab (null for other screens). Used for the transitions. */
private data class ShellDest(val screen: Screen, val tab: DonorTab?)

/** Simple state-based navigation, no library. */
sealed interface Screen {
    data object Home : Screen
    data class Profile(val personId: String) : Screen
    data object Onboarding : Screen
    data object Rewards : Screen
    data object Charities : Screen
    data object Giveaways : Screen
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
        // The menu is a side bar that slides in from the right, on every screen size.
        val menuContent: @Composable () -> Unit = {
            SettingsMenuContent(
                accountRole = stringResource(if (account.isGuest) Res.string.role_guest else role.label()),
                onOpenPage = { menuOpen = false; settingsPage = it },
                onLogout = { menuOpen = false; repo.logout() },
                accountName = account.name,
                accountPhoto = repo.accountPhotoOf(account.id),
                onProfilePicture = { menuOpen = false; shell.pictureOpen = true },
                // volunteers have just one entry there (Charities), which reads better without divider lines around it
                featureDividers = role != Role.VOLUNTEER,
                features = buildList {
                    add(Res.string.menu_charities to { menuOpen = false; screen = Screen.Charities })
                    if (role == Role.DONOR) add(Res.string.menu_giveaways to { menuOpen = false; screen = Screen.Giveaways })
                },
            )
        }
        Column(Modifier.fillMaxSize()) {
            AppTopBar(
                onMenuClick = { menuOpen = !menuOpen },
                profile = { AccountAvatar(account.name, repo.accountPhotoOf(account.id), 32.dp, Modifier.clickable { shell.pictureOpen = true }) },
            )
            // Everyone gets the same three tabs at the bottom: Deliveries, Map, Rewards
            val showTabs = true
            Column(Modifier.weight(1f).fillMaxWidth()) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    // Never show a screen the role isn't allowed on (same rule the role chips used).
                    val allowed = !((role != Role.VOLUNTEER && (screen is Screen.Onboarding || screen is Screen.AddNeed)) ||
                        (role != Role.DONOR && (screen is Screen.Rewards || screen is Screen.Giveaways)))
                    val shown = if (allowed) screen else Screen.Home
                    val reduceMotion = rememberReduceMotion()
                    // Smooth changes: between the bottom tabs the page slides a little in the direction of the tab and
                    // fades; opening or leaving a screen (a profile, a form) cross-fades. No motion if reduced.
                    AnimatedContent(
                        targetState = ShellDest(shown, if (shown == Screen.Home) tab else null),
                        transitionSpec = {
                            val from = initialState.tab
                            val to = targetState.tab
                            when {
                                reduceMotion -> EnterTransition.None togetherWith ExitTransition.None
                                from != null && to != null && from != to -> {
                                    val dir = if (to.ordinal > from.ordinal) 1 else -1
                                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 6 * dir } + fadeIn(tween(240, delayMillis = 60))) togetherWith
                                        (slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 6 * dir } + fadeOut(tween(140)))
                                }
                                else -> fadeIn(tween(240, delayMillis = 60)) togetherWith fadeOut(tween(140))
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        label = "screen",
                    ) { dest ->
                    when (val s = dest.screen) {
                        Screen.Home -> when (dest.tab ?: DonorTab.MAP) {
                            DonorTab.MAP -> HomeScreen(
                                repo, role,
                                city = shell.city,
                                onCityChange = { shell.city = it },
                                onOpenPerson = { screen = Screen.Profile(it) },
                                onNewPerson = { screen = Screen.Onboarding },
                                onRewards = { selectTab(DonorTab.ACHIEVEMENTS) },
                            )
                            // Deliveries: a patron's own gifts, or the volunteers' order book
                            DonorTab.CONTRIBUTIONS ->
                                if (role == Role.DONOR) ContributionsScreen(repo, onOpenPerson = { screen = Screen.Profile(it) })
                                else OrdersScreen(repo, account.id, account.name, onOpenPerson = { screen = Screen.Profile(it) })
                            DonorTab.ACHIEVEMENTS -> if (role == Role.DONOR) AchievementsScreen(repo) else VolunteerRewardsScreen(repo, account.id)
                        }
                        is Screen.Profile -> ProfileScreen(
                            repo, role, s.personId,
                            onBack = { screen = Screen.Home },
                            onAddNeed = { screen = Screen.AddNeed(s.personId) },
                        )
                        Screen.Rewards -> RewardScreen(repo, onBack = { screen = Screen.Home })
                        Screen.Charities -> CharitiesScreen(shell.city, { shell.city = it }, onBack = { screen = Screen.Home })
                        Screen.Giveaways -> GiveawaysScreen(repo, onBack = { screen = Screen.Home })
                        Screen.Onboarding -> OnboardingScreen(
                            repo,
                            initialCity = shell.city,
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
                if (showTabs) DonorBottomBar(tab, selectTab)
            }
        }
        SettingsDrawer(open = menuOpen, onDismiss = { menuOpen = false }) { menuContent() }
        settingsPage?.let { SettingsPageDialog(it) { settingsPage = null } }
        if (shell.pictureOpen) ProfilePictureDialog(repo, account) { shell.pictureOpen = false }
        // A patron whose delivery a volunteer has confirmed sees where it went, once: "Your socks reached Luc near Flon"
        if (role == Role.DONOR) {
            val fresh = deliveriesToCelebrate(repo.contributionsOf(ME_DONOR), repo.needs) { repo.isCelebrated(it) }
            fresh.lastOrNull()?.let { need ->
                val person = repo.people.firstOrNull { it.id == need.personId }
                ConfirmedMoment(
                    impactLine = reachedLine(need, person?.firstName.orEmpty(), repo.zones.firstOrNull { it.id == person?.zoneId }?.name.orEmpty(), mine = true),
                    impact = communityImpact(repo.needs, repo.giveaways),
                ) { repo.markCelebrated(fresh.map { it.id }) }
            }
        }
        }
    }
}
