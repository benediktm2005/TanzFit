package de.tanzfit.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Pink = Color(0xFFFF4F8B)
val Cyan = Color(0xFF3DDCFF)
val Green = Color(0xFF4CD787)

private val colors = darkColorScheme(
    primary = Pink,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF00222C),
    tertiary = Green,
    background = Color(0xFF14121A),
    onBackground = Color(0xFFF1EEF6),
    surface = Color(0xFF14121A),
    onSurface = Color(0xFFF1EEF6),
    surfaceVariant = Color(0xFF26222F),
    onSurfaceVariant = Color(0xFFC9C3D3),
    surfaceContainer = Color(0xFF1E1A26),
    surfaceContainerHigh = Color(0xFF26222F),
    surfaceContainerHighest = Color(0xFF2F2A39),
    primaryContainer = Color(0xFF5C1834),
    onPrimaryContainer = Color(0xFFFFD9E3),
)

@Composable
fun TanzFitTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
