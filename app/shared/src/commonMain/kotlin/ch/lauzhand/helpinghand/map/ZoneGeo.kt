package ch.lauzhand.helpinghand.map

import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.City
import ch.lauzhand.helpinghand.model.Zone

/**
 * A zone's centre: its real coordinates when it has them (London), otherwise derived from x / y (0..1) inside
 * its city's box (Lausanne). Zones are areas, never exact spots, so this centre is the most precise position
 * the app ever derives from a zone.
 */
val Zone.city: City get() = Cities.byId(cityId)
val Zone.centerLng: Double get() = lng ?: (city.west + x * city.lonSpan)
val Zone.centerLat: Double get() = lat ?: (city.north - y * city.latSpan)

/** Where a position falls on a city's drawn map, as 0..1 fractions. */
fun fractionX(lng: Double, city: City = Cities.LAUSANNE): Float = city.fractionX(lng)
fun fractionY(lat: Double, city: City = Cities.LAUSANNE): Float = city.fractionY(lat)
