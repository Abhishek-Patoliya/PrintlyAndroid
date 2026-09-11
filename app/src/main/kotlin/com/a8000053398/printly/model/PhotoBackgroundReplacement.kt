package com.a8000053398.printly.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** A plain RGB color, independent of Compose's [Color], so [PhotoItem] can
 * persist a user-picked custom background color as ordinary JSON. */
@Serializable
data class RGBColor(val red: Double, val green: Double, val blue: Double) {
    val color: Color get() = Color(red.toFloat(), green.toFloat(), blue.toFloat())
    val argb: Int get() = android.graphics.Color.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

    companion object {
        fun from(color: Color) = RGBColor(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())
    }
}

/** A small, print-safe palette for ID-photo background replacement, plus a custom option. */
@Serializable
enum class PhotoBackgroundPreset(val displayName: String) {
    WHITE("White"), LIGHT_GRAY("Light Gray"), BLUE("Blue"), CUSTOM("Custom");

    /** Fixed color for every preset except [CUSTOM], which instead reads
     * [PhotoBackgroundReplacement.customColor]. */
    val fixedColor: RGBColor?
        get() = when (this) {
            WHITE -> RGBColor(1.0, 1.0, 1.0)
            LIGHT_GRAY -> RGBColor(0.88, 0.88, 0.89)
            BLUE -> RGBColor(0.20, 0.42, 0.72)
            CUSTOM -> null
        }
}

/** A non-destructive background swap applied when resolving a photo for
 * layout/export — the original image on disk is never modified. */
@Serializable
data class PhotoBackgroundReplacement(val preset: PhotoBackgroundPreset, val customColor: RGBColor) {
    val resolvedColor: RGBColor get() = preset.fixedColor ?: customColor

    companion object {
        fun standard(preset: PhotoBackgroundPreset) =
            PhotoBackgroundReplacement(preset, preset.fixedColor ?: RGBColor(1.0, 1.0, 1.0))
    }
}
