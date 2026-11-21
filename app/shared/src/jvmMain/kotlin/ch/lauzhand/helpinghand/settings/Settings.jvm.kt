package ch.lauzhand.helpinghand.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale
import java.util.prefs.Preferences

@Composable
actual fun rememberKeyValueStore(): KeyValueStore = remember {
    // Desktop: java.util.prefs (the registry on Windows, a plist on macOS, ~/.java on Linux)
    val prefs = Preferences.userRoot().node("ch/lauzhand/helpinghand")
    object : KeyValueStore {
        override fun get(key: String): String? = prefs.get(key, null)
        override fun put(key: String, value: String?) {
            if (value == null) prefs.remove(key) else prefs.put(key, value)
            prefs.flush()
        }
    }
}

actual object LocalAppLocale {
    private var system: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (system == null) system = Locale.getDefault()
        val wanted = if (value == null) system!! else Locale(value)
        Locale.setDefault(wanted) // the resource library reads the JVM default locale
        return LocalLayoutDirection.provides(LayoutDirection.Ltr)
    }
}
