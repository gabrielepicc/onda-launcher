// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Gabriele Piccione
package com.gabrielepiccione.onda.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FlightTakeoff
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabrielepiccione.onda.data.AppCategory
import com.gabrielepiccione.onda.data.AppInfo
import com.gabrielepiccione.onda.ui.theme.LocalOndaColors
import kotlin.math.sin
import kotlin.random.Random

/** Stili di icona generata disponibili. */
val GENERATED_ICON_STYLES = setOf("MATERIAL", "OUTLINE", "STICKER", "WATERCOLOR", "MARBLE")

/** Glifo della categoria: arrotondato (pieno) o a contorno. */
@Suppress("DEPRECATION")
fun categoryGlyph(category: AppCategory, outlined: Boolean): ImageVector? = if (outlined) {
    when (category) {
        AppCategory.PHONE -> Icons.Outlined.Phone
        AppCategory.MESSAGES -> Icons.Outlined.Sms
        AppCategory.CHAT -> Icons.Outlined.Chat
        AppCategory.EMAIL -> Icons.Outlined.Email
        AppCategory.CAMERA -> Icons.Outlined.PhotoCamera
        AppCategory.GALLERY -> Icons.Outlined.Photo
        AppCategory.MUSIC -> Icons.Outlined.MusicNote
        AppCategory.VIDEO -> Icons.Outlined.PlayCircle
        AppCategory.MAPS -> Icons.Outlined.Map
        AppCategory.BROWSER -> Icons.Outlined.Language
        AppCategory.STORE -> Icons.Outlined.Storefront
        AppCategory.SHOPPING -> Icons.Outlined.ShoppingBag
        AppCategory.BANK -> Icons.Outlined.AccountBalance
        AppCategory.PAYMENT -> Icons.Outlined.Payments
        AppCategory.CALENDAR -> Icons.Outlined.Event
        AppCategory.CLOCK -> Icons.Outlined.Schedule
        AppCategory.CALCULATOR -> Icons.Outlined.Calculate
        AppCategory.NOTES -> Icons.Outlined.Description
        AppCategory.FILES -> Icons.Outlined.Folder
        AppCategory.SETTINGS -> Icons.Outlined.Settings
        AppCategory.WEATHER -> Icons.Outlined.WbSunny
        AppCategory.NEWS -> Icons.Outlined.Newspaper
        AppCategory.BOOKS -> Icons.Outlined.MenuBook
        AppCategory.GAMES -> Icons.Outlined.SportsEsports
        AppCategory.SOCIAL -> Icons.Outlined.Groups
        AppCategory.HEALTH -> Icons.Outlined.Favorite
        AppCategory.FOOD -> Icons.Outlined.Restaurant
        AppCategory.TRAVEL -> Icons.Outlined.FlightTakeoff
        AppCategory.TRANSPORT -> Icons.Outlined.DirectionsCar
        AppCategory.EDUCATION -> Icons.Outlined.School
        AppCategory.WORK -> Icons.Outlined.Work
        AppCategory.CLOUD -> Icons.Outlined.Cloud
        AppCategory.SECURITY -> Icons.Outlined.Security
        AppCategory.TOOLS -> Icons.Outlined.Build
        AppCategory.TRANSLATE -> Icons.Outlined.Translate
        AppCategory.CONTACTS -> Icons.Outlined.Contacts
        AppCategory.RECORDER -> Icons.Outlined.Mic
        AppCategory.OTHER -> null
    }
} else {
    when (category) {
        AppCategory.PHONE -> Icons.Rounded.Phone
        AppCategory.MESSAGES -> Icons.Rounded.Sms
        AppCategory.CHAT -> Icons.Rounded.Chat
        AppCategory.EMAIL -> Icons.Rounded.Email
        AppCategory.CAMERA -> Icons.Rounded.PhotoCamera
        AppCategory.GALLERY -> Icons.Rounded.Photo
        AppCategory.MUSIC -> Icons.Rounded.MusicNote
        AppCategory.VIDEO -> Icons.Rounded.PlayCircle
        AppCategory.MAPS -> Icons.Rounded.Map
        AppCategory.BROWSER -> Icons.Rounded.Language
        AppCategory.STORE -> Icons.Rounded.Storefront
        AppCategory.SHOPPING -> Icons.Rounded.ShoppingBag
        AppCategory.BANK -> Icons.Rounded.AccountBalance
        AppCategory.PAYMENT -> Icons.Rounded.Payments
        AppCategory.CALENDAR -> Icons.Rounded.Event
        AppCategory.CLOCK -> Icons.Rounded.Schedule
        AppCategory.CALCULATOR -> Icons.Rounded.Calculate
        AppCategory.NOTES -> Icons.Rounded.Description
        AppCategory.FILES -> Icons.Rounded.Folder
        AppCategory.SETTINGS -> Icons.Rounded.Settings
        AppCategory.WEATHER -> Icons.Rounded.WbSunny
        AppCategory.NEWS -> Icons.Rounded.Newspaper
        AppCategory.BOOKS -> Icons.Rounded.MenuBook
        AppCategory.GAMES -> Icons.Rounded.SportsEsports
        AppCategory.SOCIAL -> Icons.Rounded.Groups
        AppCategory.HEALTH -> Icons.Rounded.Favorite
        AppCategory.FOOD -> Icons.Rounded.Restaurant
        AppCategory.TRAVEL -> Icons.Rounded.FlightTakeoff
        AppCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
        AppCategory.EDUCATION -> Icons.Rounded.School
        AppCategory.WORK -> Icons.Rounded.Work
        AppCategory.CLOUD -> Icons.Rounded.Cloud
        AppCategory.SECURITY -> Icons.Rounded.Security
        AppCategory.TOOLS -> Icons.Rounded.Build
        AppCategory.TRANSLATE -> Icons.Rounded.Translate
        AppCategory.CONTACTS -> Icons.Rounded.Contacts
        AppCategory.RECORDER -> Icons.Rounded.Mic
        AppCategory.OTHER -> null
    }
}

/** Glifo o, per le app senza categoria, l'iniziale del nome. */
@Composable
private fun Glyph(app: AppInfo, size: Dp, tint: Color, outlined: Boolean) {
    val vector = categoryGlyph(app.category, outlined)
    if (vector != null) {
        Icon(vector, null, tint = tint, modifier = Modifier.size(size * 0.52f))
    } else {
        Text(
            app.label.trim().take(1).uppercase(),
            color = tint,
            fontSize = (size.value * 0.42f).sp,
            fontWeight = if (outlined) FontWeight.Light else FontWeight.SemiBold,
        )
    }
}

/** Icona generata nello stile richiesto (vedi [GENERATED_ICON_STYLES]). */
@Composable
fun GeneratedIcon(app: AppInfo, size: Dp, style: String, modifier: Modifier = Modifier) {
    val colors = LocalOndaColors.current
    val appColor = Color(app.color)
    when (style) {
        // Material You: glifo monocromo nei colori del tema
        "MATERIAL" -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).clip(CircleShape).background(colors.accentContainer),
        ) { Glyph(app, size, colors.onAccentContainer, outlined = false) }

        // Solo linee: anello sottile e glifo a contorno
        "OUTLINE" -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).border(1.5.dp, colors.onWallpaper.copy(alpha = 0.8f), CircleShape),
        ) { Glyph(app, size, colors.onWallpaper, outlined = true) }

        // Sticker: bordo bianco, ombra, sfondo sfumato nel colore dell'app
        "STICKER" -> Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .shadow(size * 0.08f, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .padding(size * 0.07f)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(lighten(appColor, 0.25f), darken(appColor, 0.15f)))),
        ) { Glyph(app, size, if (appColor.luminance() > 0.6f) Color(0xFF1A1A1A) else Color.White, outlined = false) }

        // Acquerello: macchie morbide del colore dell'app e dell'accento su carta
        "WATERCOLOR" -> {
            val seed = remember(app.key) { app.key.hashCode() }
            Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).clip(CircleShape)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color(0xFFFBF8F1))
                    val rnd = Random(seed)
                    val palette = listOf(appColor, colors.accent, lighten(appColor, 0.4f))
                    repeat(5) { i ->
                        val c = palette[i % palette.size]
                        val center = Offset(this.size.width * (0.2f + 0.6f * rnd.nextFloat()), this.size.height * (0.2f + 0.6f * rnd.nextFloat()))
                        val r = this.size.minDimension * (0.35f + 0.3f * rnd.nextFloat())
                        drawCircle(
                            Brush.radialGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0.18f), Color.Transparent), center, r),
                            radius = r,
                            center = center,
                        )
                    }
                }
                Glyph(app, size, darken(appColor, 0.45f), outlined = false)
            }
        }

        // Marmo: base chiara con venature sinuose generate dal nome del pacchetto
        "MARBLE" -> {
            val seed = remember(app.key) { app.key.hashCode() }
            Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).clip(CircleShape)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Brush.linearGradient(listOf(Color(0xFFF4F2EE), Color(0xFFE2DFD8))))
                    val rnd = Random(seed)
                    val w = this.size.width
                    val h = this.size.height
                    repeat(4) { i ->
                        val path = Path()
                        val baseY = h * (0.15f + 0.22f * i + 0.08f * rnd.nextFloat())
                        val amp = h * (0.05f + 0.07f * rnd.nextFloat())
                        val freq = 2f + 3f * rnd.nextFloat()
                        val phase = rnd.nextFloat() * 6f
                        var x = 0f
                        path.moveTo(0f, baseY)
                        while (x <= w) {
                            path.lineTo(x, baseY + amp * sin(freq * x / w * 3.14f + phase) + (x / w) * h * 0.1f)
                            x += w / 24f
                        }
                        val vein = if (i % 2 == 0) Color(0xFF9E9A92) else appColor
                        drawPath(path, vein.copy(alpha = 0.35f + 0.25f * rnd.nextFloat()), style = Stroke(width = w * (0.012f + 0.02f * rnd.nextFloat())))
                    }
                }
                Glyph(app, size, Color(0xFF2B2A28), outlined = false)
            }
        }

        else -> Box(modifier.size(size))
    }
}

private fun lighten(c: Color, t: Float) = Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t, 1f)
private fun darken(c: Color, t: Float) = Color(c.red * (1 - t), c.green * (1 - t), c.blue * (1 - t), 1f)
