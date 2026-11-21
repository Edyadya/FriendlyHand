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

/** Number of distinct finished needs among [contributions] (what the voucher counts). */
fun completedGoals(contributions: List<Contribution>, needs: List<Need>): Int {
    val helped = contributions.map { it.needId }.toSet()
    return needs.count { it.id in helped && it.isCompleted }
}

enum class AchievementId { FIRST_HELP, HELPED_3, HELPED_10, HAND_TO_HAND, GOAL_REACHED, GAVE_100, THREE_AREAS, THANKS_3 }

data class Achievement(val id: AchievementId, val progress: Int, val target: Int) {
    val unlocked: Boolean get() = progress >= target
}

/** A donor's badges, from their own [contributions]. Progress is capped at the target. */
fun donorAchievements(contributions: List<Contribution>, needs: List<Need>, people: List<Person>): List<Achievement> {
    val needById = needs.associateBy { it.id }
    val helpedNeeds = contributions.mapNotNull { needById[it.needId] }.distinctBy { it.id }
    val helpedPeople = helpedNeeds.map { it.personId }.toSet()
    val areas = people.filter { it.id in helpedPeople }.map { it.zoneId }.toSet()
    val given = contributions.sumOf { it.amountChf ?: 0 }

    fun a(id: AchievementId, progress: Int, target: Int) = Achievement(id, progress.coerceAtMost(target), target)
    return listOf(
        a(AchievementId.FIRST_HELP, helpedPeople.size, 1),
        a(AchievementId.HELPED_3, helpedPeople.size, 3),
        a(AchievementId.HELPED_10, helpedPeople.size, 10),
        a(AchievementId.HAND_TO_HAND, contributions.count { it.kind == ContributionKind.BROUGHT }, 1),
        a(AchievementId.GOAL_REACHED, helpedNeeds.count { it.type == NeedType.BIG && it.isCompleted }, 1),
        a(AchievementId.GAVE_100, given, 100),
        a(AchievementId.THREE_AREAS, areas.size, 3),
        a(AchievementId.THANKS_3, helpedNeeds.count { it.status == NeedStatus.CONFIRMED }, 3),
    )
}
