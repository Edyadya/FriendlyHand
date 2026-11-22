package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

/**
 * The hand-over order book, like orders for delivery drivers: a patron who brings something hands it over to a
 * volunteer. OPEN (waiting for a volunteer) -> ACCEPTED (a volunteer took it) -> PICKED_UP (the volunteer has
 * the item) -> DELIVERED (the person confirmed they received it).
 */
@Serializable
enum class OrderStatus { OPEN, ACCEPTED, PICKED_UP, DELIVERED }

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
)

fun DeliveryOrder.acceptedBy(volunteerId: String, volunteerName: String, atMillis: Long = nowMillis()): DeliveryOrder =
    if (status != OrderStatus.OPEN) this
    else copy(status = OrderStatus.ACCEPTED, volunteerId = volunteerId, volunteerName = volunteerName, acceptedAtMillis = atMillis)

fun DeliveryOrder.pickedUp(): DeliveryOrder =
    if (status == OrderStatus.ACCEPTED) copy(status = OrderStatus.PICKED_UP) else this

fun DeliveryOrder.delivered(atMillis: Long = nowMillis()): DeliveryOrder =
    if (status == OrderStatus.DELIVERED || status == OrderStatus.OPEN) this
    else copy(status = OrderStatus.DELIVERED, deliveredAtMillis = atMillis)
