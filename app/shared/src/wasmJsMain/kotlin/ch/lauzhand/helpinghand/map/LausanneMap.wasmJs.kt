package ch.lauzhand.helpinghand.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.Person
import ch.lauzhand.helpinghand.model.Role
import ch.lauzhand.helpinghand.model.Zone
import helpinghand.app.shared.generated.resources.Res
import helpinghand.app.shared.generated.resources.map_unavailable
import org.jetbrains.compose.resources.stringResource

// No MapLibre on this target: a simple placeholder keeps the target compiling.
@Composable
actual fun LausanneMap(
    zones: List<Zone>,
    people: List<Person>,
    needs: List<Need>,
    role: Role,
    onZoneClick: (zoneId: String) -> Unit,
    onPersonClick: (personId: String) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.map_unavailable),
            Modifier.padding(24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

actual val isLiveMapSupported: Boolean = false

actual fun installMapSupport() {}
