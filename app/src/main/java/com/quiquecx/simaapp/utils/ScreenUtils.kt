package com.quiquecx.simaapp.view.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Definición de umbrales:
// Compact (Móvil): Ancho < 600dp
// Medium (Tablet Pequeña): 600dp <= Ancho < 840dp
// Expanded (Tablet Grande/Escritorio): Ancho >= 840dp
enum class WindowSizeClass { Compact, Medium, Expanded }

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    return when {
        screenWidth < 600.dp -> WindowSizeClass.Compact
        screenWidth < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}