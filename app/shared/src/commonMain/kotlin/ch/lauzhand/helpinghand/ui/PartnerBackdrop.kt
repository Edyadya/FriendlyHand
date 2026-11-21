package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Where each partner's bubble sits, as fractions of the page, and how big it is relative to the others.
// Staggered on purpose, so the page looks scattered rather than gridded. The two real partners
// (JetBrains, IMS) are in the middle of the page, where the cards let them show through best.
private val BackdropSpots = mapOf(
    Partner.KOLIBRI to Triple(0.03f, 0.03f, 0.90f),
    Partner.WAMAZON to Triple(0.57f, 0.07f, 0.82f),
    Partner.JETBRAINS to Triple(0.06f, 0.27f, 1.15f),
    Partner.IMS to Triple(0.58f, 0.41f, 1.02f),
    Partner.VELOLAC to Triple(0.03f, 0.61f, 0.86f),
    Partner.PAGINA to Triple(0.54f, 0.72f, 1.06f),
)

/**
 * A soft, fixed backdrop made of the partners (big faint bubbles with their initials and name),
 * for the Achievements page: the cards scroll over it. Colours come only from the theme.
 * Partners show as initials and name; no logos are used.
 */
@Composable
fun PartnerBackdrop(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(
        modifier.background(
            Brush.verticalGradient(
                listOf(
                    colors.primaryContainer.copy(alpha = 0.55f),
                    colors.tertiaryContainer.copy(alpha = 0.35f),
                    colors.background,
                ),
            ),
        ),
    ) {
        val base = minOf(maxWidth, 560.dp) * 0.40f
        Partner.entries.forEach { partner ->
            val (fx, fy, scale) = BackdropSpots.getValue(partner)
            BackdropBubble(
                partner,
                Modifier.offset(x = maxWidth * fx, y = maxHeight * fy).size(base * scale),
            )
        }
    }
}

@Composable
private fun BackdropBubble(partner: Partner, modifier: Modifier) {
    val colors = MaterialTheme.colorScheme
    val tint = when (partner.ordinal % 3) {
        0 -> colors.tertiary
        1 -> colors.secondary
        else -> colors.primary
    }
    Box(
        modifier
            .alpha(0.75f)
            .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.60f), tint.copy(alpha = 0.14f))), CircleShape)
            .border(1.5.dp, tint.copy(alpha = 0.50f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                partner.monogram,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = tint,
            )
            Text(
                partner.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurface.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
    }
}
