package com.a8000053398.printly.service.layout

import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import kotlin.math.hypot
import kotlin.math.sqrt

/** One of the 8 handles drawn around a selected placement's axis-aligned
 * footprint on the canvas. `xSign`/`ySign` describe which edge(s) a handle
 * controls: -1 means the handle is on the min side (dragging keeps the max
 * side anchored), +1 the max side, 0 means this handle doesn't move that axis. */
enum class ResizeHandle(val xSign: Int, val ySign: Int) {
    TOP_LEFT(-1, -1), TOP(0, -1), TOP_RIGHT(1, -1),
    RIGHT(1, 0), BOTTOM_RIGHT(1, 1), BOTTOM(0, 1),
    BOTTOM_LEFT(-1, 1), LEFT(-1, 0);

    /** Corner handles resize both dimensions together, locked to the placement's
     * starting aspect ratio; side handles resize one dimension independently. */
    val isCorner: Boolean get() = xSign != 0 && ySign != 0

    /** Fractional position along a placement's frame used to place this handle
     * on screen: 0 = left/top edge, 0.5 = middle, 1 = right/bottom edge. */
    val fractionX: Double get() = (xSign + 1) / 2.0
    val fractionY: Double get() = (ySign + 1) / 2.0
}

/** Pure resize math for dragging a placement's handle on the page canvas. */
object ResizeGeometryService {

    /** Computes the new page-space frame for a placement being resized from
     * [handle], given the frame at gesture start and the cumulative drag
     * translation (in page points). Corner handles keep the diagonally opposite
     * corner anchored and lock the result to the start frame's aspect ratio;
     * side handles move only their own edge, independently. The result is
     * clamped to [minSide]...[maxSide] and to stay within [pageSize]. */
    fun resizedFrame(
        handle: ResizeHandle,
        startFrame: RectD,
        translation: SizeD,
        pageSize: SizeD,
        minSide: Double,
        maxSide: Double
    ): RectD {
        val xSign = handle.xSign
        val ySign = handle.ySign

        var width = startFrame.width
        var height = startFrame.height

        if (handle.isCorner) {
            val projection = (translation.width * xSign + translation.height * ySign) / sqrt(2.0)
            val startDiagonal = hypot(width, height)
            if (startDiagonal <= 0) return startFrame
            val scale = maxOf(0.01, (startDiagonal + projection) / startDiagonal)
            width *= scale
            height *= scale
        } else {
            if (xSign != 0) width += translation.width * xSign
            if (ySign != 0) height += translation.height * ySign
        }

        if (handle.isCorner) {
            val (w, h) = clampPreservingAspect(width, height, minSide, maxSide)
            width = w; height = h
        } else {
            width = width.coerceIn(minSide, maxSide)
            height = height.coerceIn(minSide, maxSide)
        }

        val availableWidth = if (xSign < 0) startFrame.maxX else pageSize.width - startFrame.minX
        val availableHeight = if (ySign < 0) startFrame.maxY else pageSize.height - startFrame.minY

        if (handle.isCorner) {
            var boundsScale = 1.0
            if (availableWidth > 0) boundsScale = minOf(boundsScale, availableWidth / width)
            if (availableHeight > 0) boundsScale = minOf(boundsScale, availableHeight / height)
            width *= boundsScale
            height *= boundsScale
        } else {
            if (xSign != 0) width = minOf(width, maxOf(0.0, availableWidth))
            if (ySign != 0) height = minOf(height, maxOf(0.0, availableHeight))
        }

        val originX = if (xSign < 0) startFrame.maxX - width else startFrame.minX
        val originY = if (ySign < 0) startFrame.maxY - height else startFrame.minY

        return RectD(originX, originY, width, height)
    }

    private fun clampPreservingAspect(width: Double, height: Double, minSide: Double, maxSide: Double): Pair<Double, Double> {
        if (width <= 0 || height <= 0) return minSide to minSide
        var w = width
        var h = height

        val largest = maxOf(w, h)
        if (largest > maxSide) {
            val scale = maxSide / largest
            w *= scale; h *= scale
        }

        val smallest = minOf(w, h)
        if (smallest < minSide) {
            val scale = minSide / smallest
            w *= scale; h *= scale
        }

        return w to h
    }
}
