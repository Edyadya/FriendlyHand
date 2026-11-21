package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import ch.lauzhand.helpinghand.model.*
import kotlin.random.Random

interface HelpRepository {
    val zones: List<Zone>
    val people: List<Person>
    val needs: List<Need>

    fun addPerson(person: Person)
    fun addNeed(need: Need)
    fun claimNeed(needId: String, donorId: String)
    fun donate(needId: String, donorId: String, amountChf: Int)
    fun markFulfilled(needId: String)
    fun confirmReceived(needId: String)
    fun completedGoalsFor(donorId: String): Int

    /** What this donor did (brought / paid / gave), newest last. Backed by Compose state. */
    fun contributionsOf(donorId: String): List<Contribution>

    // --- login and last seen (added later; the functions above are unchanged) ---

    /** The logged-in account, or null. Backed by Compose state, so reading it in a composable recomposes. */
    val currentAccount: Account?

    /** Logs in; DONOR needs no code, VOLUNTEER needs the access code. Failure = wrong code or no connection. */
    suspend fun login(name: String, role: Role, code: String? = null): Result<Account>
    fun logout()

    /** Volunteers record where they last saw a person. The exact spot is only shown to VOLUNTEERs. */
    fun updateLastSeen(personId: String, lat: Double, lng: Double, note: String? = null)
}

fun newId(prefix: String) = "$prefix-${Random.nextInt(1_000_000)}"

const val ME_DONOR = Seed.ME_DONOR
const val ME_VOLUNTEER = Seed.ME_VOLUNTEER

// Seed data and status rules live in :core (shared with the server).
class FakeRepository : HelpRepository {

    override val zones = Seed.zones

    override val people = Seed.people.toMutableStateList()

    override val needs = Seed.needs.toMutableStateList()

    // who helped with what, used for "My contributions" and rewards
    private val contributions = mutableStateListOf<Contribution>()

    override fun addPerson(person: Person) { people += person }
    override fun addNeed(need: Need) { needs += need }

    override fun claimNeed(needId: String, donorId: String) {
        val need = needs.firstOrNull { it.id == needId } ?: return
        update(needId) { it.claimedBy(donorId) }
        contributions += need.claimContribution(donorId)
    }

    override fun donate(needId: String, donorId: String, amountChf: Int) {
        val need = needs.firstOrNull { it.id == needId } ?: return
        update(needId) { it.withDonation(donorId, amountChf) }
        contributions += need.donationContribution(donorId, amountChf)
    }

    override fun markFulfilled(needId: String) = update(needId) { it.fulfilled() }

    override fun confirmReceived(needId: String) = update(needId) { it.confirmed() }

    override fun completedGoalsFor(donorId: String): Int = completedGoals(contributionsOf(donorId), needs)

    override fun contributionsOf(donorId: String): List<Contribution> = contributions.filter { it.donorId == donorId }

    override var currentAccount: Account? by mutableStateOf(null)
        private set

    override suspend fun login(name: String, role: Role, code: String?): Result<Account> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("name must not be blank"))
        if (!AccessCodes.isValid(role, code)) return Result.failure(IllegalArgumentException("Wrong access code"))
        val id = when (role) { Role.DONOR -> ME_DONOR; Role.VOLUNTEER -> ME_VOLUNTEER }
        return Result.success(Account(id, name.trim(), role).also { currentAccount = it })
    }

    override fun logout() { currentAccount = null }

    override fun updateLastSeen(personId: String, lat: Double, lng: Double, note: String?) {
        val i = people.indexOfFirst { it.id == personId }
        if (i >= 0) people[i] = people[i].seenAt(lat, lng, note)
    }

    private fun update(id: String, change: (Need) -> Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = change(needs[i])
    }
}
