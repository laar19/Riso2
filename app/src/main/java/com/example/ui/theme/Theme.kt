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
    inversePrimary = MidnightBlueDeep,
    secondary = DarkSecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = SteelBrushedLight,
    tertiary = MidnightBlueLight,
    onTertiary = Color(0xFF060D1A),
    tertiaryContainer = MidnightBlueContainerDeep,
    onTertiaryContainer = Color(0xFFDBEAFE),
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextMuted,
    surfaceContainerLowest = DarkNeutralSurfaceLowest,
    surfaceContainerLow = DarkNeutralSurfaceLow,
    surfaceContainer = DarkNeutralSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkNeutralSurfaceHighest,
    inverseSurface = DarkNeutralText,
    inverseOnSurface = DarkNeutralBg,
    outline = DarkOutline,
    outlineVariant = DarkNeutralOutlineVariant,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFF451111),
    onErrorContainer = Color(0xFFFECDD3)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    inversePrimary = MidnightBlueLight,
    secondary = LightSecondary,
    onSecondary = Color.White,
    secondaryContainer = SteelBrushedLight,
    onSecondaryContainer = SteelBrushedDeep,
    tertiary = MidnightBluePrimary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0369A1),
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextMuted,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F9),
    surfaceContainer = Color(0xFFE2E8F0),
    surfaceContainerHigh = Color(0xFFCBD5E1),
    surfaceContainerHighest = SteelBrushedMuted,
    inverseSurface = Color(0xFF101724),
    inverseOnSurface = Color(0xFFF8FAFC),
    outline = LightOutline,
    outlineVariant = LightNeutralOutlineVariant,
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
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
