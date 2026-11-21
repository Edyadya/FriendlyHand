package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable

/**
 * True when the user asked the system to reduce motion. Animations then show a static version.
 * Platform-specific because Compose has no common API for it.
 */
@Composable
expect fun rememberReduceMotion(): Boolean
