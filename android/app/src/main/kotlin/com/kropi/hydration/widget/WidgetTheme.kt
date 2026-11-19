package com.kropi.hydration.widget

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider
import com.kropi.hydration.data.WidgetStyle

/**
 * Kolory widgetu. Teksty i tła to [ColorProvider] (Glance sam dobiera wariant jasny/ciemny),
 * a [graphics] to te same kolory jako liczby — do bitmap, które Glance rysować nie umie.
 */
class WidgetPalette(
    val card: ColorProvider,
    val screen: ColorProvider,
    val secondary: ColorProvider,
    val foreground: ColorProvider,
    val muted: ColorProvider,
    val accent: ColorProvider,
    val accentMuted: ColorProvider,
    val accentSoft: ColorProvider,
    val warn: ColorProvider,
    val graphics: GraphicsPalette,
    /** Ta sama paleta z pomarańczowym akcentem — pierścień, gdy jesteś za planem. */
    val warnGraphics: GraphicsPalette,
)

val LocalPalette = compositionLocalOf<WidgetPalette> { error("WidgetTheme missing") }

/** Opakowuje treść widgetu w wybrany styl: własna paleta Kropi albo kolory z tapety. */
@Composable
fun WidgetTheme(style: WidgetStyle, content: @Composable () -> Unit) {
    val context = LocalContext.current
    if (style == WidgetStyle.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        GlanceTheme {
            val palette = materialPalette(context)
            CompositionLocalProvider(LocalPalette provides palette) { content() }
        }
    } else {
        CompositionLocalProvider(LocalPalette provides kropiPalette(context)) { content() }
    }
}

private fun isNight(context: Context): Boolean =
    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

private fun pair(day: Long, night: Long) = ColorProvider(day = Color(day), night = Color(night))
private fun pair(day: Color, night: Color) = ColorProvider(day = day, night = night)

private val WARN_DARK = Color(0xFFFF8A65)
private val WARN_LIGHT = Color(0xFFD84F1E)

private fun kropiPalette(context: Context): WidgetPalette {
    val night = isNight(context)
    val graphics = if (night) GraphicsPalette.DARK else GraphicsPalette.LIGHT
    val warn = if (night) WARN_DARK else WARN_LIGHT
    return WidgetPalette(
        card = pair(0xFFF3FAFC, 0xFF071A27),
        screen = pair(0xFFE3F0F4, 0xFF020F19),
        secondary = pair(0xFFD3E4EA, 0xFF132938),
        foreground = pair(0xFF06202C, 0xFFEEF7FA),
        muted = pair(0xFF4F6A77, 0xFF91A9B3),
        accent = pair(0xFF008C96, 0xFF00DFE8),
        accentMuted = pair(Color(0xFF008C96).copy(alpha = 0.55f), Color(0xFF00DFE8).copy(alpha = 0.55f)),
        accentSoft = pair(Color(0xFF00A3AD).copy(alpha = 0.22f), Color(0xFF00DFE8).copy(alpha = 0.28f)),
        warn = pair(WARN_LIGHT, WARN_DARK),
        graphics = graphics,
        warnGraphics = graphics.withAccent(warn.toArgb(), darken(warn.toArgb())),
    )
}

/** Kolory z motywu systemu; wywoływane wewnątrz [GlanceTheme], więc czytają paletę tapety. */
@Composable
private fun materialPalette(context: Context): WidgetPalette {
    val c = GlanceTheme.colors
    val accent = c.primary.getColor(context)
    val warn = c.error.getColor(context)
    val graphics = GraphicsPalette(
        accent = accent.toArgb(),
        accentDeep = darken(accent.toArgb()),
        track = c.secondaryContainer.getColor(context).toArgb(),
        card = c.widgetBackground.getColor(context).toArgb(),
        muted = c.onSurfaceVariant.getColor(context).toArgb(),
        foreground = c.onSurface.getColor(context).toArgb(),
        pupil = c.onSurface.getColor(context).toArgb(),
    )
    return WidgetPalette(
        card = c.widgetBackground,
        screen = c.surface,
        secondary = c.secondaryContainer,
        foreground = c.onSurface,
        muted = c.onSurfaceVariant,
        accent = c.primary,
        accentMuted = ColorProvider(accent.copy(alpha = 0.55f)),
        accentSoft = ColorProvider(accent.copy(alpha = 0.28f)),
        warn = c.error,
        graphics = graphics,
        warnGraphics = graphics.withAccent(warn.toArgb(), darken(warn.toArgb())),
    )
}

private fun darken(argb: Int): Int = ColorUtils.blendARGB(argb, 0xFF000000.toInt(), 0.4f)
