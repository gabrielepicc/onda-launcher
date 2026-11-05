// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.theme

import com.gabrielepiccione.onda.util.LocalOndaHaptics
import com.gabrielepiccione.onda.util.rememberOndaHaptics
import com.gabrielepiccione.onda.ui.components.*
import com.gabrielepiccione.onda.ui.home.*
import com.gabrielepiccione.onda.ui.drawer.*
import com.gabrielepiccione.onda.ui.search.*
import com.gabrielepiccione.onda.ui.settings.*
import com.gabrielepiccione.onda.ui.sheets.*

import android.os.Build
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import com.gabrielepiccione.onda.data.FontChoice
import com.gabrielepiccione.onda.data.Settings
import com.gabrielepiccione.onda.data.ThemeMode

/** Colori derivati dallo schema Material, comodi per i testi sopra lo sfondo. */
data class OndaColors(
    val dark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val onWallpaper: Color,
    val onWallpaperMuted: Color,
    val scrim: Color,
    val panel: Color,
    val panelHigh: Color,
    val panelBorder: Color,
)

val LocalOndaColors = staticCompositionLocalOf { colorsFrom(darkColorScheme(), true) }
val LocalHapticsEnabled = staticCompositionLocalOf { true }

/** Ombra da applicare ai testi sopra lo sfondo (null = nessuna). */
val LocalTextShadow = staticCompositionLocalOf<androidx.compose.ui.graphics.Shadow?> { null }

/** Font importati dall'utente per la lista e per l'orologio (null = carattere del tema). */
val LocalListFont = staticCompositionLocalOf<FontFamily?> { null }
val LocalClockFont = staticCompositionLocalOf<FontFamily?> { null }

/** Font da un file importato; null se il file non esiste o non è leggibile. */
@androidx.compose.runtime.Composable
fun rememberFileFont(path: String): FontFamily? = remember(path) {
    if (path.isBlank()) null
    else runCatching {
        val f = java.io.File(path)
        if (!f.exists()) null else FontFamily(androidx.compose.ui.text.font.Font(f))
    }.getOrNull()
}

/** Aspetto delle righe dei preferiti (Fase 2). */
@androidx.compose.runtime.Immutable
data class ListStyle(
    val align: com.gabrielepiccione.onda.data.HAlign = com.gabrielepiccione.onda.data.HAlign.START,
    val weight: Int = 400,
    val letterSpacingEm: Float = 0f,
)
val LocalListStyle = staticCompositionLocalOf { ListStyle() }

/** Stile delle icone corrente (ORIGINAL, DOTS, MATERIAL, OUTLINE, STICKER, WATERCOLOR, MARBLE). */
val LocalIconStyle = staticCompositionLocalOf { "ORIGINAL" }
val LocalNotificationImages = staticCompositionLocalOf { com.gabrielepiccione.onda.data.NotificationImages.THUMBNAIL }

val AccentChoices = listOf(
    0xFF7FE3D1, // acqua
    0xFF8AB4F8, // cielo
    0xFFC3A6FF, // lavanda
    0xFFFFAB91, // corallo
    0xFFFFD27F, // sabbia
    0xFFA5D6A7, // salvia
)

/** Easing "emphasized" di Material 3. */
object Motion {
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    fun <T> enter(duration: Int = 400): FiniteAnimationSpec<T> = tween(duration, easing = EmphasizedDecelerate)
    fun <T> exit(duration: Int = 200): FiniteAnimationSpec<T> = tween(duration, easing = EmphasizedAccelerate)
    fun <T> bouncy(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.75f, stiffness = 380f)
    val offsetEnter: FiniteAnimationSpec<IntOffset> = tween(400, easing = EmphasizedDecelerate)
    val offsetExit: FiniteAnimationSpec<IntOffset> = tween(200, easing = EmphasizedAccelerate)
}

private fun colorsFrom(scheme: ColorScheme, dark: Boolean) = OndaColors(
    dark = dark,
    accent = scheme.primary,
    onAccent = scheme.onPrimary,
    accentContainer = scheme.primaryContainer,
    onAccentContainer = scheme.onPrimaryContainer,
    onWallpaper = scheme.onSurface,
    onWallpaperMuted = scheme.onSurfaceVariant,
    scrim = scheme.surface,
    panel = scheme.surfaceContainer,
    panelHigh = scheme.surfaceContainerHigh,
    panelBorder = scheme.outlineVariant.copy(alpha = 0.5f),
)

@Composable
fun OndaTheme(settings: Settings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        // Sotto Android 12: se i colori dinamici sono attivi, accento ricavato dallo sfondo
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 ->
            staticScheme(rememberWallpaperAccent() ?: Color(settings.accent), dark)
        else -> staticScheme(Color(settings.accent), dark)
    }
    val family = fontFamilyOf(settings.font)
    val typography = remember(family) { typographyWith(family) }
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalOndaColors provides colorsFrom(scheme, dark).let { c ->
            if (settings.textColor == 0L) c
            else Color(settings.textColor).let { t -> c.copy(onWallpaper = t, onWallpaperMuted = t.copy(alpha = 0.7f)) }
        },
        LocalTextShadow provides if (settings.textShadow) {
            androidx.compose.ui.graphics.Shadow(Color.Black.copy(alpha = if (dark) 0.55f else 0.35f), androidx.compose.ui.geometry.Offset(0f, 2f), 8f)
        } else null,
        LocalListFont provides rememberFileFont(settings.customFontList),
        LocalListStyle provides ListStyle(settings.listAlign, settings.listFontWeight, settings.listLetterSpacing),
        LocalClockFont provides rememberFileFont(settings.customFontClock),
        LocalHapticsEnabled provides settings.haptics,
        LocalOndaHaptics provides rememberOndaHaptics(settings.haptics),
        LocalIconStyle provides settings.iconStyle,
        LocalNotificationImages provides settings.notificationImages,
        LocalOndaTokens provides OndaTokens(),
        LocalDensity provides Density(base.density, base.fontScale * settings.textScale),
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

/** Schema statico costruito a partire dal colore d'accento (per Android < 12 o colori dinamici spenti). */
private fun staticScheme(accent: Color, dark: Boolean): ColorScheme {
    fun mix(a: Color, b: Color, t: Float) = Color(
        a.red + (b.red - a.red) * t, a.green + (b.green - a.green) * t, a.blue + (b.blue - a.blue) * t, 1f
    )
    return if (dark) {
        val surface = mix(Color(0xFF101315), accent, 0.04f)
        darkColorScheme(
            primary = accent,
            onPrimary = if (accent.luminance() > 0.4f) Color(0xFF0B1A17) else Color.White,
            primaryContainer = mix(Color(0xFF101315), accent, 0.35f),
            onPrimaryContainer = mix(Color.White, accent, 0.2f),
            secondary = accent,
            surface = surface,
            background = surface,
            surfaceContainer = mix(surface, accent, 0.06f),
            surfaceContainerHigh = mix(surface, accent, 0.1f),
            onSurface = Color(0xFFE3E6E8),
            onSurfaceVariant = Color(0xFFB9C0C4),
        )
    } else {
        val primary = mix(accent, Color.Black, 0.45f)
        val surface = mix(Color(0xFFF8FAFB), accent, 0.04f)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = mix(Color.White, accent, 0.45f),
            onPrimaryContainer = mix(accent, Color.Black, 0.7f),
            secondary = primary,
            surface = surface,
            background = surface,
            surfaceContainer = mix(surface, accent, 0.08f),
            surfaceContainerHigh = mix(surface, accent, 0.13f),
            onSurface = Color(0xFF171C1F),
            onSurfaceVariant = Color(0xFF42494D),
        )
    }
}

/** Colore principale dello sfondo (Android 8.1+), aggiornato quando lo sfondo cambia. */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.O_MR1)
@Composable
fun rememberWallpaperAccent(): Color? {
    val context = LocalContext.current
    val wm = remember { android.app.WallpaperManager.getInstance(context) }
    var color by remember {
        androidx.compose.runtime.mutableStateOf(
            runCatching { wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)?.primaryColor?.toArgb() }.getOrNull()
        )
    }
    androidx.compose.runtime.DisposableEffect(wm) {
        val listener = android.app.WallpaperManager.OnColorsChangedListener { colors, which ->
            if (which and android.app.WallpaperManager.FLAG_SYSTEM != 0) color = colors?.primaryColor?.toArgb()
        }
        runCatching { wm.addOnColorsChangedListener(listener, android.os.Handler(android.os.Looper.getMainLooper())) }
        onDispose { runCatching { wm.removeOnColorsChangedListener(listener) } }
    }
    return color?.let { vivid(Color(it)) }
}

/** Rende l'accento abbastanza saturo e luminoso da essere usabile su sfondo scuro o chiaro. */
private fun vivid(c: Color): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(c.toArgb(), hsv)
    hsv[1] = hsv[1].coerceIn(0.35f, 0.75f)
    hsv[2] = hsv[2].coerceIn(0.7f, 0.95f)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

fun fontFamilyOf(font: FontChoice): FontFamily = when (font) {
    FontChoice.SYSTEM -> FontFamily.Default
    FontChoice.SANS -> FontFamily.SansSerif
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
    FontChoice.ROUNDED -> FontFamily.Cursive
}

fun typographyWith(family: FontFamily): Typography {
    val t = Typography()
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge = t.displayLarge.f(), displayMedium = t.displayMedium.f(), displaySmall = t.displaySmall.f(),
        headlineLarge = t.headlineLarge.f(), headlineMedium = t.headlineMedium.f(), headlineSmall = t.headlineSmall.f(),
        titleLarge = t.titleLarge.f(), titleMedium = t.titleMedium.f(), titleSmall = t.titleSmall.f(),
        bodyLarge = t.bodyLarge.f(), bodyMedium = t.bodyMedium.f(), bodySmall = t.bodySmall.f(),
        labelLarge = t.labelLarge.f(), labelMedium = t.labelMedium.f(), labelSmall = t.labelSmall.f(),
    )
}

/** Impedisce che i tocchi su un pannello sovrapposto arrivino alla home sottostante. */
fun Modifier.blockTouches(): Modifier = this.then(Modifier.pointerInput(Unit) { detectTapGestures() })
