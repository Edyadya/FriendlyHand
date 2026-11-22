package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.data.ME_VOLUNTEER
import ch.lauzhand.helpinghand.model.*
import kotlin.test.*

class OrderBookTest {
    @Test
    fun theOrderBookStartsWithSomeOpenOrdersAndDoesNotChangeTheLausanneMap() {
        val repo = FakeRepository()
        val open = repo.orders.filter { it.status == OrderStatus.OPEN }
        assertTrue(open.size >= 3)
        // the people behind them still have another open need, so no bubble disappears
        for (o in open) assertTrue(repo.needs.any { it.personId == o.personId && it.id != o.needId && (it.status == NeedStatus.OPEN || it.status == NeedStatus.FUNDING) })
    }

    @Test
    fun aHandoverGoesThroughTheVolunteerToTheVerifiedDelivery() {
        val repo = FakeRepository()
        repo.claimNeed("n-1", ME_DONOR)
        repo.requestHandover("n-1", ME_DONOR, "Dan")
        repo.requestHandover("n-1", ME_DONOR, "Dan") // pressing twice makes one order
        val order = repo.orders.single { it.needId == "n-1" }
        assertEquals(OrderStatus.OPEN, order.status)
        assertEquals("Dan", order.patronName)

        repo.acceptOrder(order.id, ME_VOLUNTEER, "Mia")
        val taken = repo.orders.single { it.id == order.id }
        assertEquals("Mia", taken.volunteerName)
        assertEquals(NeedStatus.CLAIMED, repo.needs.first { it.id == "n-1" }.status) // not handed over yet

        repo.markPickedUp(order.id)
        assertEquals(OrderStatus.PICKED_UP, repo.orders.single { it.id == order.id }.status)
        assertEquals(NeedStatus.FULFILLED, repo.needs.first { it.id == "n-1" }.status)

        repo.confirmDelivered("n-1")
        assertEquals(OrderStatus.DELIVERED, repo.orders.single { it.id == order.id }.status)
        assertEquals(NeedStatus.CONFIRMED, repo.needs.first { it.id == "n-1" }.status)
    }

    @Test
    fun aVolunteerCannotPickUpAnOrderNobodyHasTaken() {
        val repo = FakeRepository()
        val open = repo.orders.first { it.status == OrderStatus.OPEN }
        repo.markPickedUp(open.id)
        assertEquals(OrderStatus.OPEN, repo.orders.first { it.id == open.id }.status)
        assertNotEquals(NeedStatus.FULFILLED, repo.needs.first { it.id == open.needId }.status)
    }

    @Test
    fun payingForAGiveawayCountsTowardsTheMoneyBadges() {
        val repo = FakeRepository()
        val g = repo.giveaways.first()
        val before = g.fundedChf
        repo.giveToGiveaway(g.id, ME_DONOR, 60)
        assertEquals(before + 60, repo.giveaways.first { it.id == g.id }.fundedChf)
        val paid = repo.giveawayContributionsOf(ME_DONOR).sumOf { it.amountChf }
        assertEquals(60, paid)
        val badges = donorAchievements(repo.contributionsOf(ME_DONOR), repo.needs, repo.people, paid)
        assertTrue(badges.first { it.id == AchievementId.GAVE_50 }.unlocked)
        assertFalse(badges.first { it.id == AchievementId.GAVE_100 }.unlocked)
    }

    @Test
    fun aGiveawayStopsAtItsGoal() {
        val repo = FakeRepository()
        val g = repo.giveaways.first()
        repo.giveToGiveaway(g.id, ME_DONOR, 100_000)
        val after = repo.giveaways.first { it.id == g.id }
        assertEquals(g.logisticsGoalChf, after.fundedChf)
        repo.giveToGiveaway(g.id, ME_DONOR, 10)
        assertEquals(g.logisticsGoalChf, repo.giveaways.first { it.id == g.id }.fundedChf)
    }

    @Test
    fun aProfilePictureCanBeSetAndRemoved() {
        val repo = FakeRepository()
        assertNull(repo.accountPhotoOf("acc"))
        repo.setAccountPhoto("acc", "QUJD")
        assertEquals("QUJD", repo.accountPhotoOf("acc"))
        repo.setAccountPhoto("acc", null)
        assertNull(repo.accountPhotoOf("acc"))
    }

    @Test
    fun theVolunteerPasswordIs1234() {
        assertTrue(AccessCodes.isValid(Role.VOLUNTEER, "1234"))
        assertFalse(AccessCodes.isValid(Role.VOLUNTEER, "LAUSANNE-2026"))
        assertFalse(AccessCodes.isValid(Role.VOLUNTEER, null))
        assertTrue(AccessCodes.isValid(Role.DONOR, null)) // a patron needs no password (account or guest)
    }

    @Test
    fun payingMoneyCountsAtOnceButABroughtItemWaitsForAVolunteer() {
        val repo = FakeRepository()
        repo.donate("n-1", ME_DONOR, 20)                 // pay for an item: no volunteer needed
        assertEquals(1, repo.completedGoalsFor(ME_DONOR))
        assertEquals(0, pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs))

        repo.claimNeed("n-3", ME_DONOR)                  // bring an item
        repo.markFulfilled("n-3")
        assertEquals(1, repo.completedGoalsFor(ME_DONOR)) // still just the paid one
        assertEquals(1, pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs))
        repo.confirmReceived("n-3")
        assertEquals(2, repo.completedGoalsFor(ME_DONOR))
    }

    @Test
    fun aProfileKeepsAnOptionalGenderAndLocation() {
        val repo = FakeRepository()
        assertEquals(AccountProfile(), repo.accountProfileOf("acc"))
        repo.setAccountProfile("acc", AccountProfile(Gender.FEMALE, " Lausanne ", "Switzerland"))
        val saved = repo.accountProfileOf("acc")
        assertEquals(Gender.FEMALE, saved.gender)
        assertEquals("Lausanne, Switzerland", saved.location)
        assertEquals(AccountProfile(), repo.accountProfileOf("someone-else")) // each account has its own
    }
}

class GuestTest {
    @Test
    fun onlyAPatronWhoContinuedAsGuestIsAGuest() {
        assertTrue(Account("a", "Guest", Role.DONOR).isGuest)
        assertTrue(Account("a", "Invité", Role.DONOR).isGuest)
        assertFalse(Account("a", "Dan", Role.DONOR).isGuest)
        assertFalse(Account("a", "Guest", Role.VOLUNTEER).isGuest) // a volunteer is never a guest
    }
}

class ImpactAppTest {
    @Test
    fun aConfirmedDeliveryIsCelebratedOnceAndTheTotalsGoUp() {
        val repo = FakeRepository()
        val before = communityImpact(repo.needs, repo.giveaways)
        repo.claimNeed("n-1", ME_DONOR)
        repo.requestHandover("n-1", ME_DONOR, "Dan")
        assertTrue(deliveriesToCelebrate(repo.contributionsOf(ME_DONOR), repo.needs) { repo.isCelebrated(it) }.isEmpty()) // not confirmed yet
        val order = repo.orders.single { it.needId == "n-1" }
        repo.acceptOrder(order.id, ME_VOLUNTEER, "Mia")
        repo.markPickedUp(order.id)
        repo.confirmDelivered("n-1")

        val fresh = deliveriesToCelebrate(repo.contributionsOf(ME_DONOR), repo.needs) { repo.isCelebrated(it) }
        assertEquals(listOf("n-1"), fresh.map { it.id })
        val after = communityImpact(repo.needs, repo.giveaways)
        assertEquals(before.peopleHelped + 1, after.peopleHelped)
        assertEquals(before.itemsDelivered + 1, after.itemsDelivered)

        repo.markCelebrated(fresh.map { it.id })
        assertTrue(deliveriesToCelebrate(repo.contributionsOf(ME_DONOR), repo.needs) { repo.isCelebrated(it) }.isEmpty())
    }
}

class PurchaseOrderTest {
    @Test
    fun payingForAnItemPutsAPurchaseOrderInTheOrderBook() {
        val repo = FakeRepository()
        repo.donate("n-1", ME_DONOR, 20)
        repo.requestPurchase("n-1", ME_DONOR, "Dan")
        repo.requestPurchase("n-1", ME_DONOR, "Dan") // once per item
        val order = repo.orders.single { it.needId == "n-1" }
        assertEquals(OrderKind.PURCHASE, order.kind)
        assertEquals(OrderStatus.OPEN, order.status)

        repo.acceptOrder(order.id, ME_VOLUNTEER, "Mia")
        repo.markPickedUp(order.id)                 // "I bought it"
        repo.confirmDelivered("n-1")                // the person received it
        assertEquals(OrderStatus.DELIVERED, repo.orders.single { it.id == order.id }.status)
        assertEquals(NeedStatus.CONFIRMED, repo.needs.first { it.id == "n-1" }.status)
        assertEquals(1, deliveriesDone(repo.orders, ME_VOLUNTEER))
    }

    @Test
    fun aHandoverOrderIsStillTheDefaultKind() {
        val repo = FakeRepository()
        repo.claimNeed("n-3", ME_DONOR)
        repo.requestHandover("n-3", ME_DONOR, "Dan")
        assertEquals(OrderKind.HANDOVER, repo.orders.single { it.needId == "n-3" }.kind)
    }
}

