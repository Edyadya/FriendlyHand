package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.NeedStatus
import ch.lauzhand.helpinghand.model.pendingGoals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The demo loop judges watch: claim the coat, hand it over, volunteer confirms, reward count updates. */
class DemoLoopTest {
    @Test
    fun coatLoopUpdatesRewards() {
        val repo = FakeRepository()
        val coat = repo.needs.first { it.personId == "p-jerry" && it.title == "Winter coat" }
        assertEquals(NeedStatus.OPEN, coat.status)
        assertEquals(0, repo.completedGoalsFor(ME_DONOR))

        repo.claimNeed(coat.id, ME_DONOR)
        assertEquals(NeedStatus.CLAIMED, repo.needs.first { it.id == coat.id }.status)
        assertEquals(0, repo.completedGoalsFor(ME_DONOR))        // not yet delivered

        repo.markFulfilled(coat.id)
        assertEquals(NeedStatus.FULFILLED, repo.needs.first { it.id == coat.id }.status)
        assertEquals(0, repo.completedGoalsFor(ME_DONOR))        // delivered, but PENDING until a volunteer verifies it
        assertEquals(1, pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs))

        repo.confirmReceived(coat.id)
        assertEquals(NeedStatus.CONFIRMED, repo.needs.first { it.id == coat.id }.status)
        assertEquals(1, repo.completedGoalsFor(ME_DONOR))        // verified: now the reward screen shows 1
        assertEquals(0, pendingGoals(repo.contributionsOf(ME_DONOR), repo.needs))
    }

    @Test
    fun lastSeenUpdateIsVisibleToVolunteer() {
        val repo = FakeRepository()
        repo.updateLastSeen("p-luc", 46.52, 6.63, "Near the Riponne")
        val luc = repo.people.first { it.id == "p-luc" }
        assertEquals("Near the Riponne", luc.lastSeenNote)
        assertTrue(luc.lastSeenAtMillis != null)
    }

    @Test
    fun aPhotoNeedsConsentAndCanBeRemoved() {
        val repo = FakeRepository()
        repo.setPhoto("p-luc", "AAAA", consent = true)
        assertEquals("AAAA", repo.people.first { it.id == "p-luc" }.photo)

        // consent withdrawn: the photo is gone
        repo.setPhoto("p-luc", "AAAA", consent = false)
        val luc = repo.people.first { it.id == "p-luc" }
        assertTrue(luc.photo == null && !luc.photoConsent)
    }
}
