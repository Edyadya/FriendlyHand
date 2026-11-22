package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.ChatMessage
import ch.lauzhand.helpinghand.model.DeliveryOrder
import ch.lauzhand.helpinghand.model.MAX_CHAT_CHARS
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * The temporary chat between a patron and the volunteer who took their order. It is open while the order is on its
 * way, and its messages are deleted when the item is delivered. [asVolunteer] says which side this screen is.
 */
@Composable
fun ChatDialog(repo: HelpRepository, order: DeliveryOrder, asVolunteer: Boolean, myName: String, onClose: () -> Unit) {
    val messages = repo.chatOf(order.id)
    var draft by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1) }
    val other = if (asVolunteer) order.patronName else order.volunteerName.orEmpty()
    val closeLabel = stringResource(Res.string.settings_close)

    fun send() {
        if (draft.isNotBlank()) { repo.sendChat(order.id, asVolunteer, myName, draft); draft = "" }
    }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            Modifier.padding(16.dp).fillMaxWidth().widthIn(max = 520.dp).heightIn(max = 640.dp).imePadding(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(MaterialTheme.borderWidth, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.chat_title, other), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClose)
                            .semantics { contentDescription = closeLabel; role = Role.Button },
                        contentAlignment = Alignment.Center,
                    ) { CloseIcon(MaterialTheme.colorScheme.primary) }
                }
                Text(stringResource(Res.string.chat_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(Res.string.chat_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (messages.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.chat_empty), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(messages, key = { it.id }) { m -> MessageBubble(m, mine = m.fromVolunteer == asVolunteer) }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppTextField(
                        draft, { draft = it.take(MAX_CHAT_CHARS) }, Modifier.weight(1f),
                        label = { Text(stringResource(Res.string.chat_input)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                    )
                    AppButton(onClick = { send() }, enabled = draft.isNotBlank(), modifier = Modifier.heightIn(min = 56.dp)) {
                        Text(stringResource(Res.string.chat_send))
                    }
                }
                Text(stringResource(Res.string.chat_demo_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessage, mine: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier.widthIn(max = 340.dp)
                .clip(shape)
                .background(if (mine) colors.primary else colors.surface)
                .then(if (mine) Modifier else Modifier.border(MaterialTheme.borderWidth, colors.outlineVariant, shape))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!mine) Text(m.senderName, style = MaterialTheme.typography.labelMedium, color = colors.primary)
            Text(m.text, style = MaterialTheme.typography.bodyLarge, color = if (mine) colors.onPrimary else colors.onSurface)
        }
    }
}
