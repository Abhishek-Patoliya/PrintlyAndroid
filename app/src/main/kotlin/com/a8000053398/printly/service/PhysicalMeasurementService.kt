package com.a8000053398.printly.service

import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PhysicalSize
import com.a8000053398.printly.util.PrintlyConstants

/**
 * Single source of truth for converting between physical units and PDF/print points.
 *
 * PDF (and this app's own page/photo/canvas geometry) uses "points" where
 * 1 point = 1/72 inch. Every physical-to-screen or physical-to-PDF conversion
 * in the app flows through this object so a `2 inch` photo is always exactly
 * `144pt` wherever it is rendered.
 */
object PhysicalMeasurementService {
    const val POINTS_PER_INCH: Double = 72.0
    const val MILLIMETERS_PER_INCH: Double = 25.4
    const val CENTIMETERS_PER_INCH: Double = 2.54

    fun points(value: Double, unit: MeasurementUnit): Double = when (unit) {
        MeasurementUnit.INCH -> value * POINTS_PER_INCH
        MeasurementUnit.MILLIMETER -> (value / MILLIMETERS_PER_INCH) * POINTS_PER_INCH
        MeasurementUnit.CENTIMETER -> (value / CENTIMETERS_PER_INCH) * POINTS_PER_INCH
    }

    fun value(points: Double, unit: MeasurementUnit): Double = when (unit) {
        MeasurementUnit.INCH -> points / POINTS_PER_INCH
        MeasurementUnit.MILLIMETER -> (points / POINTS_PER_INCH) * MILLIMETERS_PER_INCH
        MeasurementUnit.CENTIMETER -> (points / POINTS_PER_INCH) * CENTIMETERS_PER_INCH
    }

    fun convert(value: Double, from: MeasurementUnit, to: MeasurementUnit): Double {
        if (from == to) return value
        return value(points(value, from), to)
    }

    fun points(size: PhysicalSize): SizeD =
        SizeD(points(size.width, size.unit), points(size.height, size.unit))

    /** Required source-image pixel dimensions to render [size] at [dpi] without upsampling. */
    fun requiredPixelSize(size: PhysicalSize, dpi: Double): SizeD {
        val widthInches = convert(size.width, size.unit, MeasurementUnit.INCH)
        val heightInches = convert(size.height, size.unit, MeasurementUnit.INCH)
        return SizeD(widthInches * dpi, heightInches * dpi)
    }

    /** Effective print DPI a source image will yield when placed into [physicalSize]. */
    fun effectiveDPI(pixelSize: SizeD, physicalSize: PhysicalSize): Double {
        val widthInches = convert(physicalSize.width, physicalSize.unit, MeasurementUnit.INCH)
        if (widthInches <= 0) return 0.0
        return pixelSize.width / widthInches
    }
}

val PhysicalSize.pointSize: SizeD get() = PhysicalMeasurementService.points(this)

/** Clamps width/height to the app-wide valid custom-dimension range
 * ([PrintlyConstants.MIN_CUSTOM_DIMENSION_MM]/[PrintlyConstants.MAX_CUSTOM_DIMENSION_MM]),
 * converted into this size's own unit. */
fun PhysicalSize.clampedToValidRange(): PhysicalSize {
    val minValue = PhysicalMeasurementService.convert(PrintlyConstants.MIN_CUSTOM_DIMENSION_MM, MeasurementUnit.MILLIMETER, unit)
    val maxValue = PhysicalMeasurementService.convert(PrintlyConstants.MAX_CUSTOM_DIMENSION_MM, MeasurementUnit.MILLIMETER, unit)
    return PhysicalSize(
        width = width.coerceIn(minValue, maxValue),
        height = height.coerceIn(minValue, maxValue),
        unit = unit
    )
}
