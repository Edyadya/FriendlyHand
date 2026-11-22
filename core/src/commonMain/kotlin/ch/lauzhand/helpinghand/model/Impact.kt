package ch.lauzhand.helpinghand.model

/**
 * The community's totals, for everyone (not one patron): people helped, items delivered and CHF funded.
 * People and items only count once a volunteer has verified the delivery. CHF funded is what big goals have
 * received plus what patrons paid towards giveaway logistics (the amount of a paid small item is not recorded,
 * so it is not in this sum).
 */
data class CommunityImpact(val peopleHelped: Int, val itemsDelivered: Int, val chfFunded: Int)

fun communityImpact(needs: List<Need>, giveaways: List<Giveaway> = emptyList()): CommunityImpact = CommunityImpact(
    peopleHelped = peopleHelpedOverall(needs),
    itemsDelivered = needs.count { it.type == NeedType.SMALL && it.isVerified },
    chfFunded = needs.sumOf { it.fundedAmountChf } + giveaways.sumOf { it.fundedChf.coerceAtMost(it.logisticsGoalChf) },
)

/** How much of the need is covered: [fulfilled] of [total] needs are delivered, [waiting] are still open. */
data class Coverage(val fulfilled: Int, val total: Int, val waiting: Int) {
    /** Whole percent, 0 when there are no needs. */
    val percent: Int get() = if (total == 0) 0 else (fulfilled * 100) / total
    val fraction: Float get() = if (total == 0) 0f else fulfilled.toFloat() / total
}

/** Fulfilled = delivered or confirmed. Waiting = nobody has taken it yet (open, or a goal still being funded). */
fun needCoverage(needs: List<Need>): Coverage = Coverage(
    fulfilled = needs.count { it.isCompleted },
    total = needs.size,
    waiting = needs.count { it.status == NeedStatus.OPEN || it.status == NeedStatus.FUNDING },
)

/**
 * The patron's deliveries a volunteer has confirmed and whose "it reached them" moment was not shown yet, oldest
 * first. A confirmed need counts once, however many times the patron helped with it.
 */
fun deliveriesToCelebrate(contributions: List<Contribution>, needs: List<Need>, isCelebrated: (String) -> Boolean): List<Need> {
    val byId = needs.associateBy { it.id }
    return contributions.sortedBy { it.atMillis }.mapNotNull { byId[it.needId] }.distinctBy { it.id }
        .filter { it.isVerified && !isCelebrated(it.id) }
}

/**
 * Volunteer badges. Each is earned from the orders the volunteer has delivered (delivered = the person confirmed
 * they received it): how many, in how many different areas, how many different people, and bought items.
 */
enum class VolunteerBadge(val target: Int) {
    FIRST_DELIVERY(1), FIVE_DELIVERIES(5), TEN_DELIVERIES(10),
    THREE_AREAS(3), FIVE_PEOPLE(5), FIRST_PURCHASE(1),
}

data class VolunteerProgress(val badge: VolunteerBadge, val progress: Int) {
    val unlocked: Boolean get() = progress >= badge.target
}

private fun deliveredBy(orders: List<DeliveryOrder>, volunteerId: String) =
    orders.filter { it.volunteerId == volunteerId && it.status == OrderStatus.DELIVERED }

/** Orders this volunteer delivered. */
fun deliveriesDone(orders: List<DeliveryOrder>, volunteerId: String): Int = deliveredBy(orders, volunteerId).size

/** Orders this volunteer has taken and not delivered yet. */
fun deliveriesInProgress(orders: List<DeliveryOrder>, volunteerId: String): Int =
    orders.count { it.volunteerId == volunteerId && it.chatOpen }

/** The volunteer's badges with their progress (capped at the target). */
fun volunteerBadges(orders: List<DeliveryOrder>, volunteerId: String): List<VolunteerProgress> {
    val done = deliveredBy(orders, volunteerId)
    fun value(b: VolunteerBadge) = when (b) {
        VolunteerBadge.FIRST_DELIVERY, VolunteerBadge.FIVE_DELIVERIES, VolunteerBadge.TEN_DELIVERIES -> done.size
        VolunteerBadge.THREE_AREAS -> done.map { it.zoneId }.toSet().size
        VolunteerBadge.FIVE_PEOPLE -> done.map { it.personId }.toSet().size
        VolunteerBadge.FIRST_PURCHASE -> done.count { it.kind == OrderKind.PURCHASE }
    }
    return VolunteerBadge.entries.map { VolunteerProgress(it, value(it).coerceAtMost(it.target)) }
}

/**
 * A number that is the same for the same [name] and [count] and different for others. It seeds each person's
 * personal badge art, so the pattern is theirs and changes as their count grows.
 */
fun badgeSeed(name: String, count: Int): Int {
    var h = 1125899907 // arbitrary start
    for (c in name.trim().lowercase()) h = h * 31 + c.code
    h = h * 31 + count
    h = h xor (h ushr 15); h *= 0x2C1B3C6D; h = h xor (h ushr 12); h *= 0x297A2D39; h = h xor (h ushr 15)
    return h
}
