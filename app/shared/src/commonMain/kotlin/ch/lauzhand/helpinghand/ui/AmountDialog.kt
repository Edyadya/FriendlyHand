package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

/** Pick any whole CHF amount between [min] and [max] with a slider, starting at [initial]. */
@Composable
fun AmountDialog(
    initial: Int,
    max: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
    min: Int = 1,
) {
    var value by remember { mutableStateOf(initial.coerceIn(min, max).toFloat()) }
    val amount = value.roundToInt()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.donate_title)) },
        text = {
            Column {
                Text(
                    stringResource(Res.string.donate_amount_chf, amount),
                    style = MaterialTheme.typography.displaySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = min.toFloat()..max.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            AppButton(onClick = { onConfirm(amount) }) {
                Text(stringResource(Res.string.donate_confirm), style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
