package com.a8000053398.printly.service.photo

import android.graphics.Bitmap
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * On-device face detection to assist ID-photo crop positioning — the
 * Android equivalent of iOS's Vision `VNDetectFaceRectanglesRequest`, using
 * ML Kit's on-device Face Detection API (no image data leaves the device).
 * Only ever informs where a good crop rectangle should sit; never modifies pixels.
 */
object FaceDetectionService {

    private val detector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build()
        )
    }

    /** The largest detected face's bounding box, in the app's usual top-left-
     * origin, unit-square coordinates (same convention as `PhotoItem.cropRect`).
     * Returns null if no face is found. Blocks the calling thread — call from
     * a background dispatcher. */
    fun detectFaceBoundingBox(bitmap: Bitmap): RectD? {
        val image = InputImage.fromBitmap(bitmap, 0)
        val faces = runCatching { Tasks.await(detector.process(image)) }.getOrNull() ?: return null
        val largest = faces.maxByOrNull { it.boundingBox.width().toLong() * it.boundingBox.height() } ?: return null

        val box = largest.boundingBox
        val w = bitmap.width.toDouble()
        val h = bitmap.height.toDouble()
        return RectD(
            x = (box.left / w).coerceIn(0.0, 1.0),
            y = (box.top / h).coerceIn(0.0, 1.0),
            width = (box.width() / w).coerceIn(0.0, 1.0),
            height = (box.height() / h).coerceIn(0.0, 1.0)
        )
    }
}
