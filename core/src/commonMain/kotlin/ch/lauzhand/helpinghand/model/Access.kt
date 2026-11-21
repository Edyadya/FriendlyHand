package ch.lauzhand.helpinghand.model

import kotlin.time.Clock

/** Access codes for login: the one place to change them. DONOR needs no code. */
object AccessCodes {
    const val VOLUNTEER = "LAUSANNE-2026"
    const val NGO = "NGO-2026"

    /** True if [code] is acceptable for [role]. */
    fun isValid(role: Role, code: String?): Boolean = when (role) {
        Role.DONOR -> true
        Role.VOLUNTEER -> code == VOLUNTEER
        Role.NGO -> code == NGO
    }
}

fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** True if the last-seen spot exists and is at most [maxAgeHours] old. Role checks are done by the caller. */
fun Person.lastSeenVisible(nowMillis: Long, maxAgeHours: Int = 6): Boolean {
    val at = lastSeenAtMillis ?: return false
    if (lastSeenLat == null || lastSeenLng == null) return false
    val ageMillis = nowMillis - at
    return ageMillis in 0..maxAgeHours * 3_600_000L
}

/** Copy without the exact last-seen data (what donors get). */
fun Person.withoutLastSeen(): Person =
    copy(lastSeenLat = null, lastSeenLng = null, lastSeenNote = null, lastSeenAtMillis = null)

/** The person seen at [lat]/[lng] right now. */
fun Person.seenAt(lat: Double, lng: Double, note: String?, nowMillis: Long = nowMillis()): Person =
    copy(lastSeenLat = lat, lastSeenLng = lng, lastSeenNote = note, lastSeenAtMillis = nowMillis)
