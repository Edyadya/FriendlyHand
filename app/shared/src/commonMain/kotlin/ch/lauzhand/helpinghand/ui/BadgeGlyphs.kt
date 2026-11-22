package ch.lauzhand.helpinghand.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The flat vector pictures on the badge medallions. Each badge has its own: hands, people, coins in growing stacks,
 * a flag, map pins, a speech bubble with a heart, parcels, a truck, bags and a cart. They are drawn on a Canvas, so no
 * icon library is needed, and recoloured by the theme.
 */
enum class BadgeGlyph {
    HAND, PEOPLE, CROWD, HAND_ITEM, FLAG,
    COINS_1, COINS_2, COINS_3, COINS_4, COINS_5,
    PIN, ROUTE, SPEECH_HEART,
    PARCEL, PARCELS, TRUCK, TRUCK_STAR, BAG, CART, HANDS,
}

/** Draws [g] in a square of the current size: [ink] for the main shapes, [soft] for the secondary ones, [cut] for the details cut out of a shape. */
fun DrawScope.drawGlyph(g: BadgeGlyph, ink: Color, soft: Color, cut: Color = soft) {
    when (g) {
        BadgeGlyph.HAND -> hand(0.5f, 0.42f, 1f, ink, soft)
        BadgeGlyph.HANDS -> { hand(0.33f, 0.5f, 0.68f, ink, soft); hand(0.67f, 0.42f, 0.68f, soft, ink) }
        BadgeGlyph.PEOPLE -> { person(0.25f, 0.48f, 0.8f, soft); person(0.75f, 0.48f, 0.8f, soft); person(0.5f, 0.42f, 1f, ink) }
        BadgeGlyph.CROWD -> {
            person(0.2f, 0.36f, 0.6f, soft); person(0.5f, 0.34f, 0.6f, soft); person(0.8f, 0.36f, 0.6f, soft)
            person(0.35f, 0.62f, 0.7f, ink); person(0.65f, 0.62f, 0.7f, ink)
        }
        BadgeGlyph.HAND_ITEM -> {
            heart(0.5f, 0.3f, 0.34f, ink)
            rr(0.14f, 0.62f, 0.72f, 0.2f, 0.09f, soft) // the open palm underneath
            rr(0.1f, 0.7f, 0.3f, 0.12f, 0.06f, ink)
        }
        BadgeGlyph.FLAG -> {
            rr(0.24f, 0.14f, 0.07f, 0.74f, 0.035f, ink)
            val flag = Path().apply {
                moveTo(w(0.31f), h(0.16f)); lineTo(w(0.82f), h(0.28f)); lineTo(w(0.31f), h(0.48f)); close()
            }
            drawPath(flag, soft)
            circ(0.275f, 0.9f, 0.07f, ink)
        }
        BadgeGlyph.COINS_1 -> coins(1, ink, soft)
        BadgeGlyph.COINS_2 -> coins(2, ink, soft)
        BadgeGlyph.COINS_3 -> coins(3, ink, soft)
        BadgeGlyph.COINS_4 -> coins(4, ink, soft)
        BadgeGlyph.COINS_5 -> coins(5, ink, soft)
        BadgeGlyph.PIN -> pin(0.5f, 0.45f, 1f, ink, cut)
        BadgeGlyph.ROUTE -> {
            val path = Path().apply {
                moveTo(w(0.22f), h(0.82f)); cubicTo(w(0.5f), h(0.9f), w(0.3f), h(0.55f), w(0.6f), h(0.6f))
                cubicTo(w(0.85f), h(0.64f), w(0.72f), h(0.5f), w(0.74f), h(0.46f))
            }
            drawPath(path, soft, style = Stroke(w(0.05f), cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(w(0.07f), w(0.07f)))))
            circ(0.22f, 0.82f, 0.07f, ink)
            pin(0.74f, 0.26f, 0.62f, ink, cut)
        }
        BadgeGlyph.SPEECH_HEART -> {
            rr(0.14f, 0.18f, 0.72f, 0.5f, 0.16f, ink)
            val tail = Path().apply { moveTo(w(0.3f), h(0.64f)); lineTo(w(0.26f), h(0.86f)); lineTo(w(0.5f), h(0.66f)); close() }
            drawPath(tail, ink)
            heart(0.5f, 0.3f, 0.3f, cut)
        }
        BadgeGlyph.PARCEL -> parcel(0.5f, 0.5f, 0.66f, ink, soft, cut)
        BadgeGlyph.PARCELS -> { parcel(0.32f, 0.66f, 0.42f, soft, ink, cut); parcel(0.68f, 0.66f, 0.42f, soft, ink, cut); parcel(0.5f, 0.36f, 0.42f, ink, soft, cut) }
        BadgeGlyph.TRUCK -> truck(0.5f, ink, soft, cut)
        BadgeGlyph.TRUCK_STAR -> { truck(0.62f, ink, soft, cut); star(0.5f, 0.22f, 0.16f, soft) }
        BadgeGlyph.BAG -> {
            val body = Path().apply {
                moveTo(w(0.22f), h(0.34f)); lineTo(w(0.78f), h(0.34f)); lineTo(w(0.84f), h(0.86f)); lineTo(w(0.16f), h(0.86f)); close()
            }
            drawPath(body, ink)
            drawArc(soft, 180f, 180f, false, Offset(w(0.34f), h(0.12f)), Size(w(0.32f), h(0.4f)), style = Stroke(w(0.06f), cap = StrokeCap.Round))
        }
        BadgeGlyph.CART -> {
            val basket = Path().apply { moveTo(w(0.26f), h(0.32f)); lineTo(w(0.9f), h(0.32f)); lineTo(w(0.8f), h(0.62f)); lineTo(w(0.34f), h(0.62f)); close() }
            drawPath(basket, ink)
            val handle = Path().apply { moveTo(w(0.08f), h(0.16f)); lineTo(w(0.2f), h(0.16f)); lineTo(w(0.34f), h(0.62f)); lineTo(w(0.78f), h(0.62f)) }
            drawPath(handle, soft, style = Stroke(w(0.055f), cap = StrokeCap.Round))
            circ(0.4f, 0.78f, 0.07f, ink); circ(0.72f, 0.78f, 0.07f, ink)
        }
    }
}

// ---- small helpers, in 0..1 units of the (square) drawing area
private fun DrawScope.w(v: Float) = size.width * v
private fun DrawScope.h(v: Float) = size.height * v
private fun DrawScope.circ(cx: Float, cy: Float, r: Float, color: Color) = drawCircle(color, w(r), Offset(w(cx), h(cy)))
private fun DrawScope.rr(x: Float, y: Float, wd: Float, ht: Float, r: Float, color: Color) =
    drawRoundRect(color, Offset(w(x), h(y)), Size(w(wd), h(ht)), CornerRadius(w(r)))

private fun DrawScope.person(cx: Float, top: Float, s: Float, color: Color) {
    circ(cx, top - 0.02f * s, 0.1f * s, color)
    rr(cx - 0.16f * s, top + 0.1f * s, 0.32f * s, 0.26f * s, 0.12f * s, color)
}

private fun DrawScope.hand(cx: Float, top: Float, s: Float, ink: Color, soft: Color) {
    rr(cx - 0.17f * s, top + 0.3f * s, 0.34f * s, 0.34f * s, 0.09f * s, ink) // palm
    val heights = floatArrayOf(0.28f, 0.38f, 0.38f, 0.3f)
    for (i in 0..3) rr(cx - 0.17f * s + i * 0.0875f * s, top + (0.36f - heights[i]) * s + 0.02f * s, 0.075f * s, heights[i] * s, 0.037f * s, soft) // fingers
    rotate(-38f, Offset(w(cx - 0.14f * s), h(top + 0.52f * s))) { rr(cx - 0.3f * s, top + 0.46f * s, 0.22f * s, 0.085f * s, 0.04f * s, soft) } // thumb
}

private fun DrawScope.heart(cx: Float, cy: Float, s: Float, color: Color) {
    val p = Path().apply {
        moveTo(w(cx), h(cy + 0.45f * s))
        cubicTo(w(cx - 0.62f * s), h(cy), w(cx - 0.5f * s), h(cy - 0.5f * s), w(cx), h(cy - 0.22f * s))
        cubicTo(w(cx + 0.5f * s), h(cy - 0.5f * s), w(cx + 0.62f * s), h(cy), w(cx), h(cy + 0.45f * s))
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.coins(n: Int, ink: Color, soft: Color) {
    for (i in 0 until n) {
        val y = 0.8f - i * 0.12f
        rr(0.22f, y - 0.1f, 0.56f, 0.14f, 0.07f, if (i % 2 == 0) ink else soft)
        drawOval(if (i % 2 == 0) soft else ink, Offset(w(0.22f), h(y - 0.15f)), Size(w(0.56f), h(0.12f)))
    }
}

private fun DrawScope.pin(cx: Float, cy: Float, s: Float, ink: Color, soft: Color) {
    val r = 0.24f * s
    val p = Path().apply {
        moveTo(w(cx), h(cy + 0.42f * s))
        lineTo(w(cx - r * 0.9f), h(cy + 0.08f * s))
        arcTo(androidx.compose.ui.geometry.Rect(Offset(w(cx - r), h(cy - 0.4f * s)), Size(w(2 * r), h(2 * r * 1.0f))), 150f, 240f, false)
        close()
    }
    drawPath(p, ink)
    circ(cx, cy - 0.16f * s, 0.09f * s, soft)
}

private fun DrawScope.parcel(cx: Float, cy: Float, s: Float, ink: Color, soft: Color, cut: Color) {
    rr(cx - s / 2, cy - s / 2, s, s, 0.07f * s, ink)
    rr(cx - s / 2, cy - s / 2, s, 0.22f * s, 0.07f * s, soft) // lid
    rr(cx - 0.065f * s, cy - s / 2, 0.13f * s, s, 0f, cut)   // tape, cut out of the box
}

private fun DrawScope.truck(cy: Float, ink: Color, soft: Color, cut: Color) {
    rr(0.08f, cy - 0.26f, 0.5f, 0.38f, 0.04f, ink)       // the load
    val cab = Path().apply { moveTo(w(0.6f), h(cy - 0.14f)); lineTo(w(0.78f), h(cy - 0.14f)); lineTo(w(0.9f), h(cy)); lineTo(w(0.9f), h(cy + 0.12f)); lineTo(w(0.6f), h(cy + 0.12f)); close() }
    drawPath(cab, soft)
    circ(0.28f, cy + 0.16f, 0.085f, ink); circ(0.74f, cy + 0.16f, 0.085f, ink)
    circ(0.28f, cy + 0.16f, 0.035f, cut); circ(0.74f, cy + 0.16f, 0.035f, cut)
}

private fun DrawScope.star(cx: Float, cy: Float, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.42f
        val a = (-90f + i * 36f) * (PI.toFloat() / 180f)
        val x = w(cx + rad * cos(a)); val y = h(cy + rad * sin(a))
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}
