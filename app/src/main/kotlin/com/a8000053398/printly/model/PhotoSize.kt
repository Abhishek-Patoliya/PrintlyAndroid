package com.a8000053398.printly.model

import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.service.pointSize
import kotlinx.serialization.Serializable

@Serializable
enum class PhotoSizePreset(val displayName: String) {
    ONE_BY_ONE("1 × 1 in"),
    TWO_BY_TWO("2 × 2 in"),
    FOUR_BY_SIX("4 × 6 in"),
    FIVE_BY_SEVEN("5 × 7 in"),
    SIX_BY_EIGHT("6 × 8 in"),
    EIGHT_BY_TEN("8 × 10 in"),
    MM_35X45("35 × 45 mm"),
    CM_3X4("3 × 4 cm"),
    PASSPORT("Passport (2 × 2 in)"),
    ID_PHOTO("ID (35 × 45 mm)"),
    PAN_CARD("PAN Card (25 × 35 mm)"),
    DRIVING_LICENCE("Driving Licence (35 × 45 mm)"),
    CUSTOM("Custom");

    /** Base physical size, width × height, as commonly specified for the preset. */
    val baseSize: PhysicalSize?
        get() = when (this) {
            ONE_BY_ONE -> PhysicalSize(1.0, 1.0, MeasurementUnit.INCH)
            TWO_BY_TWO -> PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)
            FOUR_BY_SIX -> PhysicalSize(4.0, 6.0, MeasurementUnit.INCH)
            FIVE_BY_SEVEN -> PhysicalSize(5.0, 7.0, MeasurementUnit.INCH)
            SIX_BY_EIGHT -> PhysicalSize(6.0, 8.0, MeasurementUnit.INCH)
            EIGHT_BY_TEN -> PhysicalSize(8.0, 10.0, MeasurementUnit.INCH)
            MM_35X45 -> PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)
            CM_3X4 -> PhysicalSize(3.0, 4.0, MeasurementUnit.CENTIMETER)
            PASSPORT -> PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)
            ID_PHOTO -> PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)
            PAN_CARD -> PhysicalSize(25.0, 35.0, MeasurementUnit.MILLIMETER)
            DRIVING_LICENCE -> PhysicalSize(35.0, 45.0, MeasurementUnit.MILLIMETER)
            CUSTOM -> null
        }

    /** Whether this preset commonly needs a strict passport/ID face-crop workflow. */
    val isIdentityDocument: Boolean
        get() = this in setOf(PASSPORT, ID_PHOTO, CM_3X4, PAN_CARD, DRIVING_LICENCE)
}

/** The exact physical size a single printed photo must be, e.g. 2x2 inch. */
@Serializable
data class PhotoSize(val preset: PhotoSizePreset, val customSize: PhysicalSize) {
    val physicalSize: PhysicalSize get() = preset.baseSize ?: customSize
    val pointSize: SizeD get() = physicalSize.pointSize
    val displayName: String get() = preset.displayName

    companion object {
        fun standard(preset: PhotoSizePreset): PhotoSize =
            PhotoSize(preset, preset.baseSize ?: PhysicalSize(2.0, 2.0, MeasurementUnit.INCH))

        val default: PhotoSize get() = standard(PhotoSizePreset.TWO_BY_TWO)
    }
}
