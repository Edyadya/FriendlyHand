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

    @Test
    fun listsSeedData() = api { client ->
        assertEquals(Seed.zones, client.get("/zones").body<List<Zone>>())
        assertEquals(Seed.people, client.get("/people").body<List<Person>>())
        assertEquals(Seed.needs, client.get("/needs").body<List<Need>>())
    }

    @Test
    fun claimSmallNeed() = api { client ->
        val res = client.post("/needs/n-1/claim") {
            contentType(ContentType.Application.Json); setBody(ClaimRequest("d-1"))
        }
        assertEquals(HttpStatusCode.OK, res.status)
        val need = res.body<Need>()
        assertEquals(NeedStatus.CLAIMED, need.status)
        assertEquals("d-1", need.claimedByDonorId)
    }

    @Test
    fun donateCompletesBigNeed() = api { client ->
        // n-2: 320 / 800 funded
        val res = client.post("/needs/n-2/donate") {
            contentType(ContentType.Application.Json); setBody(DonateRequest("d-1", 480))
        }
        val need = res.body<Need>()
        assertEquals(800, need.fundedAmountChf)
        assertEquals(NeedStatus.FULFILLED, need.status)
        assertEquals(NeedStatus.CONFIRMED, client.post("/needs/n-2/confirm").body<Need>().status)
    }

    @Test
    fun unknownNeedIs404() = api { client ->
        assertEquals(HttpStatusCode.NotFound, client.post("/needs/nope/fulfil").status)
    }

    @Test
    fun createPersonAndNeed() = api { client ->
        val person = client.post("/people") {
            contentType(ContentType.Application.Json)
            setBody(Person("", "Marc", null, "flon", "evenings", null, true, Seed.ME_VOLUNTEER))
        }
        assertEquals(HttpStatusCode.Created, person.status)
        val personId = person.body<Person>().id
        assertTrue(personId.isNotBlank())

        val need = client.post("/needs") {
            contentType(ContentType.Application.Json)
            setBody(Need("", personId, NeedType.SMALL, "Gloves"))
        }
        assertEquals(HttpStatusCode.Created, need.status)
        assertEquals(personId, need.body<Need>().personId)
    }
}
