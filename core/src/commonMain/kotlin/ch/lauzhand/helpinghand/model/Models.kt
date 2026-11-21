package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role { VOLUNTEER, DONOR, NGO }

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
