package ch.lauzhand.helpinghand

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform