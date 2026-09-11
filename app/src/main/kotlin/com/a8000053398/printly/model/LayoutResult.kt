package com.a8000053398.printly.model

import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import kotlinx.serialization.Serializable

/** Page margins in points, one value per edge. */
@Serializable
data class PageMargins(val top: Double, val bottom: Double, val left: Double, val right: Double) {
    companion object {
        val zero = PageMargins(0.0, 0.0, 0.0, 0.0)
        fun uniform(value: Double) = PageMargins(value, value, value, value)
    }
}

/** Spacing between adjacent grid cells, in points. */
@Serializable
data class PageSpacing(val horizontal: Double, val vertical: Double) {
    companion object {
        val zero = PageSpacing(0.0, 0.0)
        fun uniform(value: Double) = PageSpacing(value, value)
    }
}

/** The full, exact output of `PageLayoutEngine` for one page: everything needed
 * to render a preview or export a physically-accurate PDF, expressed in points. */
data class LayoutResult(
    val pageSize: SizeD,
    val printableRect: RectD,
    val photoSize: SizeD,
    val margins: PageMargins,
    val spacing: PageSpacing,
    val columns: Int,
    val rows: Int,
    val maxCapacity: Int,
    val requestedCount: Int,
    val placements: List<PlacedPhoto>
) {
    val didOverflow: Boolean get() = requestedCount > maxCapacity
    val overflowCount: Int get() = (requestedCount - maxCapacity).coerceAtLeast(0)
}
