package com.a8000053398.printly.service.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import com.a8000053398.printly.model.LayoutResult
import com.a8000053398.printly.model.PageBackground
import com.a8000053398.printly.service.PhysicalMeasurementService
import com.a8000053398.printly.util.PrintlyConstants
import java.io.ByteArrayOutputStream
import java.util.UUID

/** Renders [LayoutResult] pages to raster JPG/PNG bitmaps at print-quality
 * resolution, at a fixed DPI (pixels per inch) — the same exact physical
 * page/photo geometry as the PDF exporter, just baked into pixels. Draws
 * through the same [PageRenderer] so the two outputs can never diverge. */
object ImageExportService {

    class ImageUnavailableException(val photoID: UUID) : Exception("Image unavailable for $photoID")

    enum class Format(val displayName: String, val extension: String, val compressFormat: Bitmap.CompressFormat) {
        JPEG("JPG", "jpg", Bitmap.CompressFormat.JPEG),
        PNG("PNG", "png", Bitmap.CompressFormat.PNG)
    }

    private const val JPEG_QUALITY = 92

    fun exportImageData(
        pages: List<LayoutResult>,
        background: PageBackground = PageBackground.WHITE,
        format: Format,
        dpi: Double = PrintlyConstants.RECOMMENDED_DPI,
        showCutGuides: Boolean = false,
        imageProvider: (UUID) -> Bitmap?
    ): List<ByteArray> {
        if (pages.isEmpty()) return emptyList()

        val scale = dpi / PhysicalMeasurementService.POINTS_PER_INCH
        val results = mutableListOf<ByteArray>()
        var thrown: Exception? = null

        for (layout in pages) {
            val pixelWidth = (layout.pageSize.width * scale).toInt().coerceAtLeast(1)
            val pixelHeight = (layout.pageSize.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(pixelWidth, pixelHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            PageRenderer.fillBackground(background, pixelWidth.toFloat(), pixelHeight.toFloat(), canvas)

            canvas.save()
            canvas.scale(scale.toFloat(), scale.toFloat())
            for (placement in layout.placements) {
                val photoImage = imageProvider(placement.photoID)
                if (photoImage == null) {
                    thrown = ImageUnavailableException(placement.photoID)
                    continue
                }
                PageRenderer.draw(photoImage, placement, canvas)
            }
            if (showCutGuides) PageRenderer.drawCutGuides(layout.placements, canvas)
            canvas.restore()

            val output = ByteArrayOutputStream()
            bitmap.compress(format.compressFormat, JPEG_QUALITY, output)
            results.add(output.toByteArray())
        }

        thrown?.let { throw it }
        return results
    }
}
