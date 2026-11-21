package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.model.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlin.random.Random

fun main() {
    embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/** Thread-safe in-memory store, seeded with the same demo data as the app's FakeRepository. */
class Store {
    private val lock = Any()
    private val zones = Seed.zones
    private val people = Seed.people.toMutableList()
    private val needs = Seed.needs.toMutableList()

    // (donorId, needId): who helped with what, used for rewards
    private val contributions = mutableListOf<Pair<String, String>>()
    private val tokens = mutableMapOf<String, Account>()

    fun zones(): List<Zone> = zones
    fun people(): List<Person> = synchronized(lock) { people.toList() }
    fun needs(): List<Need> = synchronized(lock) { needs.toList() }

    fun addPerson(person: Person): Person = synchronized(lock) {
        val p = if (person.id.isBlank()) person.copy(id = newId("p")) else person
        require(people.none { it.id == p.id }) { "Person ${p.id} already exists" }
        require(zones.any { it.id == p.zoneId }) { "Unknown zone ${p.zoneId}" }
        people += p
        p
    }

    fun addNeed(need: Need): Need = synchronized(lock) {
        val n = if (need.id.isBlank()) need.copy(id = newId("n")) else need
        require(needs.none { it.id == n.id }) { "Need ${n.id} already exists" }
        require(people.any { it.id == n.personId }) { "Unknown person ${n.personId}" }
        needs += n
        n
    }

    /** Applies [change] to the need with [id]; returns the updated need, or null if not found. */
    fun updateNeed(id: String, change: (Need) -> Need): Need? = synchronized(lock) {
        val i = needs.indexOfFirst { it.id == id }
        if (i < 0) return null
        change(needs[i]).also { needs[i] = it }
    }

    fun addContribution(donorId: String, needId: String) = synchronized(lock) {
        contributions += donorId to needId
    }

    /** Number of distinct completed needs this donor helped with. */
    fun completedGoalsFor(donorId: String): Int = synchronized(lock) {
        val helped = contributions.filter { it.first == donorId }.map { it.second }.toSet()
        needs.count { it.id in helped && it.isCompleted }
    }

    fun updatePerson(id: String, change: (Person) -> Person): Person? = synchronized(lock) {
        val i = people.indexOfFirst { it.id == id }
        if (i < 0) return null
        change(people[i]).also { people[i] = it }
    }

    /** Creates an account for the login and returns a new token for it. Ids are stable per role and name. */
    fun login(name: String, role: Role): LoginResponse = synchronized(lock) {
        val slug = name.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "user" }
        val prefix = when (role) { Role.DONOR -> "donor"; Role.VOLUNTEER -> "vol"; Role.NGO -> "ngo" }
        val account = Account("$prefix-$slug", name.trim(), role)
        val token = java.util.UUID.randomUUID().toString()
        tokens[token] = account
        LoginResponse(token, account)
    }

    fun accountForToken(token: String): Account? = synchronized(lock) { tokens[token] }

    private fun newId(prefix: String) = "$prefix-${Random.nextInt(1_000_000)}"
}

fun Application.module(store: Store = Store()) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
    }
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
    }
    install(StatusPages) {
        exception<BadRequestException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Bad request")))
        }
        exception<UnauthorizedException> { call, e ->
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to (e.message ?: "Unauthorized")))
        }
        exception<ForbiddenException> { call, e ->
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to (e.message ?: "Forbidden")))
        }
        exception<IllegalArgumentException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Bad request")))
        }
    }

    routing {
        get("/") { call.respondText("HelpingHand API") }

        post("/login") {
            val req = call.receive<LoginRequest>()
            require(req.name.isNotBlank()) { "name must not be blank" }
            if (!AccessCodes.isValid(req.role, req.code)) throw UnauthorizedException("Wrong access code")
            call.respond(store.login(req.name, req.role))
        }

        get("/zones") { call.respond(store.zones()) }
        get("/people") {
            // exact last-seen data only for volunteers and NGOs
            val role = call.account(store)?.role
            val people = store.people()
            call.respond(if (role == Role.VOLUNTEER || role == Role.NGO) people else people.map { it.withoutLastSeen() })
        }
        get("/needs") { call.respond(store.needs()) }

        get("/donors/{id}/completed-goals") {
            val account = call.requireRole(store, Role.DONOR)
            val id = call.parameters["id"]!!
            if (id != account.id) throw ForbiddenException("Only your own rewards")
            call.respond(CompletedGoals(id, store.completedGoalsFor(id)))
        }

        post("/people") {
            val account = call.requireRole(store, Role.VOLUNTEER)
            val person = call.receive<Person>()
            call.respond(HttpStatusCode.Created, store.addPerson(person.copy(createdByVolunteerId = account.id)))
        }
        post("/people/{id}/last-seen") {
            call.requireRole(store, Role.VOLUNTEER)
            val req = call.receive<LastSeenRequest>()
            val person = store.updatePerson(call.parameters["id"]!!) { it.seenAt(req.lat, req.lng, req.note) }
            if (person == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "Person not found"))
            else call.respond(person)
        }
        post("/needs") {
            call.requireRole(store, Role.VOLUNTEER)
            call.respond(HttpStatusCode.Created, store.addNeed(call.receive<Need>()))
        }

        route("/needs/{id}") {
            post("/claim") {
                val account = call.requireRole(store, Role.DONOR)
                call.receive<ClaimRequest>() // body kept for compatibility; the caller's account is the donor
                val id = call.parameters["id"]!!
                val need = store.updateNeed(id) { it.claimedBy(account.id) }
                if (need != null) store.addContribution(account.id, id)
                call.respondNeed(need)
            }
            post("/donate") {
                val account = call.requireRole(store, Role.DONOR)
                val req = call.receive<DonateRequest>()
                require(req.amountChf > 0) { "amountChf must be positive" }
                val id = call.parameters["id"]!!
                val need = store.updateNeed(id) { it.withDonation(account.id, req.amountChf) }
                if (need != null) store.addContribution(account.id, id)
                call.respondNeed(need)
            }
            post("/fulfil") {
                val account = call.requireAccount(store)
                val id = call.parameters["id"]!!
                val allowed = account.role == Role.NGO ||
                    (account.role == Role.DONOR && store.needs().firstOrNull { it.id == id }?.claimedByDonorId == account.id)
                if (!allowed) throw ForbiddenException("Only the NGO or the donor who claimed it can mark it delivered")
                call.respondNeed(store.updateNeed(id) { it.fulfilled() })
            }
            post("/confirm") {
                call.respondNeed(store.updateNeed(call.parameters["id"]!!) { it.confirmed() })
            }
        }
    }
}

class UnauthorizedException(message: String) : RuntimeException(message)
class ForbiddenException(message: String) : RuntimeException(message)

/** The account behind "Authorization: Bearer <token>", or null. */
private fun ApplicationCall.account(store: Store): Account? =
    request.headers[HttpHeaders.Authorization]
        ?.removePrefix("Bearer ")?.takeIf { it != request.headers[HttpHeaders.Authorization] }
        ?.let { store.accountForToken(it.trim()) }

private fun ApplicationCall.requireAccount(store: Store): Account =
    account(store) ?: throw UnauthorizedException("Please log in")

private fun ApplicationCall.requireRole(store: Store, role: Role): Account {
    val account = requireAccount(store)
    if (account.role != role) throw ForbiddenException("This needs the ${role.name} role")
    return account
}

private suspend fun ApplicationCall.respondNeed(need: Need?) {
    if (need == null) respond(HttpStatusCode.NotFound, mapOf("error" to "Need not found"))
    else respond(need)
}
