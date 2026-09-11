package com.a8000053398.printly.service.layout

import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageSpacing

/** Computes the photo cell size for an N-photo collage: a grid of `count`
 * equal-sized cells that exactly tile one page's printable area. A collage is
 * otherwise just an ordinary project — a custom photo size set to this
 * computed cell size, with `fillPageEdgeToEdge` on. */
object CollageLayoutService {
    /** Collage sizes offered throughout the app. */
    val supportedCounts = listOf(2, 4, 6, 8, 9, 12, 16)

    /** Preferred (columns, rows) factorization for a supported collage count. */
    fun grid(count: Int, pageIsLandscape: Boolean): Pair<Int, Int> {
        val portraitFactors = when (count) {
            2 -> 1 to 2
            4 -> 2 to 2
            6 -> 2 to 3
            8 -> 2 to 4
            9 -> 3 to 3
            12 -> 3 to 4
            16 -> 4 to 4
            else -> 1 to maxOf(1, count)
        }
        return if (pageIsLandscape) portraitFactors.second to portraitFactors.first else portraitFactors
    }

    /** The exact photo cell size, in points, so `columns * rows` cells fill the
     * page's printable area edge-to-edge given [margins] and [spacing]. */
    fun cellSize(pageSize: SizeD, margins: PageMargins, spacing: PageSpacing, columns: Int, rows: Int): SizeD {
        val printable = PageLayoutEngine.printableRect(pageSize, margins)
        val width = (printable.width - (columns - 1) * spacing.horizontal) / columns
        val height = (printable.height - (rows - 1) * spacing.vertical) / rows
        return SizeD(maxOf(1.0, width), maxOf(1.0, height))
    }
}
