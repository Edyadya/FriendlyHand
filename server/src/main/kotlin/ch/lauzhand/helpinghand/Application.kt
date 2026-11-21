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
    }
    install(StatusPages) {
        exception<BadRequestException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Bad request")))
        }
        exception<IllegalArgumentException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to (e.message ?: "Bad request")))
        }
    }

    routing {
        get("/") { call.respondText("HelpingHand API") }

        get("/zones") { call.respond(store.zones()) }
        get("/people") { call.respond(store.people()) }
        get("/needs") { call.respond(store.needs()) }

        post("/people") {
            call.respond(HttpStatusCode.Created, store.addPerson(call.receive<Person>()))
        }
        post("/needs") {
            call.respond(HttpStatusCode.Created, store.addNeed(call.receive<Need>()))
        }

        route("/needs/{id}") {
            post("/claim") {
                val req = call.receive<ClaimRequest>()
                call.respondNeed(store.updateNeed(call.parameters["id"]!!) { it.claimedBy(req.donorId) })
            }
            post("/donate") {
                val req = call.receive<DonateRequest>()
                require(req.amountChf > 0) { "amountChf must be positive" }
                call.respondNeed(store.updateNeed(call.parameters["id"]!!) {
                    it.withDonation(req.donorId, req.amountChf)
                })
            }
            post("/fulfil") {
                call.respondNeed(store.updateNeed(call.parameters["id"]!!) { it.fulfilled() })
            }
            post("/confirm") {
                call.respondNeed(store.updateNeed(call.parameters["id"]!!) { it.confirmed() })
            }
        }
    }
}

private suspend fun ApplicationCall.respondNeed(need: Need?) {
    if (need == null) respond(HttpStatusCode.NotFound, mapOf("error" to "Need not found"))
    else respond(need)
}
