package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Light top bar: no heavy fill, a small logo on the left and the menu (hamburger) icon on the right.
 * The settings side bar slides in from the right when that icon is pressed.
 * [menuAnchor] is drawn inside the same box as the icon (not used by the side bar, kept for callers).
 */
@Composable
fun AppTopBar(onMenuClick: () -> Unit, modifier: Modifier = Modifier, profile: @Composable () -> Unit = {}, menuAnchor: @Composable () -> Unit = {}) {
    val label = stringResource(Res.string.settings_open_description)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(30.dp))
        Spacer(Modifier.weight(1f))
        profile()
        Spacer(Modifier.width(4.dp))
        Box {
            // 48 dp touch target around the 24 dp icon
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onMenuClick)
                    .semantics { contentDescription = label; role = Role.Button },
                contentAlignment = Alignment.Center,
            ) { HamburgerIcon(MaterialTheme.colorScheme.primary) }
            menuAnchor()
        }
    }
}

/** Three lines. Drawn, so no icon library is needed. */
@Composable
private fun HamburgerIcon(color: androidx.compose.ui.graphics.Color) {
    Canvas(Modifier.size(24.dp)) {
        val w = 2.5.dp.toPx()
        for (i in 0..2) {
            val y = size.height * (0.22f + 0.28f * i)
            drawLine(color, Offset(size.width * 0.08f, y), Offset(size.width * 0.92f, y), w, StrokeCap.Round)
        }
    }
}
