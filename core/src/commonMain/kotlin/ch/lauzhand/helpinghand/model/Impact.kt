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

/** Volunteer badges, by how many deliveries they have completed (an order delivered and confirmed by the person). */
enum class VolunteerBadge(val target: Int) { FIRST_DELIVERY(1), FIVE_DELIVERIES(5), TEN_DELIVERIES(10) }

data class VolunteerProgress(val badge: VolunteerBadge, val progress: Int) {
    val unlocked: Boolean get() = progress >= badge.target
}

/** Orders this volunteer delivered. */
fun deliveriesDone(orders: List<DeliveryOrder>, volunteerId: String): Int =
    orders.count { it.volunteerId == volunteerId && it.status == OrderStatus.DELIVERED }

/** Orders this volunteer has taken and not delivered yet. */
fun deliveriesInProgress(orders: List<DeliveryOrder>, volunteerId: String): Int =
    orders.count { it.volunteerId == volunteerId && (it.status == OrderStatus.ACCEPTED || it.status == OrderStatus.PICKED_UP) }

fun volunteerBadges(deliveries: Int): List<VolunteerProgress> =
    VolunteerBadge.entries.map { VolunteerProgress(it, deliveries.coerceAtMost(it.target)) }
