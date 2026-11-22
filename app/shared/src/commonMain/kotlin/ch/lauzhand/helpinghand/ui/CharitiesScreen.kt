package ch.lauzhand.helpinghand.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import helpinghand.app.shared.generated.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** A real charity in the Lausanne area. Data-driven: adding one is a single entry. */
private class Charity(val name: StringResource, val description: StringResource, val url: String)

// The official pages were checked in November 2026. Solid-ère's site did not load when it was checked, so its
// link goes to the home page; Cultive toi has no donation page, its home page explains how to give time or money.
private val Charities = listOf(
    Charity(Res.string.charity_caritas_name, Res.string.charity_caritas_desc, "https://caritas-regio.ch/fr/offrir-son-aide/dons/donner-de-largent"),
    Charity(Res.string.charity_solidere_name, Res.string.charity_solidere_desc, "https://solid-ere.ch/"),
    Charity(Res.string.charity_cartons_name, Res.string.charity_cartons_desc, "https://www.cartons-du-coeur.swiss/faire-un-don-aux-cartons-du-coeur"),
    Charity(Res.string.charity_cultive_name, Res.string.charity_cultive_desc, "https://cultive-toi.ch/"),
)

/** Donor tab: reputable charities nearby, each with a description and a button to its own donation page. */
@Composable
fun CharitiesScreen() {
    val uri = LocalUriHandler.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(Res.string.charities_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(Res.string.charities_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(Charities, key = { it.url }) { c ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(c.name), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(c.description), style = MaterialTheme.typography.bodyMedium)
                    AppButton(onClick = { uri.openUri(c.url) }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.charity_donate), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
