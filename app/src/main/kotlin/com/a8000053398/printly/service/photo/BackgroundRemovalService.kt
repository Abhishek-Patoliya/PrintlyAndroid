package com.a8000053398.printly.service.photo

import android.graphics.Bitmap
import android.graphics.Color
import com.a8000053398.printly.model.RGBColor
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions

/**
 * On-device background replacement for ID/passport photos, using ML Kit's
 * on-device Selfie Segmentation — the Android equivalent of iOS's Vision
 * `VNGeneratePersonSegmentationRequest`. Both produce a per-pixel foreground
 * confidence mask that is blended against a solid color exactly the same
 * way. Runs entirely on-device; only ever produces a *new* composited
 * bitmap — the caller's original is never touched.
 */
object BackgroundRemovalService {

    class NoPersonFoundException : Exception("No person detected in this photo")

    private val segmenter by lazy {
        Segmentation.getClient(
            SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .build()
        )
    }

    /** Replaces everything behind the primary person in [bitmap] with a solid
     * [color]. Foreground (person) pixels are preserved unmodified. Blocks
     * the calling thread — call from a background dispatcher. */
    fun replacingBackgroundSync(bitmap: Bitmap, color: RGBColor): Bitmap {
        val image = InputImage.fromBitmap(bitmap, 0)
        val mask = Tasks.await(segmenter.process(image))
        val buffer = mask.buffer
        val maskWidth = mask.width
        val maskHeight = mask.height
        buffer.rewind()

        val sourcePixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(sourcePixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val backgroundArgb = Color.argb(255, (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
        val outputPixels = IntArray(bitmap.width * bitmap.height)
        var anyForeground = false

        for (y in 0 until bitmap.height) {
            // The mask is produced at the input image's own size in SINGLE_IMAGE_MODE,
            // but guard with a nearest-sample scale in case dimensions ever differ.
            val maskY = if (maskHeight == bitmap.height) y else (y * maskHeight / bitmap.height)
            for (x in 0 until bitmap.width) {
                val maskX = if (maskWidth == bitmap.width) x else (x * maskWidth / bitmap.width)
                val confidence = buffer.getFloat(4 * (maskY * maskWidth + maskX))
                if (confidence > 0.5f) anyForeground = true
                val srcColor = sourcePixels[y * bitmap.width + x]
                outputPixels[y * bitmap.width + x] = blend(srcColor, backgroundArgb, confidence)
            }
        }
        buffer.rewind()

        if (!anyForeground) throw NoPersonFoundException()

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        output.setPixels(outputPixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return output
    }

    private fun blend(foreground: Int, background: Int, confidence: Float): Int {
        val a = confidence.coerceIn(0f, 1f)
        val inv = 1f - a
        val r = (Color.red(foreground) * a + Color.red(background) * inv).toInt()
        val g = (Color.green(foreground) * a + Color.green(background) * inv).toInt()
        val b = (Color.blue(foreground) * a + Color.blue(background) * inv).toInt()
        return Color.argb(255, r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
    }
}
