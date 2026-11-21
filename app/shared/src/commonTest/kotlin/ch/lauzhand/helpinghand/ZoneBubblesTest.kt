package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.data.ME_DONOR
import ch.lauzhand.helpinghand.map.peopleInZone
import ch.lauzhand.helpinghand.map.zoneBubbleCount
import ch.lauzhand.helpinghand.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZoneBubblesTest {
    @Test
    fun theNumberIsPeopleNotNeeds() {
        val repo = FakeRepository()
        // Jerry has two open needs but is one person; Tom is the second person in the Flon
        assertEquals(2, zoneBubbleCount(Role.DONOR, "flon", repo.people, repo.needs))
        assertEquals(2, repo.needs.count { it.personId == "p-jerry" && it.status.name != "CONFIRMED" })
        assertEquals(1, zoneBubbleCount(Role.DONOR, "malley", repo.people, repo.needs)) // Karim
    }

    @Test
    fun noPeopleNoBubble() {
        val repo = FakeRepository()
        assertNull(zoneBubbleCount(Role.DONOR, "ouchy", emptyList(), repo.needs))
        assertNull(zoneBubbleCount(Role.VOLUNTEER, "ouchy", emptyList(), repo.needs))
    }

    @Test
    fun everyAreaHasSomeoneToHelp() {
        val repo = FakeRepository()
        for (z in repo.zones) assertTrue((zoneBubbleCount(Role.DONOR, z.id, repo.people, repo.needs) ?: 0) >= 1, z.id)
    }

    @Test
    fun volunteerCanStillReachAPersonWhoseNeedsAreDone() {
        val repo = FakeRepository()
        // Karim (Malley) has one open need; once it is delivered he has nothing open, only a delivery to verify
        val boots = repo.needs.first { it.personId == "p-karim" && it.title == "Work boots" }
        repo.claimNeed(boots.id, ME_DONOR)
        repo.markFulfilled(boots.id)

        // donors have nothing to do in Malley any more, so no bubble ...
        assertNull(zoneBubbleCount(Role.DONOR, "malley", repo.people, repo.needs))
        // ... but the volunteer must still reach Karim to verify the delivery
        assertEquals(1, zoneBubbleCount(Role.VOLUNTEER, "malley", repo.people, repo.needs))
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
}
