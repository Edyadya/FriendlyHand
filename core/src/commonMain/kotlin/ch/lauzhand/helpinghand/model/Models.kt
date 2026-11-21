package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role { VOLUNTEER, DONOR }

@Serializable
enum class NeedType { SMALL, BIG }

// OPEN → CLAIMED (someone brings it / paid) or FUNDING (big goal partly funded)
//      → FULFILLED (delivered / fully funded) → CONFIRMED (person confirmed)
@Serializable
enum class NeedStatus { OPEN, CLAIMED, FUNDING, FULFILLED, CONFIRMED }

// x, y = position on the zone map, from 0 to 1 (0,0 = top-left)
@Serializable
data class Zone(val id: String, val name: String, val x: Float, val y: Float)

@Serializable
data class Person(
    val id: String,
    val firstName: String,
    val age: Int?,
    val zoneId: String,
    val usualTime: String,          // "mornings"
    val ownWords: String? = null,   // short sentence in their own words
    val consentGiven: Boolean,
    val createdByVolunteerId: String,
    // Exact last-seen spot: only ever shown to VOLUNTEERs, see Person.lastSeenVisible
    val lastSeenLat: Double? = null,
    val lastSeenLng: Double? = null,
    val lastSeenNote: String? = null,
    val lastSeenAtMillis: Long? = null,
)

@Serializable
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

@Serializable
data class Donor(val id: String, val name: String)

// Request bodies for the server API
@Serializable
data class ClaimRequest(val donorId: String)

@Serializable
data class DonateRequest(val donorId: String, val amountChf: Int)

// --- Login and last-seen ---

@Serializable
data class Account(val id: String, val name: String, val role: Role)

@Serializable
data class LoginRequest(val name: String, val role: Role, val code: String? = null)

@Serializable
data class LoginResponse(val token: String, val account: Account)

@Serializable
data class LastSeenRequest(val lat: Double, val lng: Double, val note: String? = null)

/** Answer of GET /donors/{id}/completed-goals */
@Serializable
data class CompletedGoals(val donorId: String, val count: Int)

// --- Donor contributions ---

/** What a donor did for a need. */
@Serializable
enum class ContributionKind {
    BROUGHT, // claimed a small need to bring it in person
    PAID,    // paid for a small need
    GAVE,    // gave money towards a big goal
}

@Serializable
data class Contribution(
    val donorId: String,
    val needId: String,
    val kind: ContributionKind,
    val amountChf: Int? = null,
    val atMillis: Long = 0,
)

/** Answer of GET /donors/{id}/contributions */
@Serializable
data class ContributionList(val donorId: String, val contributions: List<Contribution>)
