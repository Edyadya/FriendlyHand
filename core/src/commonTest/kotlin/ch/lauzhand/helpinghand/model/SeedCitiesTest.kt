package ch.lauzhand.helpinghand.model

import kotlin.test.*

class SeedCitiesTest {
    private fun peopleIn(city: City) = Seed.people.filter { p -> Seed.zones.first { it.id == p.zoneId }.cityId == city.id }

    @Test
    fun londonHasMuchMorePeopleThanLausanne() {
        val lausanne = peopleIn(Cities.LAUSANNE).size
        val london = peopleIn(Cities.LONDON).size
        assertTrue(london >= 3 * lausanne, "London $london vs Lausanne $lausanne")
    }

    @Test
    fun everyAreaInEveryCityHasSomeoneWhoStillNeedsHelp() {
        for (zone in Seed.zones) {
            val people = Seed.people.filter { it.zoneId == zone.id }
            assertTrue(people.isNotEmpty(), "nobody in ${zone.id}")
            assertTrue(people.any { p -> Seed.needs.any { it.personId == p.id && (it.status == NeedStatus.OPEN || it.status == NeedStatus.FUNDING) } }, "nothing open in ${zone.id}")
        }
    }

    @Test
    fun idsAreUniqueAndEveryNeedBelongsToSomeone() {
        assertEquals(Seed.people.size, Seed.people.map { it.id }.toSet().size)
        assertEquals(Seed.needs.size, Seed.needs.map { it.id }.toSet().size)
        assertEquals(Seed.zones.size, Seed.zones.map { it.id }.toSet().size)
        assertTrue(Seed.needs.all { n -> Seed.people.any { it.id == n.personId } })
    }

    @Test
    fun londonAreasSitInsideTheLondonBoxWithRealCoordinates() {
        for (z in Seed.zones.filter { it.cityId == "london" }) {
            assertNotNull(z.lat); assertNotNull(z.lng)
            assertTrue(z.x in 0f..1f && z.y in 0f..1f, "${z.id} is outside the drawn box")
            assertTrue(distanceKm(z.lat!!, z.lng!!, Cities.LONDON.lat, Cities.LONDON.lng) < 25, z.id)
        }
    }

    @Test
    fun distancesAreRoughlyRight() {
        // Lausanne to Geneva is about 52 km, Lausanne to London about 740 km
        assertTrue(distanceKm(46.519, 6.632, 46.2044, 6.1432) in 45.0..60.0)
        assertTrue(distanceKm(Cities.LAUSANNE.lat, Cities.LAUSANNE.lng, Cities.LONDON.lat, Cities.LONDON.lng) in 700.0..780.0)
    }

    @Test
    fun noMedicalWordsInNeedsOrOwnWords() {
        val banned = listOf("medic", "hiv", "drug", "addict", "illness", "disease", "mental", "diagnos", "therapy", "insurance", "health")
        val text = (Seed.needs.map { it.title + " " + (it.details ?: "") } + Seed.people.map { it.ownWords ?: "" }).joinToString(" ").lowercase()
        // "Health insurance arrears" is a money need from the original demo data, not a medical detail
        val withoutKnown = text.replace("health insurance arrears", "")
        for (w in banned) assertFalse(w in withoutKnown, "found '$w'")
    }
}

class SeedSpotsTest {
    @Test
    fun everyoneHasTheirOwnSetSpotInsideTheirArea() {
        val spots = Seed.people.map { it.lastSeenLat to it.lastSeenLng }
        assertTrue(Seed.people.all { it.lastSeenLat != null && it.lastSeenLng != null }, "someone has no spot")
        assertEquals(spots.size, spots.toSet().size, "two people share a spot")
        for (p in Seed.people) {
            val z = Seed.zones.first { it.id == p.zoneId }
            val c = Cities.byId(z.cityId)
            val lat = z.lat ?: (c.north - z.y * c.latSpan)
            val lng = z.lng ?: (c.west + z.x * c.lonSpan)
            assertTrue(distanceKm(lat, lng, p.lastSeenLat!!, p.lastSeenLng!!) < 1.0, "${p.id} is far from ${z.id}")
        }
    }

    @Test
    fun theSpotsAreRecentSoVolunteersSeeThem() {
        val now = nowMillis()
        assertTrue(Seed.people.all { it.lastSeenVisible(now) })
    }
}
