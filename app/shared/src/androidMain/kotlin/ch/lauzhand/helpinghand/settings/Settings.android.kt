package ch.lauzhand.helpinghand.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
actual fun rememberKeyValueStore(): KeyValueStore {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val prefs = context.getSharedPreferences("helping_hand_settings", Context.MODE_PRIVATE)
        object : KeyValueStore {
            override fun get(key: String): String? = prefs.getString(key, null)
            override fun put(key: String, value: String?) = prefs.edit().apply { if (value == null) remove(key) else putString(key, value) }.apply()
        }
    }
}

actual object LocalAppLocale {
    private var system: Locale? = null

    actual val current: String
        @Composable get() = Locale.getDefault().toString()

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val context = LocalContext.current
        if (system == null) system = Locale.getDefault()
        val wanted = if (value == null) system!! else Locale(value)
        Locale.setDefault(wanted)
        // a new Configuration object, so everything that reads it is recomposed
        val updated = Configuration(configuration).apply { setLocale(wanted) }
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(updated, context.resources.displayMetrics)
        return LocalConfiguration.provides(updated)
    }
}
