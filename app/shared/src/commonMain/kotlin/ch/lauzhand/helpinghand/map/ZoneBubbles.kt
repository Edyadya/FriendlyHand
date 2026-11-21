package ch.lauzhand.helpinghand.map

import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.NeedStatus
import ch.lauzhand.helpinghand.model.Person
import ch.lauzhand.helpinghand.model.Role

/**
 * The people shown for a zone: what the bubble counts and what the "People nearby" sheet lists, so the two
 * always agree. Each person appears once; their needs are shown inside their card, not counted separately.
 *
 * - DONOR: people with something still open (a zone with nobody to help would be a dead end).
 * - VOLUNTEER: everybody in the zone. A volunteer must always be able to reach a person, to confirm a
 *   delivery or to add a need, even when that person has nothing open any more.
 */
fun peopleInZone(role: Role, zoneId: String, people: List<Person>, needs: List<Need>): List<Person> {
    val inZone = people.filter { it.zoneId == zoneId }
    return when (role) {
        Role.VOLUNTEER -> inZone
        Role.DONOR -> inZone.filter { p ->
            needs.any { it.personId == p.id && (it.status == NeedStatus.OPEN || it.status == NeedStatus.FUNDING) }
        }
    }
}

/** The number on a zone's bubble: how many people are there (see [peopleInZone]), or null for no bubble. */
fun zoneBubbleCount(role: Role, zoneId: String, people: List<Person>, needs: List<Need>): Int? =
    peopleInZone(role, zoneId, people, needs).size.takeIf { it > 0 }
