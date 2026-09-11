package com.a8000053398.printly.model

import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/** A single source photo added by the user, independent of where/how many
 * times it is placed on the page. */
@Serializable
data class PhotoItem(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    /** Filename of the original (uncropped) image stored by `ProjectPersistenceService`. */
    val originalFileName: String,
    /** Filename of the cropped/adjusted image actually used, if different from original. */
    val editedFileName: String? = null,
    /** Crop rectangle in unit-square (0...1) coordinates relative to the original image. */
    val cropRect: RectD = RectD.unitSquare,
    /** Rotation applied to the photo itself (not its placement), in degrees, clockwise. */
    val rotationDegrees: Double = 0.0,
    /** How many copies of this specific photo should be placed on the page. */
    val copies: Int = 1,
    /** User-facing order in the source photo list. */
    val sortIndex: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    /** Brightness adjustment, roughly -1 (darker) ... 1 (brighter), 0 = unchanged. */
    val brightness: Double = 0.0,
    /** Contrast multiplier, roughly 0.5 (flatter) ... 1.5 (punchier), 1 = unchanged. */
    val contrast: Double = 1.0,
    val backgroundReplacement: PhotoBackgroundReplacement? = null,
    val fitMode: PhotoFitMode = PhotoFitMode.FILL,
    val textOverlay: TextOverlay? = null,
    val border: BorderStyle? = null,
    /** True for a blank text label/sticker created via "Add Text Label" rather than a picked photo. */
    val isLabel: Boolean = false,
    val transform: PhotoTransform = PhotoTransform.identity
) {
    val activeFileName: String get() = editedFileName ?: originalFileName

    /** True if any non-destructive adjustment differs from the untouched defaults. */
    val hasAdjustments: Boolean
        get() = cropRect != RectD.unitSquare || rotationDegrees != 0.0 || flipHorizontal || flipVertical ||
            brightness != 0.0 || contrast != 1.0 || backgroundReplacement != null || fitMode != PhotoFitMode.FILL ||
            textOverlay != null || border != null || transform != PhotoTransform.identity
}
