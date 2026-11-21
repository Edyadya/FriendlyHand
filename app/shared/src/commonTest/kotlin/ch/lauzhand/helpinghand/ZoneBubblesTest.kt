package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.map.peopleInZone
import ch.lauzhand.helpinghand.map.zoneBubbleCount
import ch.lauzhand.helpinghand.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZoneBubblesTest {
    @Test
    fun donorsOnlySeeZonesWithOpenNeeds() {
        val repo = FakeRepository()
        // the number is PEOPLE, not needs: Jerry has 2 open needs but is one person
        assertEquals(1, zoneBubbleCount(Role.DONOR, "flon", repo.people, repo.needs))
        assertEquals(1, zoneBubbleCount(Role.DONOR, "riponne", repo.people, repo.needs))
        assertNull(zoneBubbleCount(Role.DONOR, "ouchy", repo.people, repo.needs))        // nobody there
    }

    @Test
    fun volunteerCanStillReachAPersonWhoseNeedsAreDone() {
        val repo = FakeRepository()
        val socks = repo.needs.first { it.personId == "p-luc" }
        repo.claimNeed(socks.id, ME_DONOR)
        repo.markFulfilled(socks.id)   // Luc now has nothing open, only a delivery waiting to be confirmed

        // donors have nothing to do in Riponne any more, so no bubble ...
        assertNull(zoneBubbleCount(Role.DONOR, "riponne", repo.people, repo.needs))
        // ... but the volunteer must still reach Luc to confirm the delivery
        assertEquals(1, zoneBubbleCount(Role.VOLUNTEER, "riponne", repo.people, repo.needs))
        // zones without people never get a bubble
        assertNull(zoneBubbleCount(Role.VOLUNTEER, "ouchy", repo.people, repo.needs))
    }

    @Test
    fun theSheetListsExactlyThePeopleTheBubbleCounts() {
        val repo = FakeRepository()
        for (role in listOf(Role.DONOR, Role.VOLUNTEER)) for (z in repo.zones) {
            val listed = peopleInZone(role, z.id, repo.people, repo.needs)
            assertEquals(listed.size.takeIf { it > 0 }, zoneBubbleCount(role, z.id, repo.people, repo.needs))
            assertEquals(listed.size, listed.map { it.id }.toSet().size) // each person once
        }
    }

    @Test
    fun aPersonWithTwoNeedsCountsOnce() {
        val repo = FakeRepository()
        assertEquals(2, repo.needs.count { it.personId == "p-jerry" })
        assertEquals(1, peopleInZone(Role.DONOR, "flon", repo.people, repo.needs).size)
    }
}
