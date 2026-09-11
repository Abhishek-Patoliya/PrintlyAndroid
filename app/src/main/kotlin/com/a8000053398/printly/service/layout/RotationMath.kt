package com.a8000053398.printly.service.layout

import com.a8000053398.printly.core.PointD
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Pure 2D rotation math for a y-down (screen/page) coordinate system, where
 * positive degrees means clockwise. */
object RotationMath {
    fun rotate(point: PointD, byDegrees: Double): PointD {
        val radians = byDegrees * Math.PI / 180
        val cosT = cos(radians)
        val sinT = sin(radians)
        return PointD(
            x = point.x * cosT - point.y * sinT,
            y = point.x * sinT + point.y * cosT
        )
    }

    /** Inverse of [rotate] — converts a vector in the outer (screen/page) frame
     * into the local, unrotated frame of something rotated by [byDegrees]. */
    fun derotate(point: PointD, byDegrees: Double): PointD = rotate(point, -byDegrees)

    /** Angle, in degrees clockwise from the positive x-axis, from [origin] to [point]. */
    fun angleDegrees(origin: PointD, point: PointD): Double {
        val dx = point.x - origin.x
        val dy = point.y - origin.y
        return atan2(dy, dx) * 180 / Math.PI
    }
}
