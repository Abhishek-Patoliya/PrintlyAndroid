package com.a8000053398.printly.model

import kotlinx.serialization.Serializable

/** Where a [TextOverlay] sits on its photo/label, in unit-square terms. */
@Serializable
enum class OverlayPosition(val displayName: String) {
    TOP_LEFT("Top Left"), TOP_CENTER("Top"), TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"), BOTTOM_CENTER("Bottom"), BOTTOM_RIGHT("Bottom Right"),
    CENTER("Center");
}

/** A short line of text (name, date, watermark, caption) drawn over a photo
 * or label at export/preview time — never baked into the original file. */
@Serializable
data class TextOverlay(
    val text: String,
    val position: OverlayPosition,
    /** Font size in points (72pt = 1in), scaled to the photo's actual pixel resolution. */
    val fontSizePt: Double,
    val color: RGBColor,
    /** 0...1. Doubling as watermark control. */
    val opacity: Double,
    /** Draws a soft dark pill behind the text for legibility over busy photos. */
    val hasBackgroundPill: Boolean
) {
    companion object {
        val default = TextOverlay(
            text = "",
            position = OverlayPosition.BOTTOM_CENTER,
            fontSizePt = 14.0,
            color = RGBColor(1.0, 1.0, 1.0),
            opacity = 1.0,
            hasBackgroundPill = true
        )
    }
}

/** A stroked border drawn around a photo's/label's exact printed edge. */
@Serializable
data class BorderStyle(val widthPt: Double, val color: RGBColor) {
    companion object {
        val default = BorderStyle(2.0, RGBColor(0.0, 0.0, 0.0))
    }
}

/** How a photo's cropped content maps onto its exact physical placement size. */
@Serializable
enum class PhotoFitMode(val displayName: String) {
    /** Crops to fill the target size edge-to-edge. */
    FILL("Fill"),
    /** Shows the entire photo uncropped, letterboxed with padding on the shorter axis. */
    FIT("Fit");
}
