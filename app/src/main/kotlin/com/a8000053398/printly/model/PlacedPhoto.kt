package com.a8000053398.printly.model

import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.core.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * One physical instance of a photo placed on the page, in PDF/print points.
 *
 * All geometry here is expressed in points (1/72 inch), in a coordinate space
 * where (0,0) is the top-left of the page and Y increases downward — the
 * single representation shared by the on-screen preview, the visual editor,
 * and the PDF/image exporter.
 */
@Serializable
data class PlacedPhoto(
    /** Stable identity for one placement slot (not the same as the source PhotoItem.id). */
    @Serializable(with = UUIDSerializer::class) val id: UUID,
    /** The source photo this placement renders. */
    @Serializable(with = UUIDSerializer::class) val photoID: UUID,
    /** Top-left position of the (unrotated) bounding box, in points. */
    val origin: PointD,
    /** Exact physical size of this placement, in points. */
    val size: SizeD,
    /** Additional placement rotation (on top of the photo's own rotationDegrees), clockwise. */
    val placementRotationDegrees: Double = 0.0,
    val gridRow: Int,
    val gridColumn: Int,
    val isManuallyPositioned: Boolean = false
) {
    val frame: RectD get() = RectD.of(origin, size)
    val center: PointD get() = PointD(origin.x + size.width / 2, origin.y + size.height / 2)

    /** `size` is the placement's own axis-aligned footprint — before any rotation.
     * When drawing the source image (stored upright, pre-rotation) into a rotated
     * context, a quarter turn (90/270) swaps width/height so the rotated image still
     * exactly fills the original `size` footprint; any other angle keeps drawing at
     * `size` itself, since there's no swap that lands a non-90° rotation back on the
     * same axis-aligned box. */
    val preRotationDrawSize: SizeD
        get() {
            val normalized = placementRotationDegrees.mod(360.0)
            val isQuarterTurn = kotlin.math.abs(normalized).mod(180.0) == 90.0
            return if (isQuarterTurn) SizeD(size.height, size.width) else size
        }
}
