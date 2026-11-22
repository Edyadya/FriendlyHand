package ch.lauzhand.helpinghand.model

import kotlinx.serialization.Serializable

/** The choices for the optional gender field. Nothing is chosen by default, and "prefer not to say" is one of them. */
@Serializable
enum class Gender { FEMALE, MALE, NON_BINARY, OTHER, PREFER_NOT_TO_SAY }

/**
 * What a patron or volunteer chooses to tell about themselves (all optional). It is only kept for their own
 * account and shown to nobody else.
 */
@Serializable
data class AccountProfile(
    val gender: Gender? = null,
    val city: String = "",
    val country: String = "",
) {
    /** "Lausanne, Switzerland", or just the part that is filled in, or null. */
    val location: String?
        get() = listOf(city.trim(), country.trim()).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { null }
}
