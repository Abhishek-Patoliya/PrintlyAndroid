package com.a8000053398.printly.service.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.a8000053398.printly.model.PageBackground
import com.a8000053398.printly.model.PlacedPhoto
import com.a8000053398.printly.service.layout.CutGuideGeometry

/** Draws one page's background and placements into a [Canvas], at whatever
 * point-to-pixel scale that canvas's own transform already establishes.
 * Shared by [PDFExportService] (vector PDF, 1 point = 1/72in, no extra scale)
 * and [ImageExportService] (rasterized JPG/PNG, with an added scale for the
 * target DPI) so both exports draw identically. */
object PageRenderer {
    fun fillBackground(background: PageBackground, width: Float, height: Float, canvas: Canvas) {
        canvas.drawRect(0f, 0f, width, height, Paint().apply { color = background.argb; style = Paint.Style.FILL })
    }

    fun draw(bitmap: Bitmap, placement: PlacedPhoto, canvas: Canvas) {
        canvas.save()
        canvas.translate(placement.center.x.toFloat(), placement.center.y.toFloat())
        if (placement.placementRotationDegrees != 0.0) {
            canvas.rotate(placement.placementRotationDegrees.toFloat())
        }
        val drawSize = placement.preRotationDrawSize
        val drawRect = RectF(
            -(drawSize.width / 2).toFloat(), -(drawSize.height / 2).toFloat(),
            (drawSize.width / 2).toFloat(), (drawSize.height / 2).toFloat()
        )
        canvas.drawBitmap(bitmap, null, drawRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    /** Print-shop style corner crop marks, using the same geometry the
     * on-screen canvas overlay uses. */
    fun drawCutGuides(placements: List<PlacedPhoto>, canvas: Canvas) {
        val segments = CutGuideGeometry.segments(placements)
        if (segments.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(255, 140, 140, 140)
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }
        for (segment in segments) {
            canvas.drawLine(segment.start.x.toFloat(), segment.start.y.toFloat(), segment.end.x.toFloat(), segment.end.y.toFloat(), paint)
        }
    }
}
