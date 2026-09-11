package com.a8000053398.printly.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.a8000053398.printly.core.PointD
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/** Splits one large image across multiple printer-page-sized PDF pages (a
 * "poster print") — scales the image up to `columns x rows` pages, each page
 * overlapping its neighbors by [overlapPoints] so the printed sheets can be
 * trimmed and glued/taped back into one big image. */
object PosterTilingService {

    /** The full poster's size, in points, before slicing into pages. */
    fun posterSize(columns: Int, rows: Int, pageSize: SizeD, overlapPoints: Double): SizeD = SizeD(
        width = columns * pageSize.width - maxOf(0, columns - 1) * overlapPoints,
        height = rows * pageSize.height - maxOf(0, rows - 1) * overlapPoints
    )

    /** Top-left origin, within poster space, of every tile — row-major, `[row][column]`. */
    fun tileOrigins(columns: Int, rows: Int, pageSize: SizeD, overlapPoints: Double): List<List<PointD>> =
        (0 until rows).map { row ->
            (0 until columns).map { column ->
                PointD(column * (pageSize.width - overlapPoints), row * (pageSize.height - overlapPoints))
            }
        }

    /** The rect, within the source image's own aspect-fill scaling, that the
     * full poster is drawn into — scaled up (aspect fill) to cover [posterSize]
     * exactly, centered and cropped. */
    fun posterDrawRect(imageSize: SizeD, posterSize: SizeD): RectD {
        if (imageSize.width <= 0 || imageSize.height <= 0) return RectD(0.0, 0.0, posterSize.width, posterSize.height)
        val imageAspect = imageSize.width / imageSize.height
        val posterAspect = posterSize.width / posterSize.height
        var drawWidth = posterSize.width
        var drawHeight = posterSize.height
        if (imageAspect > posterAspect) drawWidth = posterSize.height * imageAspect else drawHeight = posterSize.width / imageAspect
        val originX = (posterSize.width - drawWidth) / 2
        val originY = (posterSize.height - drawHeight) / 2
        return RectD(originX, originY, drawWidth, drawHeight)
    }

    /** Renders a multi-page PDF, one page per tile, each at exactly [pageSize] —
     * printing every page at 100%/Actual Size and trimming along the overlap
     * reproduces the original image at full poster scale. */
    fun generatePDF(bitmap: Bitmap, columns: Int, rows: Int, pageSize: SizeD, overlapPoints: Double): ByteArray {
        require(columns > 0 && rows > 0)

        val poster = posterSize(columns, rows, pageSize, overlapPoints)
        val drawRect = posterDrawRect(SizeD(bitmap.width.toDouble(), bitmap.height.toDouble()), poster)
        val origins = tileOrigins(columns, rows, pageSize, overlapPoints)

        val pageWidth = pageSize.width.roundToInt().coerceAtLeast(1)
        val pageHeight = pageSize.height.roundToInt().coerceAtLeast(1)

        val document = PdfDocument()
        var pageNumber = 1
        for (row in origins) {
            for (tileOrigin in row) {
                val info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = document.startPage(info)
                val canvas = page.canvas
                canvas.save()
                canvas.clipRect(Rect(0, 0, pageWidth, pageHeight))
                canvas.translate(-tileOrigin.x.toFloat(), -tileOrigin.y.toFloat())
                val destRect = RectF(
                    drawRect.x.toFloat(), drawRect.y.toFloat(),
                    (drawRect.x + drawRect.width).toFloat(), (drawRect.y + drawRect.height).toFloat()
                )
                canvas.drawBitmap(bitmap, null, destRect, null)
                canvas.restore()
                document.finishPage(page)
                pageNumber++
            }
        }

        val output = ByteArrayOutputStream()
        document.writeTo(output)
        document.close()
        return output.toByteArray()
    }
}
