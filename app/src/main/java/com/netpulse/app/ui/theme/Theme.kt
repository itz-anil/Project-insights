package com.netpulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.netpulse.app.R

object Palette {
    val Background = Color(0xFF080B17)
    val OrbBlue = Color(0xFF2F6BFF)
    val OrbViolet = Color(0xFF8E4BFF)
    val OrbOrange = Color(0xFFFF7A45)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xB3FFFFFF)
    val TextTertiary = Color(0x80FFFFFF)

    val Download = Color(0xFF6CB8FF)
    val Upload = Color(0xFFFFB340)
    val Good = Color(0xFF30D158)
    val Warn = Color(0xFFFFD27A)

    val GlassTop = Color(0x1AFFFFFF)
    val GlassBottom = Color(0x0DFFFFFF)
    val GlassEdgeTop = Color(0x38FFFFFF)
    val GlassEdgeBottom = Color(0x0FFFFFFF)
    val Selected = Color(0xEBFFFFFF)
    val OnSelected = Color(0xFF0B0D1A)
}

@OptIn(ExperimentalTextApi::class)
private fun jakarta(weight: Int) = Font(
    R.font.plus_jakarta_sans,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

@OptIn(ExperimentalTextApi::class)
private fun mono(weight: Int) = Font(
    R.font.jetbrains_mono,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** UI text: Plus Jakarta Sans — clean, rounded, very readable on a tablet. */
val Jakarta = FontFamily(jakarta(400), jakarta(500), jakarta(600), jakarta(700), jakarta(800))

/** Numbers: JetBrains Mono, so digits don't jump around as the speed changes. */
val Mono = FontFamily(mono(500), mono(700), mono(800))

private val typography = Typography(
    displayLarge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 56.sp, letterSpacing = (-1).sp),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Mono, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 19.sp),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.8.sp),
)

private val colors = darkColorScheme(
    primary = Palette.Download,
    secondary = Palette.Upload,
    tertiary = Palette.Good,
    background = Palette.Background,
    surface = Palette.Background,
    onBackground = Palette.TextPrimary,
    onSurface = Palette.TextPrimary,
)

@Composable
fun NetPulseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
