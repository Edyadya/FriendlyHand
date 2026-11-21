package ch.lauzhand.helpinghand.model

import kotlin.test.*

class ContributionsTest {

    private val needs = Seed.needs
    private val people = Seed.people
    private fun need(id: String) = needs.first { it.id == id }
    private fun List<Achievement>.of(id: AchievementId) = first { it.id == id }

    @Test
    fun kindDependsOnHowTheDonorHelped() {
        assertEquals(ContributionKind.BROUGHT, need("n-1").claimContribution("d").kind)
        assertEquals(ContributionKind.PAID, need("n-1").donationContribution("d", 20).kind)
        assertEquals(ContributionKind.GAVE, need("n-2").donationContribution("d", 50).kind)
    }

    @Test
    fun noContributionsNoBadges() {
        assertTrue(donorAchievements(emptyList(), needs, people).none { it.unlocked })
    }

    @Test
    fun helpedThreeDifferentPeople() {
        // n-1 Jerry, n-3 Amina, n-5 Luc; two contributions to Jerry count once
        val c = listOf(
            need("n-1").claimContribution("d"),
            need("n-2").donationContribution("d", 30),
            need("n-3").donationContribution("d", 20),
            need("n-5").claimContribution("d"),
        )
        val a = donorAchievements(c, needs, people)
        assertTrue(a.of(AchievementId.FIRST_HELP).unlocked)
        assertTrue(a.of(AchievementId.HELPED_3).unlocked)
        assertEquals(3, a.of(AchievementId.HELPED_10).progress)
        assertFalse(a.of(AchievementId.HELPED_10).unlocked)
        assertTrue(a.of(AchievementId.HAND_TO_HAND).unlocked)
        assertEquals(50, a.of(AchievementId.GAVE_100).progress)
        // flon, gare, riponne
        assertTrue(a.of(AchievementId.THREE_AREAS).unlocked)
    }

    @Test
    fun goalReachedAndThanksFollowTheNeedStatus() {
        val done = needs.map {
            when (it.id) {
                "n-2" -> it.withDonation("d", 480)          // big goal fully funded
                "n-1", "n-3", "n-5" -> it.fulfilled().confirmed()
                else -> it
            }
        }
        val c = listOf("n-1", "n-3", "n-5").map { need(it).claimContribution("d") } +
            need("n-2").donationContribution("d", 480)
        val a = donorAchievements(c, done, people)
        assertTrue(a.of(AchievementId.GOAL_REACHED).unlocked)
        assertTrue(a.of(AchievementId.THANKS_3).unlocked)
        assertTrue(a.of(AchievementId.GAVE_100).unlocked)
        assertEquals(100, a.of(AchievementId.GAVE_100).progress) // capped at the target
        assertEquals(4, completedGoals(c, done))
    }
}
