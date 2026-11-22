package ch.lauzhand.helpinghand.map

/**
 * The base-map styles, in one place. Free OpenFreeMap styles, no API key (https://openfreemap.org).
 * LIGHT is "bright", the friendliest light one. DARK is "fiord": a blue-grey dark style (background #45516E)
 * that stays readable and goes with the navy theme; OpenFreeMap's own near-black "dark" style (background
 * rgb(12,12,12)) was too hard to read. Both were checked: they return valid style JSON (version 8).
 */
object MapStyles {
    const val LIGHT = "https://tiles.openfreemap.org/styles/bright"
    const val DARK = "https://tiles.openfreemap.org/styles/fiord"

    /** The style for the app's current theme. */
    fun forTheme(dark: Boolean): String = if (dark) DARK else LIGHT
}
