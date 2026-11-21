package ch.lauzhand.helpinghand

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import ch.lauzhand.helpinghand.data.FakeRepository
import ch.lauzhand.helpinghand.ui.AppShell
import ch.lauzhand.helpinghand.ui.HelpTheme

@Composable
@Preview
fun App() {
    // Screens only know HelpRepository; swap in RemoteRepository here later.
    val repo = remember { FakeRepository() }
    HelpTheme { AppShell(repo) }
}
