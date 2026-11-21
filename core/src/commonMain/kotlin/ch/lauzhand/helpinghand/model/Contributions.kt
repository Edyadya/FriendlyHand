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

/**
 * Number of distinct VERIFIED needs among [contributions] (what the voucher counts). A need counts once a
 * volunteer has confirmed it with the person; until then it is pending (see [pendingGoals]).
 */
fun completedGoals(contributions: List<Contribution>, needs: List<Need>): Int {
    val helped = contributions.map { it.needId }.toSet()
    return needs.count { it.id in helped && it.isVerified }
}

/** Number of distinct needs among [contributions] that are delivered and waiting for a volunteer to verify them. */
fun pendingGoals(contributions: List<Contribution>, needs: List<Need>): Int {
    val helped = contributions.map { it.needId }.toSet()
    return needs.count { it.id in helped && it.isPendingVerification }
}

enum class AchievementId { FIRST_HELP, HELPED_3, HELPED_10, HAND_TO_HAND, GOAL_REACHED, GAVE_100, THREE_AREAS, THANKS_3 }

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
fun donorAchievements(contributions: List<Contribution>, needs: List<Need>, people: List<Person>): List<Achievement> {
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
    val verified = tally(contributions.filter { needById[it.needId]?.isVerified == true })
    val delivered = tally(contributions.filter { needById[it.needId]?.isCompleted == true })

    fun a(id: AchievementId, v: Int, d: Int, target: Int) = Achievement(id, v.coerceAtMost(target), target, d.coerceAtMost(target))
    return listOf(
        a(AchievementId.FIRST_HELP, verified.helpedPeople.size, delivered.helpedPeople.size, 1),
        a(AchievementId.HELPED_3, verified.helpedPeople.size, delivered.helpedPeople.size, 3),
        a(AchievementId.HELPED_10, verified.helpedPeople.size, delivered.helpedPeople.size, 10),
        a(AchievementId.HAND_TO_HAND, verified.contributions.count { it.kind == ContributionKind.BROUGHT }, delivered.contributions.count { it.kind == ContributionKind.BROUGHT }, 1),
        a(AchievementId.GOAL_REACHED, verified.helpedNeeds.count { it.type == NeedType.BIG }, delivered.helpedNeeds.count { it.type == NeedType.BIG }, 1),
        a(AchievementId.GAVE_100, verified.given, delivered.given, 100),
        a(AchievementId.THREE_AREAS, verified.areas.size, delivered.areas.size, 3),
        // "confirmed as received" is verification itself, so there is nothing pending for this one
        a(AchievementId.THANKS_3, verified.helpedNeeds.size, verified.helpedNeeds.size, 3),
    )
}
