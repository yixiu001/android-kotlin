package cn.yixiu.starter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF16756A), onPrimary = Color.White,
    background = Color(0xFFF6F8FA), onBackground = Color(0xFF202B32),
    surface = Color.White, onSurface = Color(0xFF202B32),
    surfaceVariant = Color(0xFFE4EBE8), onSurfaceVariant = Color(0xFF53645D),
    secondaryContainer = Color(0xFFE4F4ED), onSecondaryContainer = Color(0xFF145F56),
    outline = Color(0xFF74847D), outlineVariant = Color(0xFFDDE5E1)
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFF86D5C6), onPrimary = Color(0xFF00382F),
    background = Color(0xFF111916), onBackground = Color(0xFFE1E9E4),
    surface = Color(0xFF1B2521), onSurface = Color(0xFFE1E9E4),
    surfaceVariant = Color(0xFF35453E), onSurfaceVariant = Color(0xFFB9CBC1),
    secondaryContainer = Color(0xFF244E43), onSecondaryContainer = Color(0xFFC2EFDD),
    outline = Color(0xFF899D92), outlineVariant = Color(0xFF3A4B42)
)

@Composable fun StarterTheme(themeMode: String = "system", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
