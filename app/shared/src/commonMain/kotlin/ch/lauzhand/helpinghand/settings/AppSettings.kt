package ch.lauzhand.helpinghand.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale

/** A tiny persistent key-value store: SharedPreferences, java.util.prefs or localStorage, per platform. */
interface KeyValueStore {
    fun get(key: String): String?
    fun put(key: String, value: String?)
}

@Composable
expect fun rememberKeyValueStore(): KeyValueStore

/** The two languages the app speaks. [tag] is the language tag used for the resource folders (values, values-fr). */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    FRENCH("fr");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** The system language if it is English or French, otherwise English. */
        fun fromSystem(systemTag: String): AppLanguage = fromTag(systemTag.substringBefore('-').substringBefore('_').lowercase()) ?: ENGLISH
    }
}

/**
 * The user's choices, kept in Compose state and saved as they change.
 * null means "follow the system" (language: system language if English or French, else English).
 */
class AppSettings(private val store: KeyValueStore, private val systemLanguage: AppLanguage) {
    var language: AppLanguage? by mutableStateOf(AppLanguage.fromTag(store.get(KEY_LANGUAGE)))
        private set

    /** The language in use: the user's choice, otherwise the system language (English if that is neither English nor French). */
    val effectiveLanguage: AppLanguage get() = language ?: systemLanguage

    /** null = follow the system's dark / light setting. */
    var darkMode: Boolean? by mutableStateOf(store.get(KEY_DARK)?.toBooleanStrictOrNull())
        private set

    fun chooseLanguage(value: AppLanguage) {
        language = value
        store.put(KEY_LANGUAGE, value.tag)
    }

    fun chooseDarkMode(dark: Boolean) {
        darkMode = dark
        store.put(KEY_DARK, dark.toString())
    }

    private companion object {
        const val KEY_LANGUAGE = "language"
        const val KEY_DARK = "dark_mode"
    }
}

val LocalAppSettings = staticCompositionLocalOf<AppSettings?> { null }

@Composable
fun rememberAppSettings(): AppSettings {
    val store = rememberKeyValueStore()
    // read once, before any language override is applied
    val system = remember { AppLanguage.fromSystem(Locale.current.language) }
    return remember(store) { AppSettings(store, system) }
}
