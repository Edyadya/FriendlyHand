package ch.lauzhand.helpinghand.model

// London demo data: much more than Lausanne (12 areas, 48 people). Everyday needs only, first names and their
// own words, no medical details. Generated from fixed lists, so it is the same on every run and every device.
internal object SeedLondon {
    private val city = Cities.LONDON

    private class Area(val id: String, val name: String, val lat: Double, val lng: Double)

    private val areas = listOf(
        Area("ldn-westminster", "Westminster", 51.4975, -0.1357),
        Area("ldn-camden", "Camden", 51.5390, -0.1426),
        Area("ldn-shoreditch", "Shoreditch", 51.5265, -0.0786),
        Area("ldn-southwark", "Southwark", 51.5035, -0.0910),
        Area("ldn-hackney", "Hackney", 51.5450, -0.0553),
        Area("ldn-brixton", "Brixton", 51.4613, -0.1156),
        Area("ldn-whitechapel", "Whitechapel", 51.5150, -0.0610),
        Area("ldn-islington", "Islington", 51.5362, -0.1030),
        Area("ldn-paddington", "Paddington", 51.5154, -0.1755),
        Area("ldn-stratford", "Stratford", 51.5416, -0.0035),
        Area("ldn-greenwich", "Greenwich", 51.4826, 0.0077),
        Area("ldn-hammersmith", "Hammersmith", 51.4927, -0.2230),
    )

    val zones: List<Zone> = areas.map {
        Zone(it.id, it.name, x = city.fractionX(it.lng), y = city.fractionY(it.lat), lat = it.lat, lng = it.lng, cityId = city.id)
    }

    private val names = listOf(
        "Daniel", "Aisha", "Tomasz", "Grace", "Kwame", "Elena", "Marcus", "Priya", "Liam", "Fatima", "Jorge", "Hannah",
        "Samir", "Chloe", "Andrei", "Zainab", "Callum", "Mei", "Oluwaseun", "Rosa", "Dev", "Katarzyna", "Jamal", "Freya",
        "Hassan", "Isla", "Mateo", "Nia", "Pavel", "Ruth", "Idris", "Tessa", "Viktor", "Amara", "Ravi", "Esme",
        "Yusuf", "Lena", "Bruno", "Selma", "Kofi", "Maya", "Arlo", "Noor", "Gideon", "Imani", "Stefan", "Joy",
    )
    private val times = listOf("mornings", "afternoons", "evenings")
    private val words = listOf(
        "I'm trying to get back on my feet.",
        "I worked in construction for years.",
        "I'd love to study again.",
        "I look after my dog. She keeps me going.",
        "I'm good with computers.",
        "I cook for the people I sleep near.",
        "I'm waiting for a flat to come through.",
        "I play guitar to pass the time.",
        "I was a bus driver.",
        "I'm learning English and I'm proud of it.",
    )
    private val smallNeeds = listOf(
        "Warm coat", "Sleeping bag", "Waterproof jacket", "Sturdy boots", "Thermal layers", "Backpack",
        "Hygiene kit", "Phone credit", "Hat and gloves", "Travel card top-up", "Hot meals voucher", "Blanket",
    )
    private val bigNeeds = listOf("Rent deposit" to 900, "Course fees" to 450, "Monthly travel card" to 180, "Work tools" to 350, "Passport renewal" to 120)

    val people: List<Person>
    val needs: List<Need>

    init {
        val ps = mutableListOf<Person>()
        val ns = mutableListOf<Need>()
        val now = nowMillis()
        names.forEachIndexed { i, name ->
            val area = areas[i % areas.size]
            val id = "p-ldn-${(i + 1).toString().padStart(2, '0')}"
            // every third person was seen recently: a spot a little off the middle of the area
            val seen = i % 3 == 0
            ps += Person(
                id = id, firstName = name, age = 19 + (i * 7) % 56, zoneId = area.id, usualTime = times[i % times.size],
                ownWords = if (i % 4 == 3) null else words[i % words.size],
                consentGiven = true, createdByVolunteerId = Seed.ME_VOLUNTEER,
                lastSeenLat = if (seen) area.lat + ((i % 5) - 2) * 0.0011 else null,
                lastSeenLng = if (seen) area.lng + ((i % 7) - 3) * 0.0013 else null,
                lastSeenNote = if (seen) "Near the usual spot" else null,
                lastSeenAtMillis = if (seen) now - (i % 5 + 1) * 1_200_000L else null,
            )
            var n = 0
            fun nid() = "n-ldn-${(i + 1).toString().padStart(2, '0')}-${++n}"
            ns += Need(nid(), id, NeedType.SMALL, smallNeeds[i % smallNeeds.size])
            if (i % 5 == 0) {
                val (title, target) = bigNeeds[(i / 5) % bigNeeds.size]
                ns += Need(nid(), id, NeedType.BIG, title, targetAmountChf = target, fundedAmountChf = target / 4, status = NeedStatus.FUNDING)
            } else if (i % 2 == 0) {
                ns += Need(nid(), id, NeedType.SMALL, smallNeeds[(i + 5) % smallNeeds.size])
            }
            // help that was already delivered and verified by a volunteer
            if (i % 6 == 0) {
                ns += Need(nid(), id, NeedType.SMALL, smallNeeds[(i + 3) % smallNeeds.size], status = NeedStatus.CONFIRMED, claimedByDonorId = "donor-history")
            }
        }
        people = ps
        needs = ns
    }
}
