package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.model.NeedStatus
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
        assertEquals(1, repo.completedGoalsFor(ME_DONOR))        // reward screen shows 1

        repo.confirmReceived(coat.id)
        assertEquals(NeedStatus.CONFIRMED, repo.needs.first { it.id == coat.id }.status)
        assertEquals(1, repo.completedGoalsFor(ME_DONOR))        // still 1, never double counted
    }

    @Test
    fun lastSeenUpdateIsVisibleToVolunteer() {
        val repo = FakeRepository()
        repo.updateLastSeen("p-luc", 46.52, 6.63, "Near the Riponne")
        val luc = repo.people.first { it.id == "p-luc" }
        assertEquals("Near the Riponne", luc.lastSeenNote)
        assertTrue(luc.lastSeenAtMillis != null)
    }
}
