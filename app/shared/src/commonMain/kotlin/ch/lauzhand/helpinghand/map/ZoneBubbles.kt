package ch.lauzhand.helpinghand.map

import ch.lauzhand.helpinghand.model.Need
import ch.lauzhand.helpinghand.model.NeedStatus
import ch.lauzhand.helpinghand.model.Person
import ch.lauzhand.helpinghand.model.Role

/**
 * The number on a zone's map bubble, or null when the zone gets no bubble at all. One rule for both maps.
 *
 * - DONOR: bubbles only where something is still open, and the number is how many needs are open
 *   (zones with nothing to do would be dead ends).
 * - VOLUNTEER: a bubble for every zone that has people, and the number is how many people are there.
 *   A volunteer must always be able to reach a person, to confirm a delivery or to add a need,
 *   even when that person has nothing open any more.
 */
fun zoneBubbleCount(role: Role, zoneId: String, people: List<Person>, needs: List<Need>): Int? {
    val inZone = people.filter { it.zoneId == zoneId }
    return when (role) {
        Role.VOLUNTEER -> inZone.size.takeIf { it > 0 }
        Role.DONOR -> inZone
            .sumOf { p -> needs.count { it.personId == p.id && (it.status == NeedStatus.OPEN || it.status == NeedStatus.FUNDING) } }
            .takeIf { it > 0 }
    }
}
