package com.a8000053398.printly.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Printly's single accent — the same blue used on iOS (AccentColor.colorset):
 * light `#0065E9`, dark `#3196FF`. */
val PrintlyBlueLight = Color(0xFF0065E9)
val PrintlyBlueDark = Color(0xFF3196FF)

private val LightColors = lightColorScheme(
    primary = PrintlyBlueLight,
    onPrimary = Color.White,
    secondary = PrintlyBlueLight,
    background = Color(0xFFF2F2F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFF2F2F7),
    error = Color(0xFFD32F2F)
)

private val DarkColors = darkColorScheme(
    primary = PrintlyBlueDark,
    onPrimary = Color.Black,
    secondary = PrintlyBlueDark,
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF1C1C1E),
    error = Color(0xFFEF5350)
)

private val PrintlyTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    bodyLarge = TextStyle(fontSize = 16.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

enum class AppTheme { SYSTEM, LIGHT, DARK }

@Composable
fun PrintlyTheme(appTheme: AppTheme = AppTheme.SYSTEM, content: @Composable () -> Unit) {
    val useDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        typography = PrintlyTypography,
        content = content
    )
}

/** Spacing/radius constants — the direct port of iOS's `Metrics` enum. */
object Metrics {
    val spacingXS = 4
    val spacingS = 8
    val spacingM = 12
    val spacingL = 16
    val spacingXL = 24
    val spacingXXL = 32

    val radiusSmall = 10
    val radiusMedium = 16
    val radiusLarge = 22
}
