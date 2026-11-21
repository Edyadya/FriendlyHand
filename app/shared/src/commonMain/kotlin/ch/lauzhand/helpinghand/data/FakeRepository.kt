package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.mutableStateListOf
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

const val ME_DONOR = "donor-me"
const val ME_VOLUNTEER = "vol-me"

class FakeRepository : HelpRepository {

    override val zones = listOf(
        Zone("sallaz", "Sallaz", 0.72f, 0.18f),
        Zone("riponne", "Riponne", 0.52f, 0.32f),
        Zone("chauderon", "Chauderon", 0.32f, 0.40f),
        Zone("flon", "Flon", 0.48f, 0.48f),
        Zone("malley", "Malley", 0.12f, 0.50f),
        Zone("gare", "Gare", 0.52f, 0.62f),
        Zone("ouchy", "Ouchy", 0.50f, 0.76f),
    )

    override val people = mutableStateListOf(
        Person("p-jerry", "Jerry", 53, "flon", "mornings",
            "I used to be a cook. I'd like to work in a kitchen again.", true, ME_VOLUNTEER),
        Person("p-amina", "Amina", 34, "gare", "evenings",
            "I need my papers sorted to get a job.", true, ME_VOLUNTEER),
        Person("p-luc", "Luc", 61, "riponne", "afternoons", null, true, ME_VOLUNTEER),
    )

    override val needs = mutableStateListOf(
        Need("n-1", "p-jerry", NeedType.SMALL, "Winter coat", "Size L"),
        Need("n-2", "p-jerry", NeedType.BIG, "Health insurance arrears",
            targetAmountChf = 800, fundedAmountChf = 320, status = NeedStatus.FUNDING),
        Need("n-3", "p-amina", NeedType.SMALL, "Phone charger", "USB-C"),
        Need("n-4", "p-amina", NeedType.BIG, "ID papers renewal fee", targetAmountChf = 150),
        Need("n-5", "p-luc", NeedType.SMALL, "Warm socks"),
    )

    // (donorId, needId): who helped with what, used for rewards
    private val contributions = mutableStateListOf<Pair<String, String>>()

    override fun addPerson(person: Person) { people += person }
    override fun addNeed(need: Need) { needs += need }

    override fun claimNeed(needId: String, donorId: String) {
        update(needId) { it.copy(status = NeedStatus.CLAIMED, claimedByDonorId = donorId) }
        contributions += donorId to needId
    }

    override fun donate(needId: String, donorId: String, amountChf: Int) {
        update(needId) { n ->
            when (n.type) {
                NeedType.SMALL -> n.copy(status = NeedStatus.CLAIMED, claimedByDonorId = donorId, paid = true)
                NeedType.BIG -> {
                    val funded = n.fundedAmountChf + amountChf
                    val target = n.targetAmountChf ?: funded
                    n.copy(
                        fundedAmountChf = funded,
                        status = if (funded >= target) NeedStatus.FULFILLED else NeedStatus.FUNDING,
                    )
                }
            }
        }
        contributions += donorId to needId
    }

    override fun markFulfilled(needId: String) = update(needId) { it.copy(status = NeedStatus.FULFILLED) }

    override fun confirmReceived(needId: String) = update(needId) { it.copy(status = NeedStatus.CONFIRMED) }

    override fun completedGoalsFor(donorId: String): Int {
        val helped = contributions.filter { it.first == donorId }.map { it.second }.toSet()
        return needs.count {
            it.id in helped && (it.status == NeedStatus.FULFILLED || it.status == NeedStatus.CONFIRMED)
        }
    }

    private fun update(id: String, change: (Need) -> Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = change(needs[i])
    }
}