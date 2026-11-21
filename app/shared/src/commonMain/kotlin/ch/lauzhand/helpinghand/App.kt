package ch.lauzhand.helpinghand

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import ch.lauzhand.helpinghand.data.RepositoryMode
import ch.lauzhand.helpinghand.data.createRepository
import ch.lauzhand.helpinghand.ui.AppShell
import ch.lauzhand.helpinghand.ui.HelpTheme

/**
 * Where the app gets its data:
 * - FAKE: in-memory demo data on the device, works offline (safe default for the demo).
 * - REMOTE: the Ktor server (start it with `./gradlew :server:run`), shared between devices.
 */
val DATA_SOURCE = RepositoryMode.FAKE

/**
 * Server address, used only when DATA_SOURCE = REMOTE.
 * - Desktop and web (browser on the same laptop): "http://localhost:8081"
 * - Android emulator: "http://10.0.2.2:8081" (the emulator's alias for the laptop)
 * - Real phone: "http://<laptop LAN IP>:8081", e.g. "http://192.168.1.23:8081" (same Wi-Fi)
 */
const val SERVER_BASE_URL = "http://localhost:8081"

@Composable
@Preview
fun App() {
    // Screens only know HelpRepository, so switching DATA_SOURCE changes nothing else.
    val repo = remember { createRepository(DATA_SOURCE, SERVER_BASE_URL) }
    HelpTheme { AppShell(repo) }
}
