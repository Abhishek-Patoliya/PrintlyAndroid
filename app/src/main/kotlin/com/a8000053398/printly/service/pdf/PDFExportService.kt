package com.a8000053398.printly.service.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import com.a8000053398.printly.model.LayoutResult
import com.a8000053398.printly.model.PageBackground
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Renders a list of [LayoutResult] pages to a print-ready, multi-page PDF
 * with exact physical dimensions, via [PdfDocument] — the Android equivalent
 * of iOS's `UIGraphicsPDFRenderer`. `PdfDocument.PageInfo` is already
 * specified in points (1/72in), the same unit this app's whole layout
 * pipeline uses, so a page produced for A4 is exactly 595 x 842pt and
 * printing it at 100%/Actual Size reproduces every measurement precisely.
 */
object PDFExportService {

    class ImageUnavailableException(val photoID: UUID) : Exception("Image unavailable for $photoID")

    fun exportPDF(
        pages: List<LayoutResult>,
        background: PageBackground = PageBackground.WHITE,
        showCutGuides: Boolean = false,
        imageProvider: (UUID) -> Bitmap?
    ): ByteArray {
        require(pages.isNotEmpty()) { "No pages to export" }

        val document = PdfDocument()
        var thrown: Exception? = null

        for ((index, layout) in pages.withIndex()) {
            val width = layout.pageSize.width.roundToInt().coerceAtLeast(1)
            val height = layout.pageSize.height.roundToInt().coerceAtLeast(1)
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            PageRenderer.fillBackground(background, width.toFloat(), height.toFloat(), canvas)

            for (placement in layout.placements) {
                val bitmap = imageProvider(placement.photoID)
                if (bitmap == null) {
                    thrown = ImageUnavailableException(placement.photoID)
                    continue
                }
                PageRenderer.draw(bitmap, placement, canvas)
            }

            if (showCutGuides) PageRenderer.drawCutGuides(layout.placements, canvas)

            document.finishPage(page)
        }

        thrown?.let { document.close(); throw it }

        val outputStream = java.io.ByteArrayOutputStream()
        document.writeTo(outputStream)
        document.close()
        return outputStream.toByteArray()
    }

    fun exportPDFToFile(
        pages: List<LayoutResult>,
        destination: File,
        background: PageBackground = PageBackground.WHITE,
        showCutGuides: Boolean = false,
        imageProvider: (UUID) -> Bitmap?
    ) {
        val data = exportPDF(pages, background, showCutGuides, imageProvider)
        FileOutputStream(destination).use { it.write(data) }
    }
}
