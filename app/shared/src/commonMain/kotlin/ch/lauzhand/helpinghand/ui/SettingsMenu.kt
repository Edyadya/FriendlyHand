package ch.lauzhand.helpinghand.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.settings.AppLanguage
import ch.lauzhand.helpinghand.settings.LocalAppSettings
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The pages behind the menu. */
enum class SettingsPage { PARTNERS, TERMS, ABOUT }

/**
 * The content of the settings menu, written once. The side bar shows exactly this: language, dark mode, partners, terms, about, and log out.
 */
@Composable
fun SettingsMenuContent(
    /** The account's role under the name: Volunteer, Patron or Guest patron. */
    accountRole: String,
    onOpenPage: (SettingsPage) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    /** The account's name and profile picture (tap to change it). */
    accountName: String = "",
    accountPhoto: String? = null,
    onProfilePicture: () -> Unit = {},
    /** Screens opened from the menu (charities, giveaways, ...): a label and what to do. */
    features: List<Pair<StringResource, () -> Unit>> = emptyList(),
) {
    val settings = LocalAppSettings.current
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onProfilePicture).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AccountAvatar(accountName, accountPhoto, 44.dp)
            Column(Modifier.weight(1f)) {
                // the name, and below it the role
                Text(
                    accountName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    accountRole,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (settings != null) {
            // Language: a segmented control, applied at once and remembered. It sits right under the profile,
            // above the charities and the other pages.
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEachIndexed { i, lang ->
                        SegmentedButton(
                            selected = settings.effectiveLanguage == lang,
                            onClick = { settings.chooseLanguage(lang) },
                            shape = SegmentedButtonDefaults.itemShape(i, AppLanguage.entries.size),
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text(stringResource(lang.label()), maxLines = 1)
                        }
                    }
                }
            }
            // Dark mode: a switch; it starts from the system setting and the choice is remembered
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(Res.string.settings_dark_mode), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = MaterialTheme.isDark, onCheckedChange = { settings.chooseDarkMode(it) })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        features.forEach { (label, onClick) -> MenuRow(label, onClick = onClick) }
        if (features.isNotEmpty()) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        MenuRow(Res.string.settings_partners) { onOpenPage(SettingsPage.PARTNERS) }
        MenuRow(Res.string.settings_terms) { onOpenPage(SettingsPage.TERMS) }
        MenuRow(Res.string.settings_about) { onOpenPage(SettingsPage.ABOUT) }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        MenuRow(Res.string.logout, emphasised = true, onClick = onLogout)
    }
}

private fun AppLanguage.label(): StringResource = when (this) {
    AppLanguage.ENGLISH -> Res.string.language_english
    AppLanguage.FRENCH -> Res.string.language_french
}

@Composable
private fun MenuRow(label: StringResource, emphasised: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(label),
            style = MaterialTheme.typography.bodyLarge,
            color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Phone presentation: a panel that slides in from the right edge over a dimmed screen. It closes on a tap
 * outside it, on a swipe to the right, or from the menu itself.
 */
@Composable
fun SettingsDrawer(open: Boolean, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val visible = remember { MutableTransitionState(false) }
    visible.targetState = open
    // a smooth ease; no animation at all when the user has asked for reduced motion
    val slideSpec: FiniteAnimationSpec<IntOffset> = if (rememberReduceMotion()) snap() else tween(durationMillis = 320, easing = FastOutSlowInEasing)
    if (!visible.currentState && !visible.targetState) return // fully closed

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visibleState = visible, enter = fadeIn(), exit = fadeOut()) {
            // the dimmed screen behind the panel: a tap on it closes the drawer
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.40f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            )
        }
        AnimatedVisibility(
            visibleState = visible,
            // slides in from the right edge to the left, and back out to the right
            enter = slideInHorizontally(animationSpec = slideSpec) { it },
            exit = slideOutHorizontally(animationSpec = slideSpec) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            var drag by remember { mutableStateOf(0f) }
            Surface(
                Modifier.fillMaxHeight().widthIn(max = 340.dp).fillMaxWidth(0.86f)
                    .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) { drag.coerceAtLeast(0f).toDp() })
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onDragEnd = { if (drag > 120f) onDismiss(); drag = 0f },
                            onDragCancel = { drag = 0f },
                        ) { _, dx -> drag += dx }
                    },
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
            ) {
                Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = 8.dp)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(Res.string.settings_title), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                        // an X instead of the word; screen readers still read it as "Close"
                        val closeLabel = stringResource(Res.string.settings_close)
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onDismiss)
                                .semantics { contentDescription = closeLabel; role = SemanticsRole.Button },
                            contentAlignment = Alignment.Center,
                        ) { CloseIcon(MaterialTheme.colorScheme.primary) }
                    }
                    content()
                }
            }
        }
    }
}

/** An X, drawn (no icon library needed). */
@Composable
private fun CloseIcon(color: Color) {
    androidx.compose.foundation.Canvas(Modifier.size(20.dp)) {
        val w = 2.5.dp.toPx()
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.1f), androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.9f), w, androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(color, androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f), androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f), w, androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

/** The dialog for a menu page (partners, terms, about). Scrolls, and its text wraps, so long French strings are fine. */
@Composable
fun SettingsPageDialog(page: SettingsPage, onClose: () -> Unit) {
    val title = when (page) {
        SettingsPage.PARTNERS -> Res.string.settings_partners
        SettingsPage.TERMS -> Res.string.settings_terms
        SettingsPage.ABOUT -> Res.string.settings_about
    }
    AlertDialog(
        onDismissRequest = onClose,
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(title), style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (page) {
                    SettingsPage.PARTNERS -> ProjectPartners.forEachIndexed { i, p ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        PartnerRow(p)
                    }
                    SettingsPage.TERMS -> Text(stringResource(Res.string.terms_body), style = MaterialTheme.typography.bodyMedium)
                    SettingsPage.ABOUT -> {
                        Text(stringResource(Res.string.about_body), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(Res.string.about_credits), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(Res.string.about_shared_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FooterLine(Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(Res.string.settings_close)) } },
    )
}

// ---- partners: data-driven, so adding one is a single line ----

private class ProjectPartner(
    val name: StringResource,
    val description: StringResource,
    val logo: DrawableResource,
    /** A small heading above [details], or null. */
    val detailsTitle: StringResource? = null,
    /** More paragraphs about the partner. */
    val details: List<StringResource> = emptyList(),
)

private val ProjectPartners = listOf(
    ProjectPartner(
        Res.string.partner_imc_name, Res.string.partner_imc_desc, Res.drawable.imc_logo,
        detailsTitle = Res.string.partner_imc_giving_title,
        details = listOf(Res.string.partner_imc_giving_1, Res.string.partner_imc_giving_2, Res.string.partner_imc_giving_3),
    ),
    ProjectPartner(
        Res.string.partner_jetbrains_name, Res.string.partner_jetbrains_desc, Res.drawable.jetbrains_logo,
        details = listOf(Res.string.partner_jetbrains_about_1, Res.string.partner_jetbrains_about_2, Res.string.partner_jetbrains_about_3),
    ),
)

@Composable
private fun PartnerRow(p: ProjectPartner) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Image(painterResource(p.logo), contentDescription = null, modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(p.name), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(p.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        p.detailsTitle?.let { Text(stringResource(it), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
        p.details.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    }
}
