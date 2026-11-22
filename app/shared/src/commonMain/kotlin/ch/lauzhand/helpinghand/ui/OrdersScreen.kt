package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ch.lauzhand.helpinghand.model.communityImpact
import ch.lauzhand.helpinghand.model.needCoverage
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.data.HelpRepository
import ch.lauzhand.helpinghand.model.DeliveryOrder
import ch.lauzhand.helpinghand.model.OrderKind
import ch.lauzhand.helpinghand.model.OrderStatus
import ch.lauzhand.helpinghand.model.formatDate
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Volunteers: the order book, like orders for delivery drivers. A patron hands an item over; a volunteer takes the
 * order, picks the item up and delivers it to the person, who then confirms it.
 */
@Composable
fun OrdersScreen(repo: HelpRepository, volunteerId: String, volunteerName: String, onOpenPerson: (String) -> Unit, onBack: (() -> Unit)? = null) {
    val orders = repo.orders.sortedByDescending { it.createdAtMillis }
    val open = orders.filter { it.status == OrderStatus.OPEN }
    val mine = orders.filter { it.volunteerId == volunteerId && (it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PICKED_UP) }
    val done = orders.filter { it.status == OrderStatus.DELIVERED && it.volunteerId == volunteerId }

    // The "it reached them" moment lives here, not in a card: a delivered card moves to another section.
    var delivered by remember { mutableStateOf<DeliveryOrder?>(null) }
    val onDelivered: (DeliveryOrder) -> Unit = { delivered = it }
    delivered?.let { o ->
        val need = repo.needs.firstOrNull { it.id == o.needId }
        if (need != null) {
            ConfirmedMoment(
                impactLine = reachedLine(need, repo.people.firstOrNull { it.id == o.personId }?.firstName.orEmpty(), repo.zones.firstOrNull { it.id == o.zoneId }?.name.orEmpty(), mine = false),
                impact = communityImpact(repo.needs, repo.giveaways),
            ) { delivered = null }
        }
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // the community's impact and coverage, as at the top of a patron's Deliveries
        item { ImpactStrip(communityImpact(repo.needs, repo.giveaways)) }
        item { CoverageCard(needCoverage(repo.needs)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
                Text(stringResource(Res.string.orders_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(Res.string.orders_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        section(Res.string.orders_open, open) { OrderCard(repo, it, volunteerId, volunteerName, onOpenPerson, onDelivered) }
        section(Res.string.orders_mine, mine) { OrderCard(repo, it, volunteerId, volunteerName, onOpenPerson, onDelivered) }
        section(Res.string.orders_done, done) { OrderCard(repo, it, volunteerId, volunteerName, onOpenPerson, onDelivered) }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: org.jetbrains.compose.resources.StringResource,
    list: List<DeliveryOrder>,
    card: @Composable (DeliveryOrder) -> Unit,
) {
    item { Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
    if (list.isEmpty()) item { Text(stringResource(Res.string.orders_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    items(list, key = { it.id }) { card(it) }
}

@Composable
private fun OrderCard(repo: HelpRepository, o: DeliveryOrder, volunteerId: String, volunteerName: String, onOpenPerson: (String) -> Unit, onDelivered: (DeliveryOrder) -> Unit) {
    val person = repo.people.firstOrNull { it.id == o.personId }
    val zone = repo.zones.firstOrNull { it.id == o.zoneId }?.name.orEmpty()
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(MaterialTheme.borderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val buying = o.kind == OrderKind.PURCHASE
            Text(
                if (buying) stringResource(Res.string.order_buy_for, o.itemTitle, person?.firstName.orEmpty(), zone)
                else stringResource(Res.string.order_item_for, o.itemTitle, person?.firstName.orEmpty(), zone),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                if (buying) stringResource(Res.string.order_paid_by, o.patronName, formatDate(o.createdAtMillis))
                else stringResource(Res.string.order_from, o.patronName, formatDate(o.createdAtMillis)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(o.status.line(o, person?.firstName.orEmpty()), style = MaterialTheme.typography.bodyLarge)
            when (o.status) {
                OrderStatus.OPEN -> AppButton(
                    onClick = { repo.acceptOrder(o.id, volunteerId, volunteerName) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(Res.string.order_accept), style = MaterialTheme.typography.titleMedium) }
                OrderStatus.ACCEPTED -> AppButton(
                    onClick = { repo.markPickedUp(o.id) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(if (buying) Res.string.order_bought else Res.string.order_picked_up), style = MaterialTheme.typography.titleMedium) }
                OrderStatus.PICKED_UP -> AppButton(
                    onClick = { repo.confirmDelivered(o.needId); onDelivered(o) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                ) { Text(stringResource(Res.string.order_delivered_button, person?.firstName.orEmpty()), style = MaterialTheme.typography.titleMedium) }
                OrderStatus.DELIVERED -> Unit
            }
            AppOutlinedButton(onClick = { onOpenPerson(o.personId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(Res.string.order_view_person, person?.firstName.orEmpty()))
            }
        }
    }
}

/** One line saying where the order is: used by the order book and by the patron's My contributions. */
@Composable
fun OrderStatus.line(o: DeliveryOrder, personName: String): String = when (this) {
    OrderStatus.OPEN -> stringResource(Res.string.order_status_open)
    OrderStatus.ACCEPTED -> stringResource(if (o.kind == OrderKind.PURCHASE) Res.string.order_status_accepted_buy else Res.string.order_status_accepted, o.volunteerName.orEmpty())
    OrderStatus.PICKED_UP -> stringResource(if (o.kind == OrderKind.PURCHASE) Res.string.order_status_picked_buy else Res.string.order_status_picked, o.volunteerName.orEmpty(), personName)
    OrderStatus.DELIVERED -> stringResource(Res.string.order_status_delivered, o.volunteerName.orEmpty())
}
