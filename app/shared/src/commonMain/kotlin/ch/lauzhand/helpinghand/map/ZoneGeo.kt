package ch.lauzhand.helpinghand.map

import ch.lauzhand.helpinghand.model.Zone

/**
 * Zone.x / Zone.y (0..1) are laid out on a box around Lausanne. Zones are areas, never exact spots,
 * so the centre of a zone is the most precise position the app ever derives from them.
 */
private const val WEST = 6.595
private const val LON_SPAN = 0.070
private const val NORTH = 46.535
private const val LAT_SPAN = 0.032

val Zone.centerLng: Double get() = WEST + x * LON_SPAN
val Zone.centerLat: Double get() = NORTH - y * LAT_SPAN
