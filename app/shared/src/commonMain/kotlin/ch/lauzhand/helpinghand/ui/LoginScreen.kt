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
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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

    val app = MaterialTheme.app
    // No Surface here, so set the default text colour ourselves (readable in light and dark).
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    // Soft gradient: strong at the top behind the hero, fading to the plain background behind the form.
    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to app.gradientStart, 0.6f to app.gradientEnd, 1f to app.gradientEnd),
        ),
    ) {
        // Short screens get a compact layout (smaller headline and tiles, no secondary lines). The hero takes
        // up to 38 % of the height, but never more than what is left after the form, so with the extra
        // access-code field (Volunteer) the whole form and the Log in button still stay above the fold.
        val short = maxHeight < 700.dp
        val tight = short || needsCode
        val reserved = when {
            short && needsCode -> 520.dp
            short -> 430.dp
            needsCode -> 640.dp
            else -> 560.dp
        }
        val heroSize = minOf(maxWidth - 48.dp, maxHeight * 0.38f, maxHeight - reserved).coerceIn(96.dp, 320.dp)
        Column(
            Modifier.fillMaxSize().safeContentPadding().imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (tight) 10.dp else 16.dp),
                ) {
                    OrbitHero(heroSize)
                    Text(
                        buildAnnotatedString {
                            append(stringResource(Res.string.login_headline_plain) + " ")
                            // one word in a blue gradient (primary to accent, so it stays readable in dark mode)
                            withStyle(SpanStyle(brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, app.accent)))) {
                                append(stringResource(Res.string.login_headline_highlight))
                            }
                        },
                        style = if (short) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    if (!tight) {
                        Text(
                            stringResource(Res.string.login_subtitle),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }

                    // Two minimal icon tiles side by side; the selected role's one-line description sits below.
                    Row(Modifier.selectableGroup().fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LoginRoles.forEach { r ->
                            RoleTile(r, selected = role == r, compact = short, modifier = Modifier.weight(1f)) {
                                role = r
                                code = ""
                                error = null
                            }
                        }
                    }
                    if (!short) {
                        Text(
                            stringResource(role.info()),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

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
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Pinned to the bottom: the Log in button (full width), then the footer.
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppButton(
                    onClick = { submit() },
                    enabled = canSubmit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Text(stringResource(Res.string.login_button))
                }
                FooterLine()
            }
        }
    }
    }
}

@Composable
private fun RoleTile(role: Role, selected: Boolean, compact: Boolean, modifier: Modifier = Modifier, onSelect: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = AppShape.card
    // unselected: surface with an outline; selected: primary fill with onPrimary text and icon
    val container by animateColorAsState(if (selected) colors.primary else colors.surface)
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.onSurface)
    Column(
        modifier
            .heightIn(min = if (compact) 84.dp else 120.dp)
            .clip(shape)
            .background(container)
            .border(1.dp, if (selected) colors.primary else colors.outlineVariant, shape)
            .selectable(selected = selected, onClick = onSelect, role = SemanticsRole.RadioButton)
            .padding(if (compact) 10.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 10.dp, Alignment.CenterVertically),
    ) {
        RoleIcon(role, content, size = if (compact) 32.dp else 44.dp)
        Text(stringResource(role.label()), style = MaterialTheme.typography.titleMedium, color = content)
    }
}
