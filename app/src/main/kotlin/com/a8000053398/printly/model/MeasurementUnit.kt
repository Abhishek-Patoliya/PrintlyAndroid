package com.a8000053398.printly.model

import kotlinx.serialization.Serializable

/** A physical length unit a user can enter dimensions in. */
@Serializable
enum class MeasurementUnit(val displayName: String) {
    MILLIMETER("mm"),
    CENTIMETER("cm"),
    INCH("in");
}

/** A width/height pair expressed in a specific [MeasurementUnit]. */
@Serializable
data class PhysicalSize(val width: Double, val height: Double, val unit: MeasurementUnit) {
    val swapped: PhysicalSize get() = PhysicalSize(height, width, unit)
}
