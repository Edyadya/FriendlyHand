package ch.lauzhand.helpinghand.model

// Demo data shared by FakeRepository (app) and the server's in-memory store.
object Seed {
    const val ME_DONOR = "donor-me"
    const val ME_VOLUNTEER = "vol-me"

    private val lausanneZones: List<Zone> = listOf(
        Zone("sallaz", "Sallaz", 0.72f, 0.18f),
        Zone("riponne", "Riponne", 0.52f, 0.32f),
        Zone("chauderon", "Chauderon", 0.32f, 0.40f),
        Zone("flon", "Flon", 0.48f, 0.48f),
        Zone("malley", "Malley", 0.12f, 0.50f),
        Zone("gare", "Gare", 0.52f, 0.62f),
        Zone("ouchy", "Ouchy", 0.50f, 0.76f),
    )

    private val lausannePeople: List<Person> = listOf(
        Person("p-jerry", "Jerry", 53, "flon", "mornings",
            "I used to be a cook. I'd like to work in a kitchen again.", true, ME_VOLUNTEER,
            // last seen about 1 hour ago near the Flon, Lausanne
            lastSeenLat = 46.5211, lastSeenLng = 6.6268, lastSeenNote = "By the Flon bridge",
            lastSeenAtMillis = nowMillis() - 3_600_000L),
        Person("p-amina", "Amina", 34, "gare", "evenings",
            "I need my papers sorted to get a job.", true, ME_VOLUNTEER),
        Person("p-luc", "Luc", 61, "riponne", "afternoons", null, true, ME_VOLUNTEER),
        // More people, so every area has someone. Everyday needs only, in their own words.
        Person("p-marco", "Marco", 47, "sallaz", "evenings",
            "I fix bikes. Give me a pump and I'm happy.", true, ME_VOLUNTEER,
            lastSeenLat = 46.5301, lastSeenLng = 6.6437, lastSeenNote = "Near the bus stop",
            lastSeenAtMillis = nowMillis() - 2_400_000L),
        Person("p-sofia", "Sofia", 29, "chauderon", "mornings",
            "I'm saving for a room of my own.", true, ME_VOLUNTEER),
        Person("p-karim", "Karim", 38, "malley", "afternoons", null, true, ME_VOLUNTEER),
        Person("p-elena", "Elena", 56, "ouchy", "mornings",
            "I sit by the lake every day. It keeps me calm.", true, ME_VOLUNTEER,
            lastSeenLat = 46.5086, lastSeenLng = 6.6279, lastSeenNote = "On the lakeside bench",
            lastSeenAtMillis = nowMillis() - 5_400_000L),
        Person("p-tom", "Tom", 24, "flon", "evenings",
            "I'd like to learn carpentry.", true, ME_VOLUNTEER),
        Person("p-nadia", "Nadia", 41, "gare", "mornings",
            "I speak four languages and I'm looking for work.", true, ME_VOLUNTEER),
        Person("p-pedro", "Pedro", 63, "riponne", "mornings",
            "I drove trucks for thirty years.", true, ME_VOLUNTEER),
    )

    private val lausanneNeeds: List<Need> = listOf(
        Need("n-1", "p-jerry", NeedType.SMALL, "Winter coat", "Size L"),
        Need("n-2", "p-jerry", NeedType.BIG, "Health insurance arrears",
            targetAmountChf = 800, fundedAmountChf = 320, status = NeedStatus.FUNDING),
        Need("n-3", "p-amina", NeedType.SMALL, "Phone charger", "USB-C"),
        Need("n-4", "p-amina", NeedType.BIG, "ID papers renewal fee", targetAmountChf = 150),
        Need("n-5", "p-luc", NeedType.SMALL, "Warm socks"),
        Need("n-6", "p-marco", NeedType.SMALL, "Sleeping bag", "Warm, for the winter"),
        Need("n-7", "p-marco", NeedType.SMALL, "Bike pump"),
        Need("n-8", "p-sofia", NeedType.BIG, "Rent deposit",
            targetAmountChf = 600, fundedAmountChf = 150, status = NeedStatus.FUNDING),
        Need("n-9", "p-karim", NeedType.SMALL, "Work boots", "Size 43"),
        Need("n-10", "p-elena", NeedType.SMALL, "Warm gloves"),
        Need("n-11", "p-elena", NeedType.SMALL, "Phone credit", "A CHF 20 top-up"),
        Need("n-12", "p-tom", NeedType.SMALL, "Clothes for an interview"),
        Need("n-13", "p-tom", NeedType.BIG, "Public transport pass", targetAmountChf = 120),
        Need("n-14", "p-nadia", NeedType.BIG, "Language course fees",
            targetAmountChf = 300, fundedAmountChf = 90, status = NeedStatus.FUNDING),
        Need("n-15", "p-pedro", NeedType.SMALL, "Thermos flask"),
        // Help that was already delivered and verified by a volunteer: it counts in "people helped overall".
        Need("n-16", "p-pedro", NeedType.SMALL, "Winter boots", status = NeedStatus.CONFIRMED, claimedByDonorId = "donor-history"),
        Need("n-17", "p-nadia", NeedType.SMALL, "Passport photos", status = NeedStatus.CONFIRMED, claimedByDonorId = "donor-history"),
        Need("n-18", "p-karim", NeedType.SMALL, "Phone charger", status = NeedStatus.CONFIRMED, claimedByDonorId = "donor-history"),
        Need("n-19", "p-luc", NeedType.SMALL, "Wool hat", status = NeedStatus.CONFIRMED, claimedByDonorId = "donor-history"),
    )

    // Lausanne and London together (declared last, so the lists above are ready).
    val zones: List<Zone> = lausanneZones + SeedLondon.zones
    val people: List<Person> = lausannePeople + SeedLondon.people
    val needs: List<Need> = lausanneNeeds + SeedLondon.needs
}
