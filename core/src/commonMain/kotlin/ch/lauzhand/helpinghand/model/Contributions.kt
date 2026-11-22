package ch.lauzhand.helpinghand.model

// Recording contributions and computing donor achievements, shared by the app and the server.

/** The contribution a claim (bring it in person) makes. */
fun Need.claimContribution(donorId: String, atMillis: Long = nowMillis()): Contribution =
    Contribution(donorId, id, ContributionKind.BROUGHT, null, atMillis)

/** The contribution a donation makes: paying for a small need, or giving to a big goal. */
fun Need.donationContribution(donorId: String, amountChf: Int, atMillis: Long = nowMillis()): Contribution =
    Contribution(
        donorId, id,
        if (type == NeedType.SMALL) ContributionKind.PAID else ContributionKind.GAVE,
        amountChf, atMillis,
    )

/** Money (paying for an item, giving to a goal) is never verified: it counts at once. Only bringing a physical item is. */
val Contribution.isMoney: Boolean get() = kind != ContributionKind.BROUGHT

/** True if this contribution counts: it is money, or the item it brought was verified by a volunteer. */
private fun Contribution.counts(need: Need): Boolean = isMoney || need.isVerified

/**
 * Number of distinct needs among [contributions] that count towards goals and the voucher. Money counts at once;
 * an item a patron brought counts once a volunteer has confirmed it with the person (until then it is pending,
 * see [pendingGoals]).
 */
fun completedGoals(contributions: List<Contribution>, needs: List<Need>): Int {
    val byId = needs.associateBy { it.id }
    return contributions.filter { c -> byId[c.needId]?.let { c.counts(it) } == true }.map { it.needId }.toSet().size
}

/** Number of distinct needs where an item was brought and handed over but no volunteer has verified it yet. */
fun pendingGoals(contributions: List<Contribution>, needs: List<Need>): Int {
    val byId = needs.associateBy { it.id }
    val counted = contributions.filter { c -> byId[c.needId]?.let { c.counts(it) } == true }.map { it.needId }.toSet()
    return contributions.map { it.needId }.toSet().count { id -> id !in counted && byId[id]?.isPendingVerification == true }
}

enum class AchievementId {
    FIRST_HELP, HELPED_3, HELPED_10, HAND_TO_HAND, GOAL_REACHED, GAVE_100, THREE_AREAS, THANKS_3,
    // money donated, in total (what was given to needs plus what was paid towards giveaway logistics)
    GAVE_50, GAVE_250, GAVE_500, GAVE_1000,
}

/** Money badges: the total in CHF each one needs. */
val MoneyBadgeTargets: Map<AchievementId, Int> = mapOf(
    AchievementId.GAVE_50 to 50, AchievementId.GAVE_100 to 100, AchievementId.GAVE_250 to 250,
    AchievementId.GAVE_500 to 500, AchievementId.GAVE_1000 to 1000,
)

/**
 * [progress] counts only verified help, so [unlocked] needs a volunteer's verification.
 * [potential] also counts help that is delivered but not verified yet: when that alone would reach the
 * target, the badge is [pending] (waiting for a volunteer) rather than locked.
 */
data class Achievement(val id: AchievementId, val progress: Int, val target: Int, val potential: Int = progress) {
    val unlocked: Boolean get() = progress >= target
    val pending: Boolean get() = !unlocked && potential >= target
}

/**
 * A donor's badges, from their own [contributions]. Progress is capped at the target.
 * Help counts once a volunteer has verified it; delivered-but-unverified help only makes a badge pending.
 */
fun donorAchievements(
    contributions: List<Contribution>,
    needs: List<Need>,
    people: List<Person>,
    /** CHF paid towards giveaway logistics: it counts at once towards the money badges (no volunteer has to verify it). */
    giveawayChf: Int = 0,
): List<Achievement> {
    val needById = needs.associateBy { it.id }

    /** The numbers badges are made of, for one set of contributions. */
    class Tally(val contributions: List<Contribution>, val helpedNeeds: List<Need>, val helpedPeople: Set<String>, val areas: Set<String>, val given: Int)

    fun tally(subset: List<Contribution>): Tally {
        val helpedNeeds = subset.mapNotNull { needById[it.needId] }.distinctBy { it.id }
        val helpedPeople = helpedNeeds.map { it.personId }.toSet()
        val areas = people.filter { it.id in helpedPeople }.map { it.zoneId }.toSet()
        return Tally(subset, helpedNeeds, helpedPeople, areas, subset.sumOf { it.amountChf ?: 0 })
    }
    // verified: the need was confirmed by a volunteer. delivered: handed over (verified or not).
    // Money counts at once (nothing to verify). A brought item counts once a volunteer verified it; delivered but
    // not yet verified items only make a badge pending.
    val verified = tally(contributions.filter { c -> needById[c.needId]?.let { c.counts(it) } == true })
    val delivered = tally(contributions.filter { c -> needById[c.needId]?.let { c.isMoney || it.isCompleted } == true })

    fun a(id: AchievementId, v: Int, d: Int, target: Int) = Achievement(id, v.coerceAtMost(target), target, d.coerceAtMost(target))
    return listOf(
        a(AchievementId.FIRST_HELP, verified.helpedPeople.size, delivered.helpedPeople.size, 1),
        a(AchievementId.HELPED_3, verified.helpedPeople.size, delivered.helpedPeople.size, 3),
        a(AchievementId.HELPED_10, verified.helpedPeople.size, delivered.helpedPeople.size, 10),
        a(AchievementId.HAND_TO_HAND, verified.contributions.count { it.kind == ContributionKind.BROUGHT }, delivered.contributions.count { it.kind == ContributionKind.BROUGHT }, 1),
        a(AchievementId.GOAL_REACHED, verified.helpedNeeds.count { it.type == NeedType.BIG && it.isCompleted }, delivered.helpedNeeds.count { it.type == NeedType.BIG && it.isCompleted }, 1),
        a(AchievementId.GAVE_50, verified.given + giveawayChf, delivered.given + giveawayChf, 50),
        a(AchievementId.GAVE_100, verified.given + giveawayChf, delivered.given + giveawayChf, 100),
        a(AchievementId.GAVE_250, verified.given + giveawayChf, delivered.given + giveawayChf, 250),
        a(AchievementId.GAVE_500, verified.given + giveawayChf, delivered.given + giveawayChf, 500),
        a(AchievementId.GAVE_1000, verified.given + giveawayChf, delivered.given + giveawayChf, 1000),
        a(AchievementId.THREE_AREAS, verified.areas.size, delivered.areas.size, 3),
        // "confirmed as received" is verification itself (only for items), so there is nothing pending for this one
        a(AchievementId.THANKS_3, verified.helpedNeeds.count { it.isVerified }, verified.helpedNeeds.count { it.isVerified }, 3),
    )
}
