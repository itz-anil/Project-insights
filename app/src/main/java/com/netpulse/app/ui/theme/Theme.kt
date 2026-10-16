package com.netpulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.netpulse.app.R
import com.netpulse.app.data.ThemeMode

/** Every colour the UI uses, in one light and one dark set. */
@Immutable
data class NetColors(
    val isDark: Boolean,
    val background: Color,
    val orbBlue: Color,
    val orbViolet: Color,
    val orbOrange: Color,
    val orbStrength: Float,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val download: Color,
    val upload: Color,
    val good: Color,
    val warn: Color,
    val danger: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassEdgeTop: Color,
    val glassEdgeBottom: Color,
    /** Subtle fill for tracks, chips and secondary buttons. */
    val fill: Color,
    val fillStrong: Color,
    val hairline: Color,
    val switchOff: Color,
    val selected: Color,
    val onSelected: Color,
    /** Box behind the status bar icon preview. */
    val iconWell: Color,
    val iconInk: Color,
)

val DarkColors = NetColors(
    isDark = true,
    background = Color(0xFF080B17),
    orbBlue = Color(0xFF2F6BFF),
    orbViolet = Color(0xFF8E4BFF),
    orbOrange = Color(0xFFFF7A45),
    orbStrength = 1f,
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xB3FFFFFF),
    textTertiary = Color(0x8CFFFFFF),
    download = Color(0xFF6CB8FF),
    upload = Color(0xFFFFB340),
    good = Color(0xFF30D158),
    warn = Color(0xFFFFD27A),
    danger = Color(0xFFFF6B5E),
    glassTop = Color(0x1AFFFFFF),
    glassBottom = Color(0x0DFFFFFF),
    glassEdgeTop = Color(0x38FFFFFF),
    glassEdgeBottom = Color(0x0FFFFFFF),
    fill = Color(0x1FFFFFFF),
    fillStrong = Color(0x29FFFFFF),
    hairline = Color(0x1FFFFFFF),
    switchOff = Color(0x38FFFFFF),
    selected = Color(0xEBFFFFFF),
    onSelected = Color(0xFF0B0D1A),
    iconWell = Color(0x99000000),
    iconInk = Color(0xFFFFFFFF),
)

val LightColors = NetColors(
    isDark = false,
    background = Color(0xFFEEF1F8),
    orbBlue = Color(0xFF5B8CFF),
    orbViolet = Color(0xFFB98CFF),
    orbOrange = Color(0xFFFFB38A),
    orbStrength = 0.75f,
    textPrimary = Color(0xFF0B0D1A),
    textSecondary = Color(0xA60B0D1A),
    textTertiary = Color(0x800B0D1A),
    download = Color(0xFF0A62C9),
    upload = Color(0xFFB85C00),
    good = Color(0xFF1E8E3E),
    warn = Color(0xFF9A6200),
    danger = Color(0xFFC62828),
    glassTop = Color(0xBFFFFFFF),
    glassBottom = Color(0x8CFFFFFF),
    glassEdgeTop = Color(0xFFFFFFFF),
    glassEdgeBottom = Color(0x330B0D1A),
    fill = Color(0x0F0B0D1A),
    fillStrong = Color(0x1A0B0D1A),
    hairline = Color(0x1A0B0D1A),
    switchOff = Color(0x290B0D1A),
    selected = Color(0xFF0B0D1A),
    onSelected = Color(0xFFFFFFFF),
    iconWell = Color(0x140B0D1A),
    iconInk = Color(0xFF0B0D1A),
)

val LocalNetColors = staticCompositionLocalOf { DarkColors }

/** Theme-aware colour accessors: `Palette.Download` etc. resolve to the current mode. */
object Palette {
    private val c: NetColors @Composable @ReadOnlyComposable get() = LocalNetColors.current

    val Background @Composable @ReadOnlyComposable get() = c.background
    val TextPrimary @Composable @ReadOnlyComposable get() = c.textPrimary
    val TextSecondary @Composable @ReadOnlyComposable get() = c.textSecondary
    val TextTertiary @Composable @ReadOnlyComposable get() = c.textTertiary
    val Download @Composable @ReadOnlyComposable get() = c.download
    val Upload @Composable @ReadOnlyComposable get() = c.upload
    val Good @Composable @ReadOnlyComposable get() = c.good
    val Warn @Composable @ReadOnlyComposable get() = c.warn
    val Danger @Composable @ReadOnlyComposable get() = c.danger
    val Fill @Composable @ReadOnlyComposable get() = c.fill
    val FillStrong @Composable @ReadOnlyComposable get() = c.fillStrong
    val Hairline @Composable @ReadOnlyComposable get() = c.hairline
    val SwitchOff @Composable @ReadOnlyComposable get() = c.switchOff
    val Selected @Composable @ReadOnlyComposable get() = c.selected
    val OnSelected @Composable @ReadOnlyComposable get() = c.onSelected
    val IconWell @Composable @ReadOnlyComposable get() = c.iconWell
    val IconInk @Composable @ReadOnlyComposable get() = c.iconInk
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

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun NetPulseTheme(dark: Boolean, content: @Composable () -> Unit) {
    val net = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = net.download, secondary = net.upload, tertiary = net.good,
            background = net.background, surface = net.background,
            onBackground = net.textPrimary, onSurface = net.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = net.download, secondary = net.upload, tertiary = net.good,
            background = net.background, surface = net.background,
            onBackground = net.textPrimary, onSurface = net.textPrimary,
        )
    }
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Text without an explicit colour uses this; without it Compose falls back to black.
        CompositionLocalProvider(
            LocalNetColors provides net,
            LocalContentColor provides net.textPrimary,
            content = content,
        )
    }
}
