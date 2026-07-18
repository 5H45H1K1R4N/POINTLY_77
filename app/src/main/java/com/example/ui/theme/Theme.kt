package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF1E1E24),
    tertiary = Color(0xFFFF8A80),
    background = Color(0xFF121115),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1D1B22),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF26242B),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFFFFFFFF),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF000000),
    onPrimary = Color.White,
    secondary = Color(0xFF1E1E24),
    onSecondary = Color.White,
    tertiary = Color(0xFFFF5252),
    background = Color(0xFFFAF8F5), // Bento notebook soft cream canvas
    onBackground = Color(0xFF000000),
    surface = Color.White,
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFF3EFE9),
    onSurfaceVariant = Color(0xFF2E2D2B),
    outline = Color(0xFF000000),
    error = Color(0xFFB3261E),
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
