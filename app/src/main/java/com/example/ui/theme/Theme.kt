package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = EmeraldOnPrimaryDark,
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = MintLightInk,
    secondary = AmberSecondaryDark,
    onSecondary = AmberOnSecondaryDark,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberContainer,
    tertiary = TerracottaTertiaryDark,
    onTertiary = TerracottaOnTertiaryDark,
    tertiaryContainer = TerracottaContainerDark,
    onTertiaryContainer = TerracottaContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedInk,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = WarmSurface,
    primaryContainer = MintContainer,
    onPrimaryContainer = MintDarkInk,
    secondary = AmberSecondary,
    onSecondary = WarmSurface,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberDarkInk,
    tertiary = TerracottaTertiary,
    onTertiary = WarmSurface,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = TerracottaDarkInk,
    background = WarmCreamBackground,
    onBackground = WarmInk,
    surface = WarmSurface,
    onSurface = WarmInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmMutedInk,
    outline = WarmOutline,
    outlineVariant = WarmOutlineVariant,
  )

val AppShapes =
  Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
  )

@Composable
fun AhorrosTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
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

  MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content,
  )
}
