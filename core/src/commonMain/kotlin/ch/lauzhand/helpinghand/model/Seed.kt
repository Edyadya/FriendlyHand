package ch.lauzhand.helpinghand.model

// Demo data shared by FakeRepository (app) and the server's in-memory store.
object Seed {
    const val ME_DONOR = "donor-me"
    const val ME_VOLUNTEER = "vol-me"

    val zones: List<Zone> = listOf(
        Zone("sallaz", "Sallaz", 0.72f, 0.18f),
        Zone("riponne", "Riponne", 0.52f, 0.32f),
        Zone("chauderon", "Chauderon", 0.32f, 0.40f),
        Zone("flon", "Flon", 0.48f, 0.48f),
        Zone("malley", "Malley", 0.12f, 0.50f),
        Zone("gare", "Gare", 0.52f, 0.62f),
        Zone("ouchy", "Ouchy", 0.50f, 0.76f),
    )

    val people: List<Person> = listOf(
        Person("p-jerry", "Jerry", 53, "flon", "mornings",
            "I used to be a cook. I'd like to work in a kitchen again.", true, ME_VOLUNTEER,
            // last seen about 1 hour ago near the Flon, Lausanne
            lastSeenLat = 46.5211, lastSeenLng = 6.6268, lastSeenNote = "By the Flon bridge",
            lastSeenAtMillis = nowMillis() - 3_600_000L),
        Person("p-amina", "Amina", 34, "gare", "evenings",
            "I need my papers sorted to get a job.", true, ME_VOLUNTEER),
        Person("p-luc", "Luc", 61, "riponne", "afternoons", null, true, ME_VOLUNTEER),
    )

    val needs: List<Need> = listOf(
        Need("n-1", "p-jerry", NeedType.SMALL, "Winter coat", "Size L"),
        Need("n-2", "p-jerry", NeedType.BIG, "Health insurance arrears",
            targetAmountChf = 800, fundedAmountChf = 320, status = NeedStatus.FUNDING),
        Need("n-3", "p-amina", NeedType.SMALL, "Phone charger", "USB-C"),
        Need("n-4", "p-amina", NeedType.BIG, "ID papers renewal fee", targetAmountChf = 150),
        Need("n-5", "p-luc", NeedType.SMALL, "Warm socks"),
    )
}
