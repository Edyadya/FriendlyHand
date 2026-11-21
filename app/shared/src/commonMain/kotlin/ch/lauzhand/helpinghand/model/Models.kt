package ch.lauzhand.helpinghand.model

enum class Role { VOLUNTEER, DONOR, NGO }

enum class NeedType { SMALL, BIG }

// OPEN → CLAIMED (someone brings it / paid) or FUNDING (big goal partly funded)
//      → FULFILLED (delivered / fully funded) → CONFIRMED (person confirmed)
enum class NeedStatus { OPEN, CLAIMED, FUNDING, FULFILLED, CONFIRMED }

// x, y = position on the zone map, from 0 to 1 (0,0 = top-left)
data class Zone(val id: String, val name: String, val x: Float, val y: Float)

data class Person(
    val id: String,
    val firstName: String,
    val age: Int?,
    val zoneId: String,
    val usualTime: String,          // "mornings"
    val ownWords: String? = null,   // short sentence in their own words
    val consentGiven: Boolean,
    val createdByVolunteerId: String,
)

data class Need(
    val id: String,
    val personId: String,
    val type: NeedType,
    val title: String,
    val details: String? = null,
    val targetAmountChf: Int? = null,   // BIG only
    val fundedAmountChf: Int = 0,
    val status: NeedStatus = NeedStatus.OPEN,
    val claimedByDonorId: String? = null,
    val paid: Boolean = false,          // SMALL: paid instead of brought in person
)

data class Donor(val id: String, val name: String)