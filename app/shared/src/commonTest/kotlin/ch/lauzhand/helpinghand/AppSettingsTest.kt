package ch.lauzhand.helpinghand

import ch.lauzhand.helpinghand.settings.AppLanguage
import ch.lauzhand.helpinghand.settings.AppSettings
import ch.lauzhand.helpinghand.settings.KeyValueStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class MemoryStore : KeyValueStore {
    val values = mutableMapOf<String, String>()
    override fun get(key: String) = values[key]
    override fun put(key: String, value: String?) { if (value == null) values.remove(key) else values[key] = value }
}

class AppSettingsTest {
    @Test
    fun systemLanguageIsUsedIfEnglishOrFrenchOtherwiseEnglish() {
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromSystem("fr"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromSystem("fr-CH"))
        assertEquals(AppLanguage.FRENCH, AppLanguage.fromSystem("fr_FR"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystem("en-GB"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromSystem("de-CH")) // not English or French: English
    }

    @Test
    fun withNoChoiceTheSystemDecides() {
        val settings = AppSettings(MemoryStore(), systemLanguage = AppLanguage.FRENCH)
        assertNull(settings.language)
        assertNull(settings.darkMode)
        assertEquals(AppLanguage.FRENCH, settings.effectiveLanguage)
    }

    @Test
    fun choicesSurviveARestart() {
        val store = MemoryStore()
        val first = AppSettings(store, AppLanguage.ENGLISH)
        first.chooseLanguage(AppLanguage.FRENCH)
        first.chooseDarkMode(true)

        // a new AppSettings on the same store stands for the next run of the app
        val second = AppSettings(store, AppLanguage.ENGLISH)
        assertEquals(AppLanguage.FRENCH, second.effectiveLanguage)
        assertEquals(true, second.darkMode)
    }

    @Test
    fun anUnknownSavedLanguageFallsBackToTheSystem() {
        val store = MemoryStore().apply { put("language", "xx") }
        assertEquals(AppLanguage.ENGLISH, AppSettings(store, AppLanguage.ENGLISH).effectiveLanguage)
    }
}
