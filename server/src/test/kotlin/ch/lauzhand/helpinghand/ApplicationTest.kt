package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.model.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {

    private fun api(block: suspend ApplicationTestBuilder.(io.ktor.client.HttpClient) -> Unit) = testApplication {
        application { module() }
        val client = createClient { install(ContentNegotiation) { json() } }
        block(client)
    }

    private suspend fun login(
        client: io.ktor.client.HttpClient, role: Role, name: String = "Test", code: String? = null,
    ): LoginResponse = client.post("/login") {
        contentType(ContentType.Application.Json); setBody(LoginRequest(name, role, code))
    }.body()

    private suspend fun volunteer(c: io.ktor.client.HttpClient) = login(c, Role.VOLUNTEER, "Vera", AccessCodes.VOLUNTEER)
    private suspend fun donor(c: io.ktor.client.HttpClient, name: String = "Dora") = login(c, Role.DONOR, name)
    private suspend fun ngo(c: io.ktor.client.HttpClient) = login(c, Role.NGO, "Nora", AccessCodes.NGO)

    private fun io.ktor.client.request.HttpRequestBuilder.auth(l: LoginResponse) =
        header(HttpHeaders.Authorization, "Bearer ${l.token}")

    @Test
    fun listsSeedData() = api { client ->
        assertEquals(Seed.zones, client.get("/zones").body<List<Zone>>())
        // anonymous callers (like donors) never get the exact last-seen data
        assertEquals(Seed.people.map { it.withoutLastSeen() }, client.get("/people").body<List<Person>>())
        assertEquals(Seed.needs, client.get("/needs").body<List<Need>>())
    }

    @Test
    fun claimSmallNeed() = api { client ->
        val d = donor(client)
        val res = client.post("/needs/n-1/claim") {
            auth(d); contentType(ContentType.Application.Json); setBody(ClaimRequest("ignored"))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val need = res.body<Need>()
        assertEquals(NeedStatus.CLAIMED, need.status)
        assertEquals(d.account.id, need.claimedByDonorId)
    }

    @Test
    fun donateCompletesBigNeed() = api { client ->
        // n-2: 320 / 800 funded
        val res = client.post("/needs/n-2/donate") {
            auth(donor(client)); contentType(ContentType.Application.Json); setBody(DonateRequest("d-1", 480))
        }
        val need = res.body<Need>()
        assertEquals(800, need.fundedAmountChf)
        assertEquals(NeedStatus.FULFILLED, need.status)
        assertEquals(NeedStatus.CONFIRMED, client.post("/needs/n-2/confirm").body<Need>().status)
    }

    @Test
    fun unknownNeedIs404() = api { client ->
        assertEquals(HttpStatusCode.NotFound, client.post("/needs/nope/fulfil") { auth(ngo(client)) }.status)
    }

    @Test
    fun createPersonAndNeed() = api { client ->
        val vol = volunteer(client)
        val person = client.post("/people") {
            auth(vol); contentType(ContentType.Application.Json)
            setBody(Person("", "Marc", null, "flon", "evenings", null, true, Seed.ME_VOLUNTEER))
        }
        assertEquals(HttpStatusCode.Created, person.status)
        val personId = person.body<Person>().id
        assertTrue(personId.isNotBlank())

        val need = client.post("/needs") {
            auth(vol); contentType(ContentType.Application.Json)
            setBody(Need("", personId, NeedType.SMALL, "Gloves"))
        }
        assertEquals(HttpStatusCode.Created, need.status)
        assertEquals(personId, need.body<Need>().personId)
    }

    // --- login and roles ---

    @Test
    fun wrongCodeIs401() = api { client ->
        for ((role, code) in listOf(Role.VOLUNTEER to "nope", Role.VOLUNTEER to null, Role.NGO to AccessCodes.VOLUNTEER)) {
            val res = client.post("/login") {
                contentType(ContentType.Application.Json); setBody(LoginRequest("X", role, code))
            }
            assertEquals(HttpStatusCode.Unauthorized, res.status, "$role / $code")
        }
    }

    @Test
    fun donorLoginNeedsNoCode() = api { client ->
        val l = donor(client, "Dora Smith")
        assertEquals(Role.DONOR, l.account.role)
        assertTrue(l.token.isNotBlank())
    }

    @Test
    fun donorCannotSeeExactLocationButVolunteerAndNgoCan() = api { client ->
        val d = donor(client)
        val asDonor = client.get("/people") { auth(d) }.body<List<Person>>()
        assertTrue(asDonor.all { it.lastSeenLat == null && it.lastSeenLng == null && it.lastSeenAtMillis == null && it.lastSeenNote == null })

        for (l in listOf(volunteer(client), ngo(client))) {
            val jerry = client.get("/people") { auth(l) }.body<List<Person>>().first { it.id == "p-jerry" }
            assertNotNull(jerry.lastSeenLat)
            assertNotNull(jerry.lastSeenLng)
            assertTrue(jerry.lastSeenVisible(nowMillis()))
        }
    }

    @Test
    fun lastSeenIsHiddenWhenOld() {
        val p = Seed.people.first { it.id == "p-jerry" }
        assertTrue(p.lastSeenVisible(nowMillis()))
        assertFalse(p.lastSeenVisible(nowMillis() + 7 * 3_600_000L))
        assertFalse(p.withoutLastSeen().lastSeenVisible(nowMillis()))
    }

    @Test
    fun volunteerUpdatesLastSeen() = api { client ->
        val vol = volunteer(client)
        val res = client.post("/people/p-luc/last-seen") {
            auth(vol); contentType(ContentType.Application.Json); setBody(LastSeenRequest(46.5, 6.6, "Near the Riponne"))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val luc = res.body<Person>()
        assertEquals(46.5, luc.lastSeenLat)
        assertTrue(luc.lastSeenVisible(nowMillis()))
        // a donor may not
        val denied = client.post("/people/p-luc/last-seen") {
            auth(donor(client)); contentType(ContentType.Application.Json); setBody(LastSeenRequest(1.0, 2.0))
        }
        assertEquals(HttpStatusCode.Forbidden, denied.status)
        assertEquals(HttpStatusCode.NotFound, client.post("/people/nope/last-seen") {
            auth(vol); contentType(ContentType.Application.Json); setBody(LastSeenRequest(1.0, 2.0))
        }.status)
    }

    @Test
    fun roleChecks() = api { client ->
        val json = ContentType.Application.Json
        // not logged in -> 401
        assertEquals(HttpStatusCode.Unauthorized, client.post("/needs/n-1/claim") {
            contentType(json); setBody(ClaimRequest("x"))
        }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/needs") {
            contentType(json); setBody(Need("", "p-luc", NeedType.SMALL, "Hat"))
        }.status)
        // wrong role -> 403
        val vol = volunteer(client)
        assertEquals(HttpStatusCode.Forbidden, client.post("/needs/n-1/claim") {
            auth(vol); contentType(json); setBody(ClaimRequest("x"))
        }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/needs/n-2/donate") {
            auth(vol); contentType(json); setBody(DonateRequest("x", 10))
        }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/needs") {
            auth(donor(client)); contentType(json); setBody(Need("", "p-luc", NeedType.SMALL, "Hat"))
        }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/people") {
            auth(ngo(client)); contentType(json)
            setBody(Person("", "Marc", null, "flon", "evenings", null, true, "x"))
        }.status)
    }

    @Test
    fun fulfilByNgoOrClaimingDonorOnly() = api { client ->
        val json = ContentType.Application.Json
        val claimer = donor(client, "Claire")
        client.post("/needs/n-1/claim") { auth(claimer); contentType(json); setBody(ClaimRequest("x")) }
        // another donor and a volunteer may not
        assertEquals(HttpStatusCode.Forbidden, client.post("/needs/n-1/fulfil") { auth(donor(client, "Other")) }.status)
        assertEquals(HttpStatusCode.Forbidden, client.post("/needs/n-1/fulfil") { auth(volunteer(client)) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.post("/needs/n-1/fulfil").status)
        // the claiming donor may
        assertEquals(NeedStatus.FULFILLED, client.post("/needs/n-1/fulfil") { auth(claimer) }.body<Need>().status)
        // the NGO may fulfil any need
        client.post("/needs/n-3/claim") { auth(claimer); contentType(json); setBody(ClaimRequest("x")) }
        assertEquals(NeedStatus.FULFILLED, client.post("/needs/n-3/fulfil") { auth(ngo(client)) }.body<Need>().status)
    }

    // --- donor contributions / rewards ---

    @Test
    fun completedGoalsCountOnlyFinishedContributions() = api { client ->
        val json = ContentType.Application.Json
        val d = donor(client, "Rita")
        suspend fun goals() = client.get("/donors/${d.account.id}/completed-goals") { auth(d) }.body<CompletedGoals>().count

        assertEquals(0, goals())
        client.post("/needs/n-1/claim") { auth(d); contentType(json); setBody(ClaimRequest("x")) }
        assertEquals(0, goals())                       // claimed, not delivered yet
        client.post("/needs/n-1/fulfil") { auth(d) }
        assertEquals(1, goals())
        client.post("/needs/n-2/donate") { auth(d); contentType(json); setBody(DonateRequest("x", 480)) }
        assertEquals(2, goals())                       // big goal fully funded
        client.post("/needs/n-2/donate") { auth(d); contentType(json); setBody(DonateRequest("x", 5)) }
        assertEquals(2, goals())                       // same need counted once
        // someone else's rewards are private
        assertEquals(HttpStatusCode.Forbidden,
            client.get("/donors/donor-other/completed-goals") { auth(d) }.status)
    }
}
