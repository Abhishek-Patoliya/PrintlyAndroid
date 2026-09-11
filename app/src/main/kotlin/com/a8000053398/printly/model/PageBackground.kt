package com.a8000053398.printly.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** The page's background fill, used both in the on-screen preview and baked
 * into the exported PDF/image. A small set of print-safe, paper-like tones. */
@Serializable
enum class PageBackground(val displayName: String, val red: Double, val green: Double, val blue: Double) {
    WHITE("White", 1.0, 1.0, 1.0),
    IVORY("Ivory", 0.996, 0.988, 0.949),
    LIGHT_GRAY("Light Gray", 0.914, 0.914, 0.918),
    BLACK("Black", 0.06, 0.06, 0.06);

    val color: Color get() = Color(red.toFloat(), green.toFloat(), blue.toFloat())
    /** ARGB int, matching `android.graphics.Color` conventions used by Canvas/PdfDocument drawing. */
    val argb: Int get() = android.graphics.Color.rgb((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
}
