package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * "Made for [IMC logo] IMC · Built with [Kotlin logo] Kotlin Multiplatform", one small muted line.
 * It wraps onto two lines at the "·" when it is too wide. Used on the login screen and in About.
 * The logo files are placeholders (ic_imc_*, ic_kotlin_*) until the real ones are supplied.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FooterLine(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.isDark
    val imc: DrawableResource = if (dark) Res.drawable.ic_imc_dark else Res.drawable.ic_imc_light
    val kotlin: DrawableResource = if (dark) Res.drawable.ic_kotlin_dark else Res.drawable.ic_kotlin_light
    val style = MaterialTheme.typography.labelMedium
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(Res.string.footer_made_for), style = style, color = muted)
            Image(painterResource(imc), contentDescription = null, modifier = Modifier.height(18.dp))
            Text(stringResource(Res.string.footer_imc) + " ·", style = style, color = muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(stringResource(Res.string.footer_built_with), style = style, color = muted)
            Image(painterResource(kotlin), contentDescription = null, modifier = Modifier.height(18.dp))
            Text(stringResource(Res.string.footer_kotlin), style = style, color = muted, textAlign = TextAlign.Start)
        }
    }
}
