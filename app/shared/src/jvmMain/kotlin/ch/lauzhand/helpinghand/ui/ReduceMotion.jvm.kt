package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable

// Desktop: the JVM exposes no reduce-motion setting, so animations always run.
@Composable
actual fun rememberReduceMotion(): Boolean = false
