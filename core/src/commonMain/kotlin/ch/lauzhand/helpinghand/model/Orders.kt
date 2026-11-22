package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

/**
 * The hand-over order book, like orders for delivery drivers: a patron who brings something hands it over to a
 * volunteer. OPEN (waiting for a volunteer) -> ACCEPTED (a volunteer took it) -> PICKED_UP (the volunteer has
 * the item) -> DELIVERED (the person confirmed they received it).
 */
@Serializable
enum class OrderStatus { OPEN, ACCEPTED, PICKED_UP, DELIVERED }

/** HANDOVER: a patron brings the item and hands it to a volunteer. PURCHASE: a patron paid, the volunteer buys the item. */
@Serializable
enum class OrderKind { HANDOVER, PURCHASE }

@Serializable
data class DeliveryOrder(
    val id: String,
    val needId: String,
    val personId: String,
    val itemTitle: String,
    val zoneId: String,
    val patronId: String,
    val patronName: String,
    val createdAtMillis: Long,
    val status: OrderStatus = OrderStatus.OPEN,
    val volunteerId: String? = null,
    val volunteerName: String? = null,
    val acceptedAtMillis: Long? = null,
    val deliveredAtMillis: Long? = null,
    val kind: OrderKind = OrderKind.HANDOVER,
)

fun DeliveryOrder.acceptedBy(volunteerId: String, volunteerName: String, atMillis: Long = nowMillis()): DeliveryOrder =
    if (status != OrderStatus.OPEN) this
    else copy(status = OrderStatus.ACCEPTED, volunteerId = volunteerId, volunteerName = volunteerName, acceptedAtMillis = atMillis)

fun DeliveryOrder.pickedUp(): DeliveryOrder =
    if (status == OrderStatus.ACCEPTED) copy(status = OrderStatus.PICKED_UP) else this

fun DeliveryOrder.delivered(atMillis: Long = nowMillis()): DeliveryOrder =
    if (status == OrderStatus.DELIVERED || status == OrderStatus.OPEN) this
    else copy(status = OrderStatus.DELIVERED, deliveredAtMillis = atMillis)

/** The chat is open while a volunteer has taken the order and it is not delivered yet. */
val DeliveryOrder.chatOpen: Boolean get() = status == OrderStatus.ACCEPTED || status == OrderStatus.PICKED_UP

/** One message in the temporary chat between a patron and the volunteer who took their order. */
@Serializable
data class ChatMessage(
    val id: String,
    val orderId: String,
    val fromVolunteer: Boolean,
    val senderName: String,
    val text: String,
    val atMillis: Long,
)

/** Longest message, in characters. */
const val MAX_CHAT_CHARS = 500
