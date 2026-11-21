package ch.lauzhand.helpinghand.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.browser.localStorage
import kotlinx.browser.window

@Composable
actual fun rememberKeyValueStore(): KeyValueStore = remember {
    // Web: the browser's localStorage
    object : KeyValueStore {
        override fun get(key: String): String? = localStorage.getItem("helpinghand.$key")
        override fun put(key: String, value: String?) {
            if (value == null) localStorage.removeItem("helpinghand.$key") else localStorage.setItem("helpinghand.$key", value)
        }
    }
}

private fun overrideNavigatorLanguage(tag: String) {
    js("Object.defineProperty(navigator, 'language', { get: function () { return tag; }, configurable: true })")
}

actual object LocalAppLocale {
    private var system: String? = null

    actual val current: String
        @Composable get() = window.navigator.language

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        if (system == null) system = window.navigator.language
        // the resource library reads navigator.language
        overrideNavigatorLanguage(value ?: system!!)
        return LocalLayoutDirection.provides(LayoutDirection.Ltr)
    }
}
