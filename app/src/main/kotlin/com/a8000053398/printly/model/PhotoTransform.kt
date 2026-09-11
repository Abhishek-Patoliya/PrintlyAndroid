package com.a8000053398.printly.model

import kotlinx.serialization.Serializable

/** A free rotate/zoom/pan transform describing how a photo's already-cropped
 * (frame-aspect-ratio) content is positioned within its fixed physical frame —
 * separate from [PhotoItem.cropRect], which only ever picks *which* rectangular
 * portion of the original image to bring into the frame. */
@Serializable
data class PhotoTransform(
    /** Pan, as a fraction of the frame's own width. */
    val offsetXFraction: Double,
    /** Pan, as a fraction of the frame's own height. */
    val offsetYFraction: Double,
    /** User-applied zoom, >= 1. */
    val zoom: Double,
    /** Free rotation, in degrees, clockwise. */
    val rotationDegrees: Double
) {
    companion object {
        val identity = PhotoTransform(0.0, 0.0, 1.0, 0.0)
    }
}
