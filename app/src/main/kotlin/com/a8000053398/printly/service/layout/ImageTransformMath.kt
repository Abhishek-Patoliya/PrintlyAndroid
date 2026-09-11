package com.a8000053398.printly.service.layout

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Pure geometry for the free rotate/zoom/pan image manipulation system.
 *
 * A rectangle that exactly fills a frame at rotation 0 will reveal gaps at the
 * frame's corners once rotated, unless it's scaled up first. [coverMultiplier]
 * computes exactly how much, and [clampedOffsetFraction] computes how far the
 * content can be panned before a gap would appear.
 */
object ImageTransformMath {
    /** The extra scale factor (>= 1), beyond "fills the frame with no rotation,"
     * needed for an [aspectRatio]-shaped rectangle rotated by [radians] to still
     * fully cover its own unrotated footprint (the frame). */
    fun coverMultiplier(aspectRatio: Double, radians: Double): Double {
        if (aspectRatio <= 0) return 1.0
        val w = aspectRatio
        val h = 1.0
        val cosT = abs(cos(radians))
        val sinT = abs(sin(radians))
        val boundingWidth = w * cosT + h * sinT
        val boundingHeight = w * sinT + h * cosT
        return maxOf(boundingWidth / w, boundingHeight / h)
    }

    data class Offset(val width: Double, val height: Double)

    /** Clamps a pan offset (as a fraction of the frame's own width/height) so the
     * content — after [zoom] (>= 1) and [radians] rotation — never reveals empty
     * space at the frame's edges. */
    fun clampedOffsetFraction(offset: Offset, aspectRatio: Double, zoom: Double, radians: Double): Offset {
        if (aspectRatio <= 0 || !zoom.isFinite() || zoom <= 0) return Offset(0.0, 0.0)
        val w = aspectRatio
        val h = 1.0
        val cosT = abs(cos(radians))
        val sinT = abs(sin(radians))
        val effectiveScale = zoom * coverMultiplier(aspectRatio, radians)
        val boundingWidth = effectiveScale * (w * cosT + h * sinT)
        val boundingHeight = effectiveScale * (w * sinT + h * cosT)
        val maxXFraction = maxOf(0.0, (boundingWidth - w) / 2) / w
        val maxYFraction = maxOf(0.0, (boundingHeight - h) / 2) / h
        return Offset(
            width = offset.width.coerceIn(-maxXFraction, maxXFraction),
            height = offset.height.coerceIn(-maxYFraction, maxYFraction)
        )
    }
}
