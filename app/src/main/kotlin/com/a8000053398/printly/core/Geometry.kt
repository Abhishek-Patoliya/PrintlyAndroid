package com.a8000053398.printly.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

/** JSON-friendly UUID serializer so model classes can use [UUID] directly, the
 * same way `PrintProject` etc. use Swift's natively-Codable `UUID`. */
object UUIDSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: UUID) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder): UUID = UUID.fromString(decoder.decodeString())
}

/** Point in the app's usual top-left-origin, Y-down coordinate space — the
 * direct equivalent of `CGPoint` everywhere this app uses page/image points. */
@Serializable
data class PointD(val x: Double, val y: Double) {
    companion object { val zero = PointD(0.0, 0.0) }
}

/** The equivalent of `CGSize`. */
@Serializable
data class SizeD(val width: Double, val height: Double) {
    companion object { val zero = SizeD(0.0, 0.0) }
}

/** The equivalent of `CGRect`, with the same min/mid/max accessors the Swift
 * layout/geometry code relies on throughout. */
@Serializable
data class RectD(val x: Double, val y: Double, val width: Double, val height: Double) {
    val minX: Double get() = x
    val minY: Double get() = y
    val maxX: Double get() = x + width
    val maxY: Double get() = y + height
    val midX: Double get() = x + width / 2
    val midY: Double get() = y + height / 2
    val origin: PointD get() = PointD(x, y)
    val size: SizeD get() = SizeD(width, height)

    companion object {
        val unitSquare = RectD(0.0, 0.0, 1.0, 1.0)
        fun of(origin: PointD, size: SizeD) = RectD(origin.x, origin.y, size.width, size.height)
    }
}
