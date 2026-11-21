package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/** "just now", "12 min ago", "3 h ago". */
@Composable
fun timeAgo(thenMillis: Long, nowMillis: Long): String {
    val minutes = ((nowMillis - thenMillis) / 60_000L).coerceAtLeast(0).toInt()
    return when {
        minutes < 1 -> stringResource(Res.string.time_just_now)
        minutes < 60 -> stringResource(Res.string.time_minutes_ago, minutes)
        else -> stringResource(Res.string.time_hours_ago, minutes / 60)
    }
}
