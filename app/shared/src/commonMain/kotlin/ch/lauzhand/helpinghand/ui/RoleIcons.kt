package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.Role

/** Minimal line icons for the two roles: a heart for the donor, a person for the volunteer. */
@Composable
fun RoleIcon(role: Role, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size)) {
        val w = size.toPx() * 0.06f
        val stroke = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val s = this.size.width
        fun x(f: Float) = f * s
        when (role) {
            Role.DONOR -> {
                val heart = Path().apply {
                    moveTo(x(0.5f), x(0.88f))
                    cubicTo(x(0.15f), x(0.62f), x(0.05f), x(0.40f), x(0.05f), x(0.30f))
                    cubicTo(x(0.05f), x(0.15f), x(0.20f), x(0.10f), x(0.30f), x(0.10f))
                    cubicTo(x(0.40f), x(0.10f), x(0.47f), x(0.17f), x(0.50f), x(0.25f))
                    cubicTo(x(0.53f), x(0.17f), x(0.60f), x(0.10f), x(0.70f), x(0.10f))
                    cubicTo(x(0.80f), x(0.10f), x(0.95f), x(0.15f), x(0.95f), x(0.30f))
                    cubicTo(x(0.95f), x(0.40f), x(0.85f), x(0.62f), x(0.50f), x(0.88f))
                    close()
                }
                drawPath(heart, color, style = stroke)
            }
            Role.VOLUNTEER -> {
                drawCircle(color, radius = x(0.16f), center = Offset(x(0.5f), x(0.30f)), style = stroke)
                val shoulders = Path().apply {
                    moveTo(x(0.16f), x(0.90f))
                    cubicTo(x(0.16f), x(0.62f), x(0.34f), x(0.55f), x(0.5f), x(0.55f))
                    cubicTo(x(0.66f), x(0.55f), x(0.84f), x(0.62f), x(0.84f), x(0.90f))
                }
                drawPath(shoulders, color, style = stroke)
            }
        }
    }
}
