package ch.lauzhand.helpinghand.map

import ch.lauzhand.helpinghand.model.Zone

/**
 * Zone.x / Zone.y (0..1) are laid out on a box around Lausanne. Zones are areas, never exact spots,
 * so the centre of a zone is the most precise position the app ever derives from them.
 */
internal const val WEST = 6.595
internal const val LON_SPAN = 0.070
internal const val NORTH = 46.535
internal const val LAT_SPAN = 0.032

val Zone.centerLng: Double get() = WEST + x * LON_SPAN
val Zone.centerLat: Double get() = NORTH - y * LAT_SPAN

/** Where a position falls on the drawn map, as 0..1 fractions (the inverse of the zone mapping above). */
fun fractionX(lng: Double): Float = ((lng - WEST) / LON_SPAN).toFloat()
fun fractionY(lat: Double): Float = ((NORTH - lat) / LAT_SPAN).toFloat()
