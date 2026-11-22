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

    /** The seed needs with [ids] delivered and verified by a volunteer. */
    private fun verified(vararg ids: String) = needs.map { if (it.id in ids) it.copy(status = NeedStatus.CONFIRMED) else it }

    @Test
    fun helpedThreeDifferentPeople() {
        // n-1 Jerry, n-3 Amina, n-5 Luc; two contributions to Jerry count once
        val c = listOf(
            need("n-1").claimContribution("d"),
            need("n-2").donationContribution("d", 30),
            need("n-3").donationContribution("d", 20),
            need("n-5").claimContribution("d"),
        )
        // a volunteer has verified all four
        val a = donorAchievements(c, verified("n-1", "n-2", "n-3", "n-5"), people)
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
                "n-2" -> it.withDonation("d", 480).confirmed() // big goal fully funded, then verified
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

    @Test
    fun deliveredButUnverifiedHelpIsPendingNotUnlocked() {
        val c = listOf(need("n-1").claimContribution("d"))
        val delivered = needs.map { if (it.id == "n-1") it.fulfilled() else it }

        val pending = donorAchievements(c, delivered, people).of(AchievementId.FIRST_HELP)
        assertFalse(pending.unlocked)
        assertTrue(pending.pending)
        assertEquals(0, completedGoals(c, delivered))   // does not count for the voucher yet
        assertEquals(1, pendingGoals(c, delivered))

        // once a volunteer verifies it, it unlocks and is no longer pending
        val verified = needs.map { if (it.id == "n-1") it.fulfilled().confirmed() else it }
        val unlocked = donorAchievements(c, verified, people).of(AchievementId.FIRST_HELP)
        assertTrue(unlocked.unlocked)
        assertFalse(unlocked.pending)
        assertEquals(1, completedGoals(c, verified))
        assertEquals(0, pendingGoals(c, verified))
    }

    @Test
    fun aPromiseAloneIsNeitherPendingNorUnlocked() {
        // claimed but not delivered yet
        val c = listOf(need("n-1").claimContribution("d"))
        val first = donorAchievements(c, needs, people).of(AchievementId.FIRST_HELP)
        assertFalse(first.unlocked)
        assertFalse(first.pending)
    }

    @Test
    fun peopleHelpedOverallCountsEachVerifiedPersonOnce() {
        val seeded = peopleHelpedOverall(needs)
        assertTrue(seeded >= 3)
        // a second verified need for someone who is already counted changes nothing
        val extra = needs + Need("n-x", "p-luc", NeedType.SMALL, "Scarf", status = NeedStatus.CONFIRMED)
        assertEquals(seeded, peopleHelpedOverall(extra))
        // delivered but unverified help is not counted
        val unverified = needs + Need("n-y", "p-tom", NeedType.SMALL, "Scarf", status = NeedStatus.FULFILLED)
        assertEquals(seeded, peopleHelpedOverall(unverified))
    }

    @Test
    fun aVerifiedGoalStaysVerifiedWhenMoreMoneyArrives() {
        val verifiedGoal = need("n-2").copy(fundedAmountChf = 800, status = NeedStatus.CONFIRMED)
        assertEquals(NeedStatus.CONFIRMED, verifiedGoal.withDonation("d", 5).status)
    }
}

class MoneyAndOrdersTest {
    @Test
    fun dateStampsAreCorrect() {
        assertEquals("01.01.1970", formatDate(0))
        assertEquals("29.02.2024", formatDate(1_709_164_800_000L)) // a leap day
        assertEquals("22.11.2026", formatDate(1_795_305_600_000L))
        assertEquals("31.12.1969", formatDate(-1))
    }

    @Test
    fun moneyBadgesFollowTheTotalIncludingGiveaways() {
        val a = donorAchievements(emptyList(), Seed.needs, Seed.people, giveawayChf = 260)
        fun id(i: AchievementId) = a.first { it.id == i }
        assertTrue(id(AchievementId.GAVE_50).unlocked)
        assertTrue(id(AchievementId.GAVE_100).unlocked)
        assertTrue(id(AchievementId.GAVE_250).unlocked)
        assertFalse(id(AchievementId.GAVE_500).unlocked)
        assertEquals(260, id(AchievementId.GAVE_500).progress)
        assertEquals(500, id(AchievementId.GAVE_500).target)
    }

    @Test
    fun aGiveawayNeverTakesMoreThanItsGoal() {
        val g = Giveaway("g", "Brand", "Coats", "x", logisticsGoalChf = 100, fundedChf = 90)
        val (after, taken) = g.withContribution(50)
        assertEquals(10, taken)
        assertTrue(after.isFunded)
        assertEquals(0, after.withContribution(5).second)
    }

    @Test
    fun anOrderGoesThroughTheVolunteerStepsInOrder() {
        val o = DeliveryOrder("o", "n-1", "p-jerry", "Coat", "flon", "donor-1", "Dan", 1)
        assertEquals(OrderStatus.OPEN, o.pickedUp().status)      // cannot be picked up before a volunteer accepts
        assertEquals(OrderStatus.OPEN, o.delivered().status)
        val taken = o.acceptedBy("v1", "Mia")
        assertEquals(OrderStatus.ACCEPTED, taken.status)
        assertEquals("Mia", taken.volunteerName)
        assertEquals("Mia", taken.acceptedBy("v2", "Other").volunteerName) // a second volunteer cannot take it
        assertEquals(OrderStatus.PICKED_UP, taken.pickedUp().status)
        assertEquals(OrderStatus.DELIVERED, taken.pickedUp().delivered().status)
    }
}

class MoneyIsNeverVerifiedTest {
    private val needs = Seed.needs
    private val people = Seed.people
    private fun need(id: String) = needs.first { it.id == id }
    private fun List<Achievement>.of(id: AchievementId) = first { it.id == id }

    @Test
    fun payingForAnItemCountsAtOnceWithoutAVolunteer() {
        val c = listOf(need("n-1").donationContribution("d", 20)) // paid: still CLAIMED, nobody verified anything
        assertEquals(1, completedGoals(c, needs))
        assertEquals(0, pendingGoals(c, needs))
        val a = donorAchievements(c, needs, people)
        assertTrue(a.of(AchievementId.FIRST_HELP).unlocked)
        assertFalse(a.of(AchievementId.FIRST_HELP).pending)
    }

    @Test
    fun givingToABigGoalCountsAtOnce() {
        val c = listOf(need("n-2").donationContribution("d", 50))
        assertEquals(1, completedGoals(c, needs))
        assertTrue(donorAchievements(c, needs, people).of(AchievementId.FIRST_HELP).unlocked)
        // but the big goal itself is only "reached" once it is fully funded
        assertFalse(donorAchievements(c, needs, people).of(AchievementId.GOAL_REACHED).unlocked)
    }

    @Test
    fun aBroughtItemStillWaitsForAVolunteer() {
        val c = listOf(need("n-1").claimContribution("d"))
        val handedOver = needs.map { if (it.id == "n-1") it.fulfilled() else it }
        assertEquals(0, completedGoals(c, handedOver))
        assertEquals(1, pendingGoals(c, handedOver))
        assertTrue(donorAchievements(c, handedOver, people).of(AchievementId.HAND_TO_HAND).pending)
    }

    @Test
    fun thankYouBadgeStillNeedsRealConfirmations() {
        // money only: nobody confirmed receiving anything, so the "confirmed" badge makes no progress
        val c = listOf(need("n-1").donationContribution("d", 20), need("n-3").donationContribution("d", 20), need("n-5").donationContribution("d", 20))
        assertEquals(0, donorAchievements(c, needs, people).of(AchievementId.THANKS_3).progress)
    }
}

class AccountProfileTest {
    @Test
    fun theLocationShowsOnlyWhatIsFilledIn() {
        assertNull(AccountProfile().location)
        assertEquals("Lausanne", AccountProfile(city = " Lausanne ").location)
        assertEquals("Switzerland", AccountProfile(country = "Switzerland").location)
        assertEquals("Lausanne, Switzerland", AccountProfile(city = "Lausanne", country = "Switzerland").location)
    }

    @Test
    fun theGenderChoiceIsOptionalAndHasAPreferNotToSay() {
        assertNull(AccountProfile().gender)
        assertTrue(Gender.entries.contains(Gender.PREFER_NOT_TO_SAY))
    }
}

class ImpactTest {
    private fun small(id: String, person: String, status: NeedStatus) = Need(id, person, NeedType.SMALL, "Socks", status = status)

    @Test
    fun communityTotalsCountOnlyVerifiedDeliveries() {
        val needs = listOf(
            small("a", "p1", NeedStatus.CONFIRMED),
            small("b", "p1", NeedStatus.CONFIRMED),            // same person twice: one person, two items
            small("c", "p2", NeedStatus.FULFILLED),            // delivered but not verified: not counted yet
            small("d", "p3", NeedStatus.OPEN),
            Need("e", "p4", NeedType.BIG, "Deposit", targetAmountChf = 900, fundedAmountChf = 300, status = NeedStatus.FUNDING),
        )
        val giveaways = listOf(Giveaway("g", "Brand", "Coats", "x", logisticsGoalChf = 100, fundedChf = 40))
        val impact = communityImpact(needs, giveaways)
        assertEquals(1, impact.peopleHelped)
        assertEquals(2, impact.itemsDelivered)
        assertEquals(340, impact.chfFunded)
    }

    @Test
    fun coverageIsTheShareOfNeedsThatAreFulfilled() {
        val needs = listOf(
            small("a", "p1", NeedStatus.CONFIRMED), small("b", "p2", NeedStatus.FULFILLED),
            small("c", "p3", NeedStatus.CLAIMED), small("d", "p4", NeedStatus.OPEN),
        )
        val c = needCoverage(needs)
        assertEquals(2, c.fulfilled); assertEquals(4, c.total); assertEquals(1, c.waiting)
        assertEquals(50, c.percent)
        assertEquals(0.5f, c.fraction)
        assertEquals(0, needCoverage(emptyList()).percent) // no division by zero
    }

    @Test
    fun theSeedHasSomethingToShow() {
        val impact = communityImpact(Seed.needs, SeedGiveaways.all)
        assertTrue(impact.peopleHelped >= 4)
        assertTrue(impact.chfFunded > 0)
        val c = needCoverage(Seed.needs)
        assertTrue(c.waiting > 0 && c.fulfilled > 0 && c.percent in 1..99)
    }
}

class CelebrationTest {
    private fun need(id: String, status: NeedStatus) = Need(id, "p", NeedType.SMALL, "Socks", status = status)

    @Test
    fun onlyConfirmedAndNotYetCelebratedDeliveriesAreShown() {
        val needs = listOf(need("a", NeedStatus.CONFIRMED), need("b", NeedStatus.FULFILLED), need("c", NeedStatus.CONFIRMED), need("d", NeedStatus.OPEN))
        val contributions = listOf(
            Contribution("d1", "a", ContributionKind.BROUGHT, null, 1),
            Contribution("d1", "a", ContributionKind.PAID, 10, 2),   // the same need twice: shown once
            Contribution("d1", "b", ContributionKind.BROUGHT, null, 3), // handed over, not confirmed yet: not shown
            Contribution("d1", "c", ContributionKind.BROUGHT, null, 4),
        )
        assertEquals(listOf("a", "c"), deliveriesToCelebrate(contributions, needs) { false }.map { it.id })
        assertEquals(listOf("c"), deliveriesToCelebrate(contributions, needs) { it == "a" }.map { it.id })
        assertTrue(deliveriesToCelebrate(contributions, needs) { true }.isEmpty())
    }
}

class VolunteerRewardsTest {
    private fun order(id: String, volunteer: String?, status: OrderStatus, zone: String = "z", person: String = "p", kind: OrderKind = OrderKind.HANDOVER) =
        DeliveryOrder(id, "n-$id", person, "Coat", zone, "d", "Dan", 1, status, volunteer, volunteer, kind = kind)

    private fun progress(orders: List<DeliveryOrder>, b: VolunteerBadge) = volunteerBadges(orders, "me").first { it.badge == b }

    @Test
    fun onlyMyDeliveredOrdersCount() {
        val orders = listOf(
            order("a", "me", OrderStatus.DELIVERED), order("b", "me", OrderStatus.DELIVERED),
            order("c", "me", OrderStatus.PICKED_UP), order("d", "other", OrderStatus.DELIVERED), order("e", null, OrderStatus.OPEN),
        )
        assertEquals(2, deliveriesDone(orders, "me"))
        assertEquals(1, deliveriesInProgress(orders, "me"))
    }

    @Test
    fun deliveryBadgesUnlockAtTheirTargetAndProgressIsCapped() {
        assertTrue(volunteerBadges(emptyList(), "me").none { it.unlocked })
        val orders = (1..6).map { order("o$it", "me", OrderStatus.DELIVERED, zone = "z$it", person = "p$it") }
        assertTrue(progress(orders, VolunteerBadge.FIRST_DELIVERY).unlocked)
        assertTrue(progress(orders, VolunteerBadge.FIVE_DELIVERIES).unlocked)
        assertFalse(progress(orders, VolunteerBadge.TEN_DELIVERIES).unlocked)
        assertEquals(6, progress(orders, VolunteerBadge.TEN_DELIVERIES).progress)
        assertEquals(5, progress(orders, VolunteerBadge.FIVE_DELIVERIES).progress)
    }

    @Test
    fun areasPeopleAndPurchasesHaveTheirOwnBadges() {
        val same = (1..4).map { order("s$it", "me", OrderStatus.DELIVERED, zone = "flon", person = "luc") } // four orders, one place, one person
        assertEquals(1, progress(same, VolunteerBadge.THREE_AREAS).progress)
        assertEquals(1, progress(same, VolunteerBadge.FIVE_PEOPLE).progress)
        assertEquals(0, progress(same, VolunteerBadge.FIRST_PURCHASE).progress)
        val spread = listOf(
            order("a", "me", OrderStatus.DELIVERED, zone = "z1", person = "p1"),
            order("b", "me", OrderStatus.DELIVERED, zone = "z2", person = "p2", kind = OrderKind.PURCHASE),
            order("c", "me", OrderStatus.DELIVERED, zone = "z3", person = "p3"),
        )
        assertTrue(progress(spread, VolunteerBadge.THREE_AREAS).unlocked)
        assertTrue(progress(spread, VolunteerBadge.FIRST_PURCHASE).unlocked)
        assertEquals(3, progress(spread, VolunteerBadge.FIVE_PEOPLE).progress)
    }

    @Test
    fun theChatIsOpenOnlyBetweenAcceptingAndDelivering() {
        val base = order("a", "me", OrderStatus.OPEN)
        assertFalse(base.chatOpen)
        assertTrue(base.copy(status = OrderStatus.ACCEPTED).chatOpen)
        assertTrue(base.copy(status = OrderStatus.PICKED_UP).chatOpen)
        assertFalse(base.copy(status = OrderStatus.DELIVERED).chatOpen)
    }

    @Test
    fun theBadgeSeedIsPersonalAndGrowsWithTheCount() {
        assertEquals(badgeSeed("Mia", 3), badgeSeed("  mia ", 3))   // same person, same count
        assertNotEquals(badgeSeed("Mia", 3), badgeSeed("Dan", 3))   // another person
        assertNotEquals(badgeSeed("Mia", 3), badgeSeed("Mia", 4))   // the pattern changes with every delivery
    }
}
