package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ch.lauzhand.helpinghand.model.Cities
import ch.lauzhand.helpinghand.model.City
import ch.lauzhand.helpinghand.model.distanceKm
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Only charities within this distance of the chosen city are listed. */
const val CHARITY_RADIUS_KM = 100

/** A real charity. [lat]/[lng] is where it works (its city), used for the radius. Data-driven: one entry each. */
private class Charity(val name: StringResource, val description: StringResource, val url: String, val lat: Double, val lng: Double)

private const val LAUSANNE_LAT = 46.519
private const val LAUSANNE_LNG = 6.632
private const val LONDON_LAT = 51.509
private const val LONDON_LNG = -0.110

// The official pages were checked in November 2026. Not confirmed: Solid-ère's site did not load when it was
// checked (its link goes to the home page). Cultive toi has no donation page, its home
// page explains how to give time or money.
private val Charities = listOf(
    // Lausanne and the canton of Vaud
    Charity(Res.string.charity_solidere_name, Res.string.charity_solidere_desc, "https://solid-ere.ch/", LAUSANNE_LAT, LAUSANNE_LNG),
    Charity(Res.string.charity_cartons_name, Res.string.charity_cartons_desc, "https://www.cartons-du-coeur.swiss/faire-un-don-aux-cartons-du-coeur", LAUSANNE_LAT, LAUSANNE_LNG),
    Charity(Res.string.charity_cultive_name, Res.string.charity_cultive_desc, "https://cultive-toi.ch/", LAUSANNE_LAT, LAUSANNE_LNG),
    // London: registered, non-party-political non-profits (homelessness, youth, education, inclusion).
    // Left out on purpose: religious groups, businesses, alumni groups, anything for personal benefit.
    Charity(Res.string.charity_stmungos_name, Res.string.charity_stmungos_desc, "https://www.mungos.org/donate/", LONDON_LAT, LONDON_LNG),
    Charity(Res.string.charity_centrepoint_name, Res.string.charity_centrepoint_desc, "https://www.centrepoint.org.uk/donate", LONDON_LAT, LONDON_LNG),
    Charity(Res.string.charity_londonyouth_name, Res.string.charity_londonyouth_desc, "https://www.londonyouth.org/donate/", LONDON_LAT, LONDON_LNG),
    Charity(Res.string.charity_kingstrust_name, Res.string.charity_kingstrust_desc, "https://www.princes-trust.org.uk/donate", LONDON_LAT, LONDON_LNG),
    // Magic Breakfast's site blocks automated checks, so its home page was not opened here
    Charity(Res.string.charity_magicbreakfast_name, Res.string.charity_magicbreakfast_desc, "https://www.magicbreakfast.com/", LONDON_LAT, LONDON_LNG),
    Charity(Res.string.charity_felix_name, Res.string.charity_felix_desc, "https://felix.org/donate", LONDON_LAT, LONDON_LNG),
)

/**
 * Donor tab: reputable charities within [CHARITY_RADIUS_KM] km of the chosen [city], nearest first, each with a
 * description and a button to its own donation page.
 */
@Composable
fun CharitiesScreen(city: City, onCityChange: (City) -> Unit = {}, onBack: (() -> Unit)? = null) {
    val uri = LocalUriHandler.current
    val nearby = Charities
        .map { it to distanceKm(city.lat, city.lng, it.lat, it.lng) }
        .filter { it.second <= CHARITY_RADIUS_KM }
        .sortedBy { it.second }
        .map { it.first }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onBack != null) TextButton(onClick = onBack) { Text(stringResource(Res.string.back)) }
                Text(stringResource(Res.string.charities_title), style = MaterialTheme.typography.headlineSmall)
                // pick the city here too, so London's charities are one tap away
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Cities.all.forEach { c ->
                        FilterChip(selected = c.id == city.id, onClick = { onCityChange(c) }, label = { Text(c.name) })
                    }
                }
                Text(
                    stringResource(Res.string.charities_radius, CHARITY_RADIUS_KM, city.name),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    stringResource(Res.string.charities_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.charities_link_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(Res.string.charities_criteria),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (nearby.isEmpty()) {
            item { Text(stringResource(Res.string.charities_none, CHARITY_RADIUS_KM, city.name), style = MaterialTheme.typography.bodyLarge) }
        }
        items(nearby, key = { it.url }) { c ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(MaterialTheme.borderWidth, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // the name is the link to the charity's own page
                    Text(
                        stringResource(c.name),
                        Modifier.heightIn(min = 48.dp).clickable(role = Role.Button) { uri.openUri(c.url) }.wrapContentHeight(Alignment.CenterVertically),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(stringResource(c.description), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
