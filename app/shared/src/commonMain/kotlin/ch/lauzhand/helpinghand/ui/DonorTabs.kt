package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** The donor's main tabs. */
enum class DonorTab(val label: StringResource) {
    CONTRIBUTIONS(Res.string.tab_contributions),
    MAP(Res.string.tab_map),
    ACHIEVEMENTS(Res.string.tab_achievements),
}

/** The donor tabs, at the bottom of the screen. */
@Composable
fun DonorBottomBar(selected: DonorTab, onSelect: (DonorTab) -> Unit) {
    // AppShell already pads for system bars, so no extra insets here
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, windowInsets = WindowInsets(0)) {
        DonorTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = { TabIcon(tab) },
                label = {
                    Text(
                        stringResource(tab.label),
                        style = MaterialTheme.typography.labelSmall,
                        // four tabs are narrow: let a long name (also in French) wrap onto a second line
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                // selected: primary icon + label on a soft primary pill (12 %); unselected: muted
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.app.navUnselected,
                    unselectedTextColor = MaterialTheme.app.navUnselected,
                ),
            )
        }
    }
}

/** Small line icons drawn on a Canvas, so no icon library is needed. Uses the current content colour. */
@Composable
private fun TabIcon(tab: DonorTab) {
    val color = LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w = 2.dp.toPx()
        when (tab) {
            DonorTab.CONTRIBUTIONS -> drawPaper(color, w)
            DonorTab.MAP -> drawPin(color, w)
            DonorTab.ACHIEVEMENTS -> drawMedal(color, w)
        }
    }
}

// a paper page: a sheet with a folded corner and a few lines of text
private fun DrawScope.drawPaper(color: Color, w: Float) {
    fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    val sheet = Path().apply {
        moveTo(p(0.2f, 0.08f).x, p(0.2f, 0.08f).y)
        lineTo(p(0.62f, 0.08f).x, p(0.62f, 0.08f).y)
        lineTo(p(0.84f, 0.3f).x, p(0.84f, 0.3f).y)
        lineTo(p(0.84f, 0.92f).x, p(0.84f, 0.92f).y)
        lineTo(p(0.2f, 0.92f).x, p(0.2f, 0.92f).y)
        close()
    }
    drawPath(sheet, color, style = Stroke(w))
    // the folded corner
    drawLine(color, p(0.62f, 0.08f), p(0.62f, 0.3f), w, StrokeCap.Round)
    drawLine(color, p(0.62f, 0.3f), p(0.84f, 0.3f), w, StrokeCap.Round)
    // text lines
    drawLine(color, p(0.33f, 0.52f), p(0.71f, 0.52f), w, StrokeCap.Round)
    drawLine(color, p(0.33f, 0.68f), p(0.71f, 0.68f), w, StrokeCap.Round)
}

// a map pin: rounded top, point at the bottom, hole in the middle
private fun DrawScope.drawPin(color: Color, w: Float) {
    val cx = size.width / 2
    val r = size.width * 0.3f
    val cy = size.height * 0.38f
    val pin = Path().apply {
        moveTo(cx, size.height * 0.92f)
        lineTo(cx - r * 0.93f, cy + r * 0.38f)
        arcTo(androidx.compose.ui.geometry.Rect(Offset(cx - r, cy - r), Size(2 * r, 2 * r)), 157.5f, 225f, false)
        close()
    }
    drawPath(pin, color, style = Stroke(w))
    drawCircle(color, r * 0.32f, Offset(cx, cy))
}

// a medal: ribbon and a round medal
private fun DrawScope.drawMedal(color: Color, w: Float) {
    val cx = size.width / 2
    drawLine(color, Offset(size.width * 0.3f, size.height * 0.08f), Offset(cx, size.height * 0.42f), w, StrokeCap.Round)
    drawLine(color, Offset(size.width * 0.7f, size.height * 0.08f), Offset(cx, size.height * 0.42f), w, StrokeCap.Round)
    drawCircle(color, size.width * 0.24f, Offset(cx, size.height * 0.66f), style = Stroke(w))
    drawCircle(color, size.width * 0.08f, Offset(cx, size.height * 0.66f))
}

// a heart: the charities tab
private fun DrawScope.drawHeart(color: Color, w: Float) {
    fun p(x: Float, y: Float) = Offset(size.width * x, size.height * y)
    val heart = Path().apply {
        moveTo(p(0.5f, 0.88f).x, p(0.5f, 0.88f).y)
        cubicTo(p(0.12f, 0.6f).x, p(0.12f, 0.6f).y, p(0.05f, 0.38f).x, p(0.05f, 0.38f).y, p(0.05f, 0.3f).x, p(0.05f, 0.3f).y)
        cubicTo(p(0.05f, 0.16f).x, p(0.05f, 0.16f).y, p(0.2f, 0.1f).x, p(0.2f, 0.1f).y, p(0.3f, 0.1f).x, p(0.3f, 0.1f).y)
        cubicTo(p(0.4f, 0.1f).x, p(0.4f, 0.1f).y, p(0.47f, 0.17f).x, p(0.47f, 0.17f).y, p(0.5f, 0.25f).x, p(0.5f, 0.25f).y)
        cubicTo(p(0.53f, 0.17f).x, p(0.53f, 0.17f).y, p(0.6f, 0.1f).x, p(0.6f, 0.1f).y, p(0.7f, 0.1f).x, p(0.7f, 0.1f).y)
        cubicTo(p(0.8f, 0.1f).x, p(0.8f, 0.1f).y, p(0.95f, 0.16f).x, p(0.95f, 0.16f).y, p(0.95f, 0.3f).x, p(0.95f, 0.3f).y)
        cubicTo(p(0.95f, 0.38f).x, p(0.95f, 0.38f).y, p(0.88f, 0.6f).x, p(0.88f, 0.6f).y, p(0.5f, 0.88f).x, p(0.5f, 0.88f).y)
        close()
    }
    drawPath(heart, color, style = Stroke(w))
}
