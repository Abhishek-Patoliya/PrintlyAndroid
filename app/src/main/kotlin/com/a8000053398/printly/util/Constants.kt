package com.a8000053398.printly.util

import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PhysicalSize
import com.a8000053398.printly.service.PhysicalMeasurementService

object PrintlyConstants {
    /** Recommended minimum print resolution for photo-quality output. */
    const val RECOMMENDED_DPI: Double = 300.0

    const val MIN_CUSTOM_DIMENSION_MM: Double = 5.0
    const val MAX_CUSTOM_DIMENSION_MM: Double = 2000.0

    val minResizeDimensionPoints: Double
        get() = PhysicalMeasurementService.points(MIN_CUSTOM_DIMENSION_MM, MeasurementUnit.MILLIMETER)
    val maxResizeDimensionPoints: Double
        get() = PhysicalMeasurementService.points(MAX_CUSTOM_DIMENSION_MM, MeasurementUnit.MILLIMETER)

    const val MIN_COPIES = 1
    const val MAX_COPIES = 200

    /** Caps how many pages a single imported PDF contributes as photos. */
    const val MAX_PDF_IMPORT_PAGES = 30

    /** Maximum user-applied zoom multiplier for the free image-transform gesture system. */
    const val MAX_IMAGE_ZOOM: Double = 8.0

    const val PRINT_INSTRUCTION_TEXT = "Print at 100% / Actual Size. Do not use \"Fit to Page.\""
}

/** Formats physical measurements for display, honoring the user's preferred unit. */
object MeasurementFormatter {
    fun stringForPoints(points: Double, unit: MeasurementUnit): String {
        val value = PhysicalMeasurementService.value(points, unit)
        return string(value, unit)
    }

    fun string(value: Double, unit: MeasurementUnit): String {
        val decimals = if (unit == MeasurementUnit.INCH) 2 else 1
        return "%.${decimals}f %s".format(value, unit.displayName)
    }

    fun sizeString(size: PhysicalSize): String {
        val decimals = if (size.unit == MeasurementUnit.INCH) 2 else 1
        return "%.${decimals}f × %.${decimals}f %s".format(size.width, size.height, size.unit.displayName)
    }
}

/** Returns the element at [index], or null if out of bounds. */
fun <T> List<T>.safe(index: Int): T? = if (index in indices) this[index] else null
