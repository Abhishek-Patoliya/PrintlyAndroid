package com.a8000053398.printly.service

import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.PlacedPhoto
import kotlin.math.abs

/** Alignment guide(s), in physical page points, that a drag is currently snapped to. */
data class SnapGuides(val verticalX: Double?, val horizontalY: Double?)

data class SnapResult(val origin: PointD, val guides: SnapGuides?)

/** Pure snap-to-grid math for dragging a placement on the page canvas. */
object SnapGuideService {
    const val DEFAULT_THRESHOLD: Double = 6.0

    /** Snaps [proposedOrigin] toward nearby alignment targets — the printable
     * area's edges/center and every other placement's edges/center —
     * independently on each axis, within [threshold] points. */
    fun snappedOrigin(
        proposedOrigin: PointD,
        size: SizeD,
        printableRect: RectD,
        otherPlacements: List<PlacedPhoto>,
        threshold: Double = DEFAULT_THRESHOLD
    ): SnapResult {
        val xTargets = mutableListOf(printableRect.minX, printableRect.midX, printableRect.maxX)
        val yTargets = mutableListOf(printableRect.minY, printableRect.midY, printableRect.maxY)
        for (other in otherPlacements) {
            xTargets.addAll(listOf(other.origin.x, other.center.x, other.origin.x + other.size.width))
            yTargets.addAll(listOf(other.origin.y, other.center.y, other.origin.y + other.size.height))
        }

        var snappedX = proposedOrigin.x
        var verticalGuideX: Double? = null
        for (edgeOffset in listOf(0.0, size.width / 2, size.width)) {
            val edgeValue = proposedOrigin.x + edgeOffset
            val target = xTargets.firstOrNull { abs(it - edgeValue) <= threshold }
            if (target != null) {
                snappedX = target - edgeOffset
                verticalGuideX = target
                break
            }
        }

        var snappedY = proposedOrigin.y
        var horizontalGuideY: Double? = null
        for (edgeOffset in listOf(0.0, size.height / 2, size.height)) {
            val edgeValue = proposedOrigin.y + edgeOffset
            val target = yTargets.firstOrNull { abs(it - edgeValue) <= threshold }
            if (target != null) {
                snappedY = target - edgeOffset
                horizontalGuideY = target
                break
            }
        }

        val guides = if (verticalGuideX != null || horizontalGuideY != null) SnapGuides(verticalGuideX, horizontalGuideY) else null
        return SnapResult(PointD(snappedX, snappedY), guides)
    }

    /** Whether each axis of [guides] sits exactly on the printable area's own center line. */
    fun isAtPrintableCenter(guides: SnapGuides, printableRect: RectD): Pair<Boolean, Boolean> {
        val isVerticalCenter = guides.verticalX?.let { abs(it - printableRect.midX) < 0.01 } ?: false
        val isHorizontalCenter = guides.horizontalY?.let { abs(it - printableRect.midY) < 0.01 } ?: false
        return isHorizontalCenter to isVerticalCenter
    }
}
