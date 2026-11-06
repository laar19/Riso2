package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    inversePrimary = CoralPrimary,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF302013),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = LightBrownContainerLight,
    tertiary = PastelPeachTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF522A1C),
    onTertiaryContainer = Color(0xFFFAF0EA),
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = DarkWarmSurfaceLowest,
    surfaceContainerLow = DarkWarmSurfaceLow,
    surfaceContainer = DarkWarmSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkWarmSurfaceHighest,
    inverseSurface = DarkWarmText,
    inverseOnSurface = DarkWarmBackground,
    outline = DarkOutline,
    outlineVariant = DarkWarmOutlineVariant,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFF5C1D1D),
    onErrorContainer = Color(0xFFFBDADA)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = DarkCoralPrimary,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightBrownContainerLight,
    onSecondaryContainer = LightBrownOnContainer,
    tertiary = PastelPeachTertiary,
    onTertiary = Color.White,
    tertiaryContainer = PastelPeachContainerLight,
    onTertiaryContainer = PastelPeachOnContainer,
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    surfaceContainerLowest = CreamLightSurfaceLowest,
    surfaceContainerLow = CreamLightSurfaceLow,
    surfaceContainer = CreamLightSurfaceContainer,
    surfaceContainerHigh = CreamLightSurfaceHigh,
    surfaceContainerHighest = CreamLightSurfaceHighest,
    inverseSurface = Color(0xFF2C221C),
    inverseOnSurface = Color(0xFFFAF7F2),
    outline = LightOutline,
    outlineVariant = CreamLightOutlineVariant,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFFFBE7E7),
    onErrorContainer = Color(0xFF8B1E1E)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Disable dynamic color so our custom Sophisticated Dark assets take priority
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
