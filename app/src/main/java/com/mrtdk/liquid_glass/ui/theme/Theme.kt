package com.mrtdk.liquid_glass.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun LiquidglassuicomponentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    themeColor: Color = ThemeManager.DefaultThemeColor,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = remember(darkTheme, pureBlack, dynamicColor, themeColor) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                if (pureBlack && darkTheme) {
                    base.copy(
                        surface = Color.Black,
                        background = Color.Black,
                        surfaceContainerLowest = Color.Black,
                        surfaceContainerLow = Color(0xFF050505),
                        surfaceContainer = Color(0xFF0F0F0F)
                    )
                } else base
            }
            darkTheme -> {
                darkColorScheme(
                    primary = themeColor,
                    secondary = themeColor.copy(alpha = 0.85f),
                    tertiary = themeColor.copy(alpha = 0.65f),
                    background = if (pureBlack) Color.Black else ThemeManager.expressiveBackgroundColor,
                    surface = if (pureBlack) Color.Black else ThemeManager.surfaceColor,
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = ThemeManager.textColor,
                    onSurface = ThemeManager.textColor,
                    onSurfaceVariant = ThemeManager.subtextColor,
                    surfaceContainerLowest = Color.Black,
                    surfaceContainerLow = if (pureBlack) Color(0xFF080808) else ThemeManager.surfaceColor,
                    surfaceContainer = if (pureBlack) Color(0xFF101010) else ThemeManager.surfaceColor,
                    surfaceContainerHigh = if (pureBlack) Color(0xFF181818) else ThemeManager.surfaceColor
                )
            }
            else -> {
                lightColorScheme(
                    primary = themeColor,
                    secondary = themeColor.copy(alpha = 0.85f),
                    tertiary = themeColor.copy(alpha = 0.65f),
                    background = ThemeManager.expressiveBackgroundColor,
                    surface = ThemeManager.surfaceColor,
                    onPrimary = Color.White,
                    onSecondary = Color.White,
                    onTertiary = Color.White,
                    onBackground = ThemeManager.textColor,
                    onSurface = ThemeManager.textColor,
                    onSurfaceVariant = ThemeManager.subtextColor,
                    surfaceContainerLow = ThemeManager.surfaceColor,
                    surfaceContainer = ThemeManager.surfaceColor,
                    surfaceContainerHigh = ThemeManager.dividerColor
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = SFPro)
            ) {
                content()
            }
        }
    )
}