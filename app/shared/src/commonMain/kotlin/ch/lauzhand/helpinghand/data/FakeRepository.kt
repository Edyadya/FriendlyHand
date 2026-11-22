package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import ch.lauzhand.helpinghand.model.*
import kotlin.random.Random

interface HelpRepository : BookApi {
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

    /**
     * Sets or removes a person's photo (a small base64 JPEG). A photo is only kept with the person's own
     * [consent]: with consent = false (or photo = null) any photo is removed. Volunteers only.
     */
    fun setPhoto(personId: String, photoBase64: String?, consent: Boolean)

    // --- hand-over order book (the order state itself lives in BookApi) ---

    /**
     * A patron who claimed an item hands it over to a volunteer: this puts an order in the order book (once per
     * need). A volunteer accepts it, picks the item up and delivers it to the person.
     */
    fun requestHandover(needId: String, patronId: String, patronName: String) {
        if (orders.any { it.needId == needId && it.status != OrderStatus.DELIVERED }) return
        val need = needs.firstOrNull { it.id == needId } ?: return
        val person = people.firstOrNull { it.id == need.personId } ?: return
        saveOrder(
            DeliveryOrder(
                id = newId("o"), needId = needId, personId = person.id, itemTitle = need.title, zoneId = person.zoneId,
                patronId = patronId, patronName = patronName, createdAtMillis = nowMillis(),
            ),
        )
    }

    /** A volunteer takes an open order. */
    fun acceptOrder(orderId: String, volunteerId: String, volunteerName: String) {
        orders.firstOrNull { it.id == orderId }?.let { saveOrder(it.acceptedBy(volunteerId, volunteerName)) }
    }

    /** The volunteer has the item: the need counts as handed over. */
    fun markPickedUp(orderId: String) {
        val o = orders.firstOrNull { it.id == orderId } ?: return
        val picked = o.pickedUp()
        if (picked.status == OrderStatus.PICKED_UP) { saveOrder(picked); markFulfilled(o.needId) }
    }

    /** The person confirmed they received it: the need is verified and its order is closed. */
    fun confirmDelivered(needId: String) {
        confirmReceived(needId)
        orders.firstOrNull { it.needId == needId }?.let { saveOrder(it.delivered()) }
    }
}

fun newId(prefix: String) = "$prefix-${Random.nextInt(1_000_000)}"

const val ME_DONOR = Seed.ME_DONOR
const val ME_VOLUNTEER = Seed.ME_VOLUNTEER

// Seed data and status rules live in :core (shared with the server).
class FakeRepository(private val book: LocalBook = LocalBook()) : HelpRepository, BookApi by book {

    override val zones = Seed.zones

    override val people = Seed.people.toMutableStateList()

    override val needs = Seed.needs.toMutableStateList()

    // who helped with what, used for "My contributions" and rewards
    private val contributions = mutableStateListOf<Contribution>()

    init {
        // A few hand-overs already waiting in the order book, so volunteers have something to take.
        val demoPatrons = listOf("Lena", "Marc", "Sophie")
        // Only needs of London people who still have another open need, so the map counts do not change.
        fun stillOpen(n: Need) = n.status == NeedStatus.OPEN || n.status == NeedStatus.FUNDING
        needs.withIndex()
            .filter { (_, n) -> n.id.startsWith("n-ldn") && n.type == NeedType.SMALL && n.status == NeedStatus.OPEN && needs.any { o -> o.personId == n.personId && o.id != n.id && stillOpen(o) } }
            .distinctBy { (_, n) -> n.personId } // one order per person, so each keeps another open need
            .take(demoPatrons.size)
            .forEachIndexed { k, (i, n) ->
                needs[i] = n.claimedBy("donor-demo-$k")
                requestHandover(n.id, "donor-demo-$k", demoPatrons[k])
            }
    }

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

    override fun setPhoto(personId: String, photoBase64: String?, consent: Boolean) {
        val i = people.indexOfFirst { it.id == personId }
        if (i >= 0) people[i] = people[i].copy(photo = photoBase64?.takeIf { consent }, photoConsent = consent)
    }

    private fun update(id: String, change: (Need) -> Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = change(needs[i])
    }
}
