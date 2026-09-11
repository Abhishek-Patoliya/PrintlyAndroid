package com.a8000053398.printly.model

import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.service.pointSize
import kotlinx.serialization.Serializable

@Serializable
enum class PageOrientation(val displayName: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape"),
    /** Lets the layout engine pick whichever orientation fits more copies per page. */
    AUTO("Auto");
}

@Serializable
enum class PageSizePreset(val displayName: String) {
    A4("A4"),
    A5("A5"),
    A3("A3"),
    LETTER("Letter"),
    LEGAL("Legal"),
    FOUR_BY_SIX("4 × 6 in"),
    FIVE_BY_SEVEN("5 × 7 in"),
    CUSTOM("Custom");

    /** Base physical size in portrait orientation (width < height, or equal). */
    val basePortraitSize: PhysicalSize?
        get() = when (this) {
            A4 -> PhysicalSize(210.0, 297.0, MeasurementUnit.MILLIMETER)
            A5 -> PhysicalSize(148.0, 210.0, MeasurementUnit.MILLIMETER)
            A3 -> PhysicalSize(297.0, 420.0, MeasurementUnit.MILLIMETER)
            LETTER -> PhysicalSize(8.5, 11.0, MeasurementUnit.INCH)
            LEGAL -> PhysicalSize(8.5, 14.0, MeasurementUnit.INCH)
            FOUR_BY_SIX -> PhysicalSize(4.0, 6.0, MeasurementUnit.INCH)
            FIVE_BY_SEVEN -> PhysicalSize(5.0, 7.0, MeasurementUnit.INCH)
            CUSTOM -> null
        }
}

/** A page the user prints onto: a standard/custom physical size plus orientation. */
@Serializable
data class PageSize(
    val preset: PageSizePreset,
    val customSize: PhysicalSize,
    val orientation: PageOrientation
) {
    private val portraitBaseSize: PhysicalSize get() = preset.basePortraitSize ?: customSize

    val physicalSize: PhysicalSize get() = physicalSize(orientation)

    fun physicalSize(forOrientation: PageOrientation): PhysicalSize {
        val base = portraitBaseSize
        return when (forOrientation) {
            PageOrientation.PORTRAIT, PageOrientation.AUTO -> base
            PageOrientation.LANDSCAPE -> base.swapped
        }
    }

    val pointSize: SizeD get() = physicalSize.pointSize

    val displayName: String get() = preset.displayName

    companion object {
        fun standard(preset: PageSizePreset, orientation: PageOrientation = PageOrientation.PORTRAIT): PageSize =
            PageSize(preset, preset.basePortraitSize ?: PhysicalSize(4.0, 6.0, MeasurementUnit.INCH), orientation)

        val default: PageSize get() = standard(PageSizePreset.A4)
    }
}
