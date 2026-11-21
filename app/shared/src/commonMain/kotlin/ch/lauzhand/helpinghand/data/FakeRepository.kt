package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.mutableStateListOf
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
}

fun newId(prefix: String) = "$prefix-${Random.nextInt(1_000_000)}"

const val ME_DONOR = Seed.ME_DONOR
const val ME_VOLUNTEER = Seed.ME_VOLUNTEER

// Seed data and status rules live in :core (shared with the server).
class FakeRepository : HelpRepository {

    override val zones = Seed.zones

    override val people = Seed.people.toMutableStateList()

    override val needs = Seed.needs.toMutableStateList()

    // (donorId, needId): who helped with what, used for rewards
    private val contributions = mutableStateListOf<Pair<String, String>>()

    override fun addPerson(person: Person) { people += person }
    override fun addNeed(need: Need) { needs += need }

    override fun claimNeed(needId: String, donorId: String) {
        update(needId) { it.claimedBy(donorId) }
        contributions += donorId to needId
    }

    override fun donate(needId: String, donorId: String, amountChf: Int) {
        update(needId) { it.withDonation(donorId, amountChf) }
        contributions += donorId to needId
    }

    override fun markFulfilled(needId: String) = update(needId) { it.fulfilled() }

    override fun confirmReceived(needId: String) = update(needId) { it.confirmed() }

    override fun completedGoalsFor(donorId: String): Int {
        val helped = contributions.filter { it.first == donorId }.map { it.second }.toSet()
        return needs.count { it.id in helped && it.isCompleted }
    }

    private fun update(id: String, change: (Need) -> Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = change(needs[i])
    }
}
