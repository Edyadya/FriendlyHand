package ch.lauzhand.helpinghand.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotApplyConflictException
import ch.lauzhand.helpinghand.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * HelpRepository backed by the Ktor server. Same interface as FakeRepository, so screens don't change.
 *
 * The lists are Compose state: screens read them synchronously, the data is loaded in the background.
 * Actions update the local list immediately (optimistic) and then tell the server; if the server
 * refuses (wrong role, offline) the lists are reloaded and [lastError] is set.
 * The lists are also reloaded every few seconds, so changes made on other devices show up.
 *
 * The UI uses the constants ME_DONOR / ME_VOLUNTEER as "me". The server uses real account ids,
 * so ids are translated here: ME_* <-> currentAccount.id.
 */
class RemoteRepository(
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : HelpRepository {

    companion object {
        /** Android emulator: use "http://10.0.2.2:8081" instead of localhost. */
        const val DEFAULT_BASE_URL = "http://localhost:8081"

        /** How often the lists are reloaded to pick up changes from other devices. */
        const val POLL_MILLIS = 3_000L
    }

    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
        expectSuccess = false
    }

    private var token: String? = null

    override val zones = mutableStateListOf<Zone>()
    override val people = mutableStateListOf<Person>()
    override val needs = mutableStateListOf<Need>()
    private val completed = mutableStateMapOf<String, Int>()
    // the logged-in donor's own contributions (server ids)
    private val myContributions = mutableStateListOf<Contribution>()

    override var currentAccount: Account? by mutableStateOf(null)
        private set

    /** Last problem talking to the server (null = fine). Screens may show it. */
    var lastError: String? by mutableStateOf(null)
        private set

    // Server calls (reloads and actions) run one at a time, so an older answer never
    // overwrites a newer one.
    private val serverLock = Mutex()

    // Actions started (UI thread) and finished (under serverLock). A poll is skipped while
    // they differ, so it can't undo an optimistic change before the server has answered.
    @Volatile private var actionsStarted = 0
    @Volatile private var actionsDone = 0

    init {
        refresh()
        scope.launch {
            while (true) {
                delay(POLL_MILLIS)
                reload(skipIfActionPending = true)
            }
        }
    }

    // ---------- loading ----------

    fun refresh() {
        scope.launch { reload(skipIfActionPending = false) }
    }

    private suspend fun reload(skipIfActionPending: Boolean) = serverLock.withLock {
        if (skipIfActionPending && actionsStarted != actionsDone) return@withLock
        try {
            val z: List<Zone> = get("/zones")
            val p: List<Person> = get("/people")
            val n: List<Need> = get("/needs")
            val account = currentAccount
            val c = if (account?.role == Role.DONOR) {
                runCatching { get<CompletedGoals>("/donors/${account.id}/completed-goals").count }.getOrNull()
            } else null
            val mine = if (account?.role == Role.DONOR) {
                runCatching { get<ContributionList>("/donors/${account.id}/contributions").contributions }.getOrNull()
            } else emptyList()
            if (skipIfActionPending && actionsStarted != actionsDone) return@withLock
            applyAtomically {
                zones.replaceAll(z)
                people.replaceAll(p.map { it.fromServer() })
                needs.replaceAll(n.map { it.fromServer() })
                if (account != null && c != null) completed[account.id] = c
                if (mine != null && account == currentAccount) myContributions.replaceAll(mine)
                lastError = null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lastError = e.message ?: "No connection"
        }
    }

    /**
     * Applies all list changes at once, so the UI never sees half-updated lists. If the UI
     * changed the same lists meanwhile, Compose refuses the snapshot: retry, and as a last
     * resort write directly.
     */
    private fun applyAtomically(block: () -> Unit) {
        repeat(3) {
            try {
                Snapshot.withMutableSnapshot(block)
                return
            } catch (_: SnapshotApplyConflictException) {
                // someone else wrote meanwhile; try again on fresh state
            }
        }
        block()
    }

    // ---------- actions ----------

    override fun addPerson(person: Person) {
        people += person
        send("/people") {
            val saved = postSame("/people", person)
            replacePerson(person.id, saved.fromServer())
        }
    }

    override fun addNeed(need: Need) {
        needs += need
        send("/needs") {
            val saved = postSame("/needs", need)
            replaceNeed(need.id, saved.fromServer())
        }
    }

    override fun claimNeed(needId: String, donorId: String) {
        needs.firstOrNull { it.id == needId }?.let { myContributions += it.claimContribution(toServerId(donorId)) }
        changeNeed(needId) { it.claimedBy(donorId) }
        send("/needs/$needId/claim") {
            replaceNeed(needId, post<ClaimRequest, Need>("/needs/$needId/claim", ClaimRequest(donorId)).fromServer())
            refreshCompleted()
        }
    }

    override fun donate(needId: String, donorId: String, amountChf: Int) {
        needs.firstOrNull { it.id == needId }?.let { myContributions += it.donationContribution(toServerId(donorId), amountChf) }
        changeNeed(needId) { it.withDonation(donorId, amountChf) }
        send("/needs/$needId/donate") {
            replaceNeed(needId, post<DonateRequest, Need>("/needs/$needId/donate", DonateRequest(donorId, amountChf)).fromServer())
            refreshCompleted()
        }
    }

    override fun markFulfilled(needId: String) {
        changeNeed(needId) { it.fulfilled() }
        send("/needs/$needId/fulfil") {
            replaceNeed(needId, postNoBody<Need>("/needs/$needId/fulfil").fromServer())
            refreshCompleted()
        }
    }

    override fun confirmReceived(needId: String) {
        changeNeed(needId) { it.confirmed() }
        send("/needs/$needId/confirm") {
            replaceNeed(needId, postNoBody<Need>("/needs/$needId/confirm").fromServer())
            refreshCompleted()
        }
    }

    /** Cached count; refreshed after every action and on [refresh]. */
    override fun completedGoalsFor(donorId: String): Int = completed[toServerId(donorId)] ?: 0

    override fun contributionsOf(donorId: String): List<Contribution> {
        val id = toServerId(donorId)
        return myContributions.filter { it.donorId == id }
    }

    override suspend fun login(name: String, role: Role, code: String?): Result<Account> = try {
        val res = client.post("$baseUrl/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(name, role, code))
        }
        when {
            res.status == HttpStatusCode.Unauthorized -> Result.failure(IllegalArgumentException("Wrong access code"))
            !res.status.isSuccess() -> Result.failure(IllegalStateException("Login failed (${res.status.value})"))
            else -> {
                val body: LoginResponse = res.body()
                token = body.token
                currentAccount = body.account
                lastError = null
                refresh() // people now include the exact last-seen data for volunteers / NGOs
                Result.success(body.account)
            }
        }
    } catch (e: Exception) {
        Result.failure(IllegalStateException(e.message ?: "No connection"))
    }

    override fun logout() {
        token = null
        currentAccount = null
        myContributions.clear()
        refresh()
    }

    override fun updateLastSeen(personId: String, lat: Double, lng: Double, note: String?) {
        val i = people.indexOfFirst { it.id == personId }
        if (i >= 0) people[i] = people[i].seenAt(lat, lng, note)
        send("/people/$personId/last-seen") {
            replacePerson(personId, post<LastSeenRequest, Person>("/people/$personId/last-seen", LastSeenRequest(lat, lng, note)).fromServer())
        }
    }

    // ---------- helpers ----------

    private fun changeNeed(id: String, change: (Need) -> Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = change(needs[i])
    }

    private fun replaceNeed(id: String, need: Need) {
        val i = needs.indexOfFirst { it.id == id }
        if (i >= 0) needs[i] = need else needs += need
    }

    private fun replacePerson(id: String, person: Person) {
        val i = people.indexOfFirst { it.id == id }
        if (i >= 0) people[i] = person else people += person
    }

    private fun refreshCompleted() {
        val account = currentAccount?.takeIf { it.role == Role.DONOR } ?: return
        scope.launch {
            runCatching { get<CompletedGoals>("/donors/${account.id}/completed-goals").count }
                .onSuccess { completed[account.id] = it }
            runCatching { get<ContributionList>("/donors/${account.id}/contributions").contributions }
                .onSuccess { if (account == currentAccount) myContributions.replaceAll(it) }
        }
    }

    /** Runs a server call in the background; on failure reloads the lists so the screen shows the truth. */
    private fun send(what: String, block: suspend () -> Unit) {
        actionsStarted++
        scope.launch {
            serverLock.withLock {
                try {
                    block()
                    lastError = null
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    lastError = "$what: ${e.message ?: "failed"}"
                    refresh()
                } finally {
                    actionsDone++
                }
            }
        }
    }

    // me <-> real account id
    private fun toServerId(id: String): String {
        val a = currentAccount ?: return id
        return if ((id == ME_DONOR && a.role == Role.DONOR) || (id == ME_VOLUNTEER && a.role == Role.VOLUNTEER)) a.id else id
    }

    private fun Need.fromServer(): Need {
        val a = currentAccount
        return if (a?.role == Role.DONOR && claimedByDonorId == a.id) copy(claimedByDonorId = ME_DONOR) else this
    }

    private fun Person.fromServer(): Person {
        val a = currentAccount
        return if (a?.role == Role.VOLUNTEER && createdByVolunteerId == a.id) copy(createdByVolunteerId = ME_VOLUNTEER) else this
    }

    private fun <T> MutableList<T>.replaceAll(items: List<T>) { clear(); addAll(items) }

    private suspend inline fun <reified T> get(path: String): T {
        val res = client.get("$baseUrl$path") { authorize() }
        if (!res.status.isSuccess()) throw IllegalStateException("${res.status.value} ${res.status.description}")
        return res.body()
    }

    private suspend inline fun <reified B : Any, reified T> post(path: String, body: B): T {
        val res = client.post("$baseUrl$path") {
            authorize(); contentType(ContentType.Application.Json); setBody(body)
        }
        if (!res.status.isSuccess()) throw IllegalStateException("${res.status.value} ${res.status.description}")
        return res.body()
    }

    private suspend inline fun <reified B : Any> postSame(path: String, body: B): B = post<B, B>(path, body)

    private suspend inline fun <reified T> postNoBody(path: String): T {
        val res = client.post("$baseUrl$path") { authorize() }
        if (!res.status.isSuccess()) throw IllegalStateException("${res.status.value} ${res.status.description}")
        return res.body()
    }

    private fun HttpRequestBuilder.authorize() {
        token?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }
}
