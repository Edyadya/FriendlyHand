package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.badgeSeed
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private enum class ArtStyle { RINGS, PETALS, FLOW }

/**
 * A personal badge pattern: rings, petals or a flow field, picked and shaped by a seed made from the person's name and
 * their count. The same name and count always draw the same pattern; another person, or another count, draws another.
 * It gets richer as the count grows. The first [goldDone] elements are gold: they mark the completed goals.
 */
@Composable
fun PersonalBadgeArt(name: String, count: Int, goldDone: Int, modifier: Modifier = Modifier) {
    val app = MaterialTheme.app
    val colors = MaterialTheme.colorScheme
    val palette = listOf(app.accent, colors.primary, app.pale, colors.onSurfaceVariant)
    val gold = app.gold
    val seed = badgeSeed(name, count)
    val style = ArtStyle.entries[(seed ushr 1).mod(ArtStyle.entries.size)]
    val description = stringResource(Res.string.art_title)
    Canvas(
        modifier.aspectRatio(1f).clip(CircleShape).background(colors.surface)
            .border(MaterialTheme.borderWidth, colors.outlineVariant, CircleShape)
            .semantics { contentDescription = description },
    ) {
        val rnd = Random(seed)
        val n = 5 + count.coerceIn(0, 10)
        val g = goldDone.coerceIn(0, n)
        when (style) {
            ArtStyle.RINGS -> drawRings(rnd, n, g, palette, gold)
            ArtStyle.PETALS -> drawPetals(rnd, n, g, palette, gold)
            ArtStyle.FLOW -> drawFlow(rnd, n, g, palette, gold)
        }
    }
}

private fun DrawScope.drawRings(rnd: Random, n: Int, gold: Int, palette: List<Color>, goldColor: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    val max = size.minDimension / 2 * 0.9f
    for (i in 0 until n) {
        val r = max * (0.18f + 0.8f * (i + 1) / n)
        val isGold = i < gold
        val w = max * (0.03f + 0.04f * rnd.nextFloat()) * (if (isGold) 1.5f else 1f)
        val start = rnd.nextFloat() * 360f
        val sweep = 90f + rnd.nextFloat() * 250f
        val col = if (isGold) goldColor else palette[rnd.nextInt(palette.size)]
        drawArc(col, start, sweep, false, Offset(c.x - r, c.y - r), Size(2 * r, 2 * r), style = Stroke(w, cap = StrokeCap.Round))
        val end = (start + sweep) * (PI.toFloat() / 180f)
        drawCircle(col, w * 0.9f, Offset(c.x + r * cos(end), c.y + r * sin(end)))
    }
}

private fun DrawScope.drawPetals(rnd: Random, n: Int, gold: Int, palette: List<Color>, goldColor: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    val max = size.minDimension / 2 * 0.92f
    val outer = n + 1
    val tilt = rnd.nextFloat() * 360f
    fun petal(len: Float, width: Float) = Path().apply {
        moveTo(c.x, c.y)
        cubicTo(c.x - width, c.y - len * 0.35f, c.x - width * 0.6f, c.y - len * 0.85f, c.x, c.y - len)
        cubicTo(c.x + width * 0.6f, c.y - len * 0.85f, c.x + width, c.y - len * 0.35f, c.x, c.y)
        close()
    }
    // inner layer: shorter petals between the outer ones
    for (i in 0 until outer) {
        val len = max * (0.42f + 0.1f * rnd.nextFloat())
        val col = palette[rnd.nextInt(palette.size)]
        rotate(tilt + (i + 0.5f) * 360f / outer, c) { drawPath(petal(len, len * 0.32f), col.copy(alpha = 0.55f)) }
    }
    // outer layer: the first ones are gold
    for (i in 0 until outer) {
        val len = max * (0.78f + 0.2f * rnd.nextFloat())
        val isGold = i < gold
        val col = if (isGold) goldColor else palette[rnd.nextInt(palette.size)]
        rotate(tilt + i * 360f / outer, c) {
            drawPath(petal(len, len * 0.2f), col.copy(alpha = if (isGold) 1f else 0.8f))
            drawPath(petal(len, len * 0.2f), col, style = Stroke(max * 0.012f, join = StrokeJoin.Round))
        }
    }
    drawCircle(goldColor.copy(alpha = if (gold > 0) 1f else 0.0f), max * 0.06f, c)
}

private fun DrawScope.drawFlow(rnd: Random, n: Int, gold: Int, palette: List<Color>, goldColor: Color) {
    val c = Offset(size.width / 2, size.height / 2)
    val max = size.minDimension / 2 * 0.92f
    val f1 = 1.5f + rnd.nextFloat() * 3f
    val f2 = 1.5f + rnd.nextFloat() * 3f
    val p1 = rnd.nextFloat() * 6.28f
    val p2 = rnd.nextFloat() * 6.28f
    val lines = n * 4
    for (i in 0 until lines) {
        // start somewhere inside the circle
        val a = rnd.nextFloat() * 2f * PI.toFloat()
        val rad = max * 0.9f * sqrt(rnd.nextFloat())
        var x = c.x + rad * cos(a)
        var y = c.y + rad * sin(a)
        val path = Path().apply { moveTo(x, y) }
        val steps = 28 + rnd.nextInt(30)
        for (s in 0 until steps) {
            val nx = (x - c.x) / max
            val ny = (y - c.y) / max
            val angle = sin(nx * f1 + p1) * 1.6f + cos(ny * f2 + p2) * 1.6f
            x += cos(angle) * max * 0.035f
            y += sin(angle) * max * 0.035f
            if ((x - c.x) * (x - c.x) + (y - c.y) * (y - c.y) > max * max) break
            path.lineTo(x, y)
        }
        val isGold = i < gold
        val col = if (isGold) goldColor else palette[rnd.nextInt(palette.size)].copy(alpha = 0.8f)
        drawPath(path, col, style = Stroke(max * (if (isGold) 0.045f else 0.018f + 0.012f * rnd.nextFloat()), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** The personal badge with a short caption: a collectible at the top of the Rewards screen. */
@Composable
fun PersonalBadgeCard(name: String, count: Int, countLabel: String, goldDone: Int, caption: StringResource, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(MaterialTheme.borderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PersonalBadgeArt(name, count, goldDone, Modifier.size(84.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.art_title), style = MaterialTheme.typography.titleMedium)
                Text(countLabel, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text(stringResource(caption), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
