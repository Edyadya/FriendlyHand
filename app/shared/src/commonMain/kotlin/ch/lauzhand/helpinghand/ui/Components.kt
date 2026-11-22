package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Primary button: primary fill, onPrimary text, 12 dp radius, at least 48 dp tall. Locked look when disabled. */
@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = AppShape.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.app.locked,
            disabledContentColor = MaterialTheme.app.lockedContent,
        ),
        content = content,
    )
}

/** Secondary button: transparent, 1 dp primary outline, primary text. */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        enabled = enabled,
        shape = AppShape.button,
        border = BorderStroke(MaterialTheme.borderWidth, if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.app.locked),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.app.lockedContent,
        ),
        content = content,
    )
}


/**
 * A text field. Light mode is the standard outlined field, unchanged. Dark mode: filled with the card colour,
 * a 1.5 dp border in the outline colour (#4A67B8), and a 2 dp #8EAEFF border while it has focus.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    keyboardActions: androidx.compose.foundation.text.KeyboardActions = androidx.compose.foundation.text.KeyboardActions.Default,
    singleLine: Boolean = false,
    minLines: Int = 1,
) {
    if (!MaterialTheme.isDark) {
        OutlinedTextField(
            value, onValueChange, modifier, enabled = enabled, readOnly = readOnly, label = label, trailingIcon = trailingIcon,
            supportingText = supportingText, isError = isError, visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions, keyboardActions = keyboardActions, singleLine = singleLine, minLines = minLines,
        )
        return
    }
    val interaction = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val fill = MaterialTheme.colorScheme.surface
    val colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = fill, unfocusedContainerColor = fill, errorContainerColor = fill, disabledContainerColor = fill)
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minWidth = OutlinedTextFieldDefaults.MinWidth, minHeight = OutlinedTextFieldDefaults.MinHeight),
        enabled = enabled,
        readOnly = readOnly,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else minLines,
        decorationBox = { inner ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value, innerTextField = inner, enabled = enabled, singleLine = singleLine,
                visualTransformation = visualTransformation, interactionSource = interaction, isError = isError,
                label = label, trailingIcon = trailingIcon, supportingText = supportingText, colors = colors,
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = enabled, isError = isError, interactionSource = interaction, colors = colors,
                        shape = OutlinedTextFieldDefaults.shape, focusedBorderThickness = 2.dp, unfocusedBorderThickness = 1.5.dp,
                    )
                },
            )
        },
    )
}


/**
 * A progress bar. In dark mode the little "stop" dot at the end of the track is left out, so an empty bar looks
 * empty (just the track) and a full one is just the fill. Light mode is the standard bar.
 */
@Composable
fun AppProgressBar(progress: () -> Float, modifier: Modifier = Modifier, color: Color, trackColor: Color) {
    if (MaterialTheme.isDark) {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor, drawStopIndicator = {})
    } else {
        LinearProgressIndicator(progress = progress, modifier = modifier, color = color, trackColor = trackColor)
    }
}
