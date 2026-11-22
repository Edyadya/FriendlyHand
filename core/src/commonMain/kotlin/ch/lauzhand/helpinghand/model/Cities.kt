package ch.lauzhand.helpinghand.model

/**
 * A city the app covers. [west]/[north]/[lonSpan]/[latSpan] describe the box the drawn map (and zone x / y,
 * 0..1) is laid out on; [zoom] is the live map's overview zoom.
 */
data class City(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val zoom: Double,
    val west: Double,
    val north: Double,
    val lonSpan: Double,
    val latSpan: Double,
) {
    fun fractionX(lng: Double): Float = ((lng - west) / lonSpan).toFloat()
    fun fractionY(lat: Double): Float = ((north - lat) / latSpan).toFloat()
}

object Cities {
    val LAUSANNE = City("lausanne", "Lausanne", 46.519, 6.632, 12.6, west = 6.595, north = 46.535, lonSpan = 0.070, latSpan = 0.032)
    val LONDON = City("london", "London", 51.5090, -0.1100, 10.6, west = -0.3000, north = 51.6000, lonSpan = 0.4200, latSpan = 0.1900)
    val all = listOf(LAUSANNE, LONDON)
    fun byId(id: String?): City = all.firstOrNull { it.id == id } ?: LAUSANNE
}

/** Great-circle distance in kilometres (haversine). */
fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0
    fun rad(d: Double) = d * kotlin.math.PI / 180.0
    val dLat = rad(lat2 - lat1)
    val dLng = rad(lng2 - lng1)
    val a = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(rad(lat1)) * kotlin.math.cos(rad(lat2)) * kotlin.math.sin(dLng / 2).let { it * it }
    return 2 * r * kotlin.math.asin(kotlin.math.sqrt(a))
}
