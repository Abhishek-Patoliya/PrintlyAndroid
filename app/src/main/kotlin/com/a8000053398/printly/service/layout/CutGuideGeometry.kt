package com.a8000053398.printly.service.layout

import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.model.PlacedPhoto

/** Pure geometry for print-shop style corner crop marks: two short line
 * segments at each corner of each placement, offset outward so they never
 * overlap the printed photo itself. */
object CutGuideGeometry {
    data class Segment(val start: PointD, val end: PointD)

    fun segments(placements: List<PlacedPhoto>, markLength: Double = 8.0, gap: Double = 2.0): List<Segment> =
        placements.flatMap { cornerSegments(it.frame, markLength, gap) }

    private fun cornerSegments(rect: RectD, markLength: Double, gap: Double): List<Segment> {
        val minX = rect.minX; val maxX = rect.maxX
        val minY = rect.minY; val maxY = rect.maxY
        return listOf(
            // Top-left
            Segment(PointD(minX - gap - markLength, minY), PointD(minX - gap, minY)),
            Segment(PointD(minX, minY - gap - markLength), PointD(minX, minY - gap)),
            // Top-right
            Segment(PointD(maxX + gap, minY), PointD(maxX + gap + markLength, minY)),
            Segment(PointD(maxX, minY - gap - markLength), PointD(maxX, minY - gap)),
            // Bottom-left
            Segment(PointD(minX - gap - markLength, maxY), PointD(minX - gap, maxY)),
            Segment(PointD(minX, maxY + gap), PointD(minX, maxY + gap + markLength)),
            // Bottom-right
            Segment(PointD(maxX + gap, maxY), PointD(maxX + gap + markLength, maxY)),
            Segment(PointD(maxX, maxY + gap), PointD(maxX, maxY + gap + markLength))
        )
    }
}
