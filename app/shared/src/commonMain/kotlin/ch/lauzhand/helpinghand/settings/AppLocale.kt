package ch.lauzhand.helpinghand.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue

/**
 * Overrides the language the shared string resources use (values / values-fr), so a language change in
 * Settings applies at once, without restarting the app. Platform-specific because each platform decides
 * the resource language differently:
 *   Android: the Configuration locale, Desktop: the JVM default Locale, web: navigator.language.
 *
 * Use: CompositionLocalProvider(LocalAppLocale provides "fr") { ... }; null restores the system language.
 */
expect object LocalAppLocale {
    val current: String @Composable get
    @Composable infix fun provides(value: String?): ProvidedValue<*>
}
