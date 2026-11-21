package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

private const val MAX_DIGITS = 6

/** Asks how much to give. The donor types any whole CHF amount; Give stays disabled until it is above 0. */
@Composable
fun AmountDialog(onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val amount = text.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.donate_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(MAX_DIGITS) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.donate_amount_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(amount) }, enabled = amount > 0) {
                Text(stringResource(Res.string.donate_confirm), style = MaterialTheme.typography.titleMedium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) }
        },
    )
}
