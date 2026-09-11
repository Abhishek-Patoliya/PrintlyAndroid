package com.a8000053398.printly.service.layout

import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.LayoutResult
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.model.PlacedPhoto
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Computes exact, physically-accurate photo placement on a single printed page.
 *
 * Deals exclusively in PDF/print points (1pt = 1/72in), derived from the
 * caller's page size, photo size, margins, and spacing — never from screen
 * pixels. Given the same inputs it always produces the same geometry, so the
 * identical [LayoutResult] can drive the on-screen preview and the exported
 * PDF/image without ever diverging.
 */
object PageLayoutEngine {

    fun layout(
        pageSize: SizeD,
        margins: PageMargins,
        photoSize: SizeD,
        spacing: PageSpacing,
        photoSequence: List<UUID>,
        allowRotationToFit: Boolean = false,
        fillPageEdgeToEdge: Boolean = false,
        occurrenceOffsets: Map<UUID, Int> = emptyMap()
    ): LayoutResult {
        val straight = grid(pageSize, margins, photoSize, spacing)

        var chosenPhotoSize = photoSize
        var chosenGrid = straight
        var rotated = false

        if (allowRotationToFit) {
            val swappedSize = SizeD(photoSize.height, photoSize.width)
            val alt = grid(pageSize, margins, swappedSize, spacing)
            if (alt.capacity > straight.capacity) {
                chosenPhotoSize = swappedSize
                chosenGrid = alt
                rotated = true
            }
        }

        val requestedCount = photoSequence.size
        val placedCount = minOf(requestedCount, chosenGrid.capacity)

        var effectiveSpacing = spacing
        var effectiveColumns = chosenGrid.columns
        var effectiveOrigin = chosenGrid.origin

        if (fillPageEdgeToEdge && placedCount > 0 && chosenGrid.columns > 0) {
            val printable = printableRect(pageSize, margins)
            val usedColumns = minOf(chosenGrid.columns, placedCount)
            val usedRows = ceil(placedCount.toDouble() / usedColumns).toInt()

            val horizontalGap = if (usedColumns > 1)
                (printable.width - usedColumns * chosenPhotoSize.width) / (usedColumns - 1) else 0.0
            val verticalGap = if (usedRows > 1)
                (printable.height - usedRows * chosenPhotoSize.height) / (usedRows - 1) else 0.0

            effectiveSpacing = PageSpacing(maxOf(0.0, horizontalGap), maxOf(0.0, verticalGap))
            effectiveColumns = usedColumns
            effectiveOrigin = printable.origin
        }

        val placements = placements(
            count = placedCount,
            photoSequence = photoSequence,
            columns = effectiveColumns,
            photoSize = chosenPhotoSize,
            spacing = effectiveSpacing,
            gridOrigin = effectiveOrigin,
            placementRotationDegrees = if (rotated) 90.0 else 0.0,
            occurrenceOffsets = occurrenceOffsets
        )

        return LayoutResult(
            pageSize = pageSize,
            printableRect = printableRect(pageSize, margins),
            photoSize = chosenPhotoSize,
            margins = margins,
            spacing = effectiveSpacing,
            columns = effectiveColumns,
            rows = chosenGrid.rows,
            maxCapacity = chosenGrid.capacity,
            requestedCount = requestedCount,
            placements = placements
        )
    }

    /** Splits [photoSequence] across as many pages as needed to place every
     * requested copy. Placement IDs stay stable across pages and across
     * recomputes by tracking each photo's cumulative occurrence count. */
    fun paginate(
        pageSize: SizeD,
        margins: PageMargins,
        photoSize: SizeD,
        spacing: PageSpacing,
        photoSequence: List<UUID>,
        allowRotationToFit: Boolean = false,
        fillPageEdgeToEdge: Boolean = false
    ): List<LayoutResult> {
        if (photoSequence.isEmpty()) {
            return listOf(
                layout(pageSize, margins, photoSize, spacing, emptyList(), allowRotationToFit, fillPageEdgeToEdge)
            )
        }

        val probe = layout(pageSize, margins, photoSize, spacing, photoSequence, allowRotationToFit, fillPageEdgeToEdge = false)
        if (probe.maxCapacity <= 0) return listOf(probe)

        val pages = mutableListOf<LayoutResult>()
        val cumulativeOccurrence = mutableMapOf<UUID, Int>()
        var index = 0
        while (index < photoSequence.size) {
            val chunk = photoSequence.subList(index, minOf(index + probe.maxCapacity, photoSequence.size))
            val page = layout(pageSize, margins, photoSize, spacing, chunk, allowRotationToFit, fillPageEdgeToEdge, cumulativeOccurrence.toMap())
            pages.add(page)
            for (id in chunk) cumulativeOccurrence[id] = (cumulativeOccurrence[id] ?: 0) + 1
            index += probe.maxCapacity
        }
        return pages
    }

    /** The printable area of the page after margins are subtracted, in points. */
    fun printableRect(pageSize: SizeD, margins: PageMargins): RectD = RectD(
        x = margins.left,
        y = margins.top,
        width = maxOf(0.0, pageSize.width - margins.left - margins.right),
        height = maxOf(0.0, pageSize.height - margins.top - margins.bottom)
    )

    private data class GridInfo(val columns: Int, val rows: Int, val origin: PointD) {
        val capacity: Int get() = columns * rows
    }

    private fun grid(pageSize: SizeD, margins: PageMargins, photoSize: SizeD, spacing: PageSpacing): GridInfo {
        val printable = printableRect(pageSize, margins)
        if (photoSize.width <= 0 || photoSize.height <= 0) return GridInfo(0, 0, printable.origin)

        val columns = maxCount(printable.width, photoSize.width, spacing.horizontal)
        val rows = maxCount(printable.height, photoSize.height, spacing.vertical)

        val totalGridWidth = columns * photoSize.width + maxOf(0, columns - 1) * spacing.horizontal
        val totalGridHeight = rows * photoSize.height + maxOf(0, rows - 1) * spacing.vertical

        val originX = printable.origin.x + (printable.width - totalGridWidth) / 2.0
        val originY = printable.origin.y + (printable.height - totalGridHeight) / 2.0

        return GridInfo(columns, rows, PointD(originX, originY))
    }

    /** n*cell + (n-1)*gap <= available => n <= (available + gap) / (cell + gap) */
    private fun maxCount(available: Double, cell: Double, gap: Double): Int {
        if (cell <= 0 || available <= 0) return 0
        val n = (available + gap) / (cell + gap)
        val epsilon = 1e-6
        return maxOf(0, floor(n + epsilon).toInt())
    }

    private fun placements(
        count: Int,
        photoSequence: List<UUID>,
        columns: Int,
        photoSize: SizeD,
        spacing: PageSpacing,
        gridOrigin: PointD,
        placementRotationDegrees: Double,
        occurrenceOffsets: Map<UUID, Int>
    ): List<PlacedPhoto> {
        if (columns <= 0 || count <= 0) return emptyList()

        val result = ArrayList<PlacedPhoto>(count)
        val occurrenceByPhotoID = occurrenceOffsets.toMutableMap()

        for (index in 0 until count) {
            val row = index / columns
            val column = index % columns
            val x = gridOrigin.x + column * (photoSize.width + spacing.horizontal)
            val y = gridOrigin.y + row * (photoSize.height + spacing.vertical)

            val photoID = photoSequence[index]
            val occurrence = occurrenceByPhotoID[photoID] ?: 0
            occurrenceByPhotoID[photoID] = occurrence + 1

            result.add(
                PlacedPhoto(
                    id = stablePlacementID(photoID, occurrence),
                    photoID = photoID,
                    origin = PointD(x, y),
                    size = photoSize,
                    placementRotationDegrees = placementRotationDegrees,
                    gridRow = row,
                    gridColumn = column
                )
            )
        }

        return result
    }

    /** Derives a deterministic placement ID from a photo's ID and how many times
     * that same photo has already appeared earlier in the sequence — the key
     * fix that makes manual drag/rotate overrides (keyed by placement ID)
     * survive across recomputes. */
    fun stablePlacementID(photoID: UUID, occurrence: Int): UUID {
        val bytes = java.nio.ByteBuffer.allocate(16)
        bytes.putLong(photoID.mostSignificantBits)
        bytes.putLong(photoID.leastSignificantBits)
        val array = bytes.array()
        val occurrenceBytes = java.nio.ByteBuffer.allocate(8).putLong(occurrence.toLong()).array()
        for (i in occurrenceBytes.indices) {
            array[8 + i] = (array[8 + i].toInt() xor occurrenceBytes[i].toInt()).toByte()
        }
        val buffer = java.nio.ByteBuffer.wrap(array)
        return UUID(buffer.long, buffer.long)
    }
}
