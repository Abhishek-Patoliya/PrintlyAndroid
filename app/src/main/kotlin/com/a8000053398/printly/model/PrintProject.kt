package com.a8000053398.printly.model

import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.core.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/** A manual, user-driven adjustment to a single placement's position/rotation,
 * applied on top of the engine's auto-computed grid layout. `size` is only set
 * once the user drags the placement's resize handles directly on the canvas. */
@Serializable
data class ManualPlacementOverride(
    val origin: PointD,
    val size: SizeD? = null,
    val rotationDegrees: Double
)

/** A user's print job: the photos they picked plus every physical setting
 * needed to lay them out on one page and export an exact PDF. */
@Serializable
data class PrintProject(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val name: String = "Untitled Project",
    val pageSize: PageSize = PageSize.default,
    val photoSize: PhotoSize = PhotoSize.default,
    val margins: PageMargins = PageMargins.uniform(28.346), // 10mm in points, resolved by callers via PhysicalMeasurementService
    val spacing: PageSpacing = PageSpacing.uniform(8.503), // 3mm in points
    val photos: List<PhotoItem> = emptyList(),
    /** Manual overrides keyed by `PlacedPhoto.id` (as a string), applied after auto-arrange. */
    val manualPlacementOverrides: Map<String, ManualPlacementOverride> = emptyMap(),
    val preferredUnit: MeasurementUnit = MeasurementUnit.MILLIMETER,
    /** When true, the layout engine tries rotating photos 90° if that fits more copies per page. */
    val allowRotationToFit: Boolean = false,
    /** When true, spacing is recomputed so the grid spans the printable area edge-to-edge. */
    val fillPageEdgeToEdge: Boolean = false,
    val backgroundColor: PageBackground = PageBackground.WHITE,
    /** When true, print-shop style corner crop marks are drawn around every placement. */
    val showCutGuides: Boolean = false,
    /** The built-in template this project was created from, if any (for reference only). */
    @Serializable(with = UUIDSerializer::class) val templateID: UUID? = null,
    /** A template's suggested copy count, applied to the first photo added to an
     * otherwise-empty project, then cleared. */
    val pendingCopiesForFirstPhoto: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis()
) {
    /** Total number of individual photo placements requested across all source photos. */
    val totalRequestedCopies: Int get() = photos.sumOf { it.copies.coerceAtLeast(0) }

    /** Expands `photos` into a flat sequence of photo IDs in placement order,
     * respecting each photo's `copies` count. */
    val expandedPhotoSequence: List<UUID>
        get() = photos.sortedBy { it.sortIndex }.flatMap { photo -> List(photo.copies.coerceAtLeast(0)) { photo.id } }
}
