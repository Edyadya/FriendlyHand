package ch.lauzhand.helpinghand.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Web: the browser's prefers-reduced-motion media query.
private fun prefersReducedMotion(): Boolean = js("window.matchMedia('(prefers-reduced-motion: reduce)').matches")

@Composable
actual fun rememberReduceMotion(): Boolean = remember { prefersReducedMotion() }
