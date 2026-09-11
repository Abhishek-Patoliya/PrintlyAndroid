package com.a8000053398.printly.service.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import com.a8000053398.printly.core.RectD
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.BorderStyle
import com.a8000053398.printly.model.OverlayPosition
import com.a8000053398.printly.model.PhotoBackgroundReplacement
import com.a8000053398.printly.model.PhotoFitMode
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PhotoTransform
import com.a8000053398.printly.model.TextOverlay
import com.a8000053398.printly.service.layout.ImageTransformMath
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Applies crop, rotation, flip, and brightness/contrast adjustments to a
 * source image, producing the exact pixel content drawn into a photo's
 * placement rect. Direct Kotlin port of the iOS `PhotoCropService`. */
object PhotoCropService {

    fun targetAspectRatio(photoSize: PhotoSize): Double {
        val size = photoSize.physicalSize
        if (size.height <= 0) return 1.0
        return size.width / size.height
    }

    /** Largest centered crop rect (unit-square 0...1) matching [aspectRatio]. */
    fun centeredCropRect(imageSize: SizeD, aspectRatio: Double): RectD {
        if (imageSize.width <= 0 || imageSize.height <= 0 || aspectRatio <= 0) return RectD.unitSquare
        val imageAspect = imageSize.width / imageSize.height

        var cropWidthFraction = 1.0
        var cropHeightFraction = 1.0
        if (imageAspect > aspectRatio) {
            cropHeightFraction = 1.0
            cropWidthFraction = aspectRatio / imageAspect
        } else {
            cropWidthFraction = 1.0
            cropHeightFraction = imageAspect / aspectRatio
        }
        val x = (1 - cropWidthFraction) / 2
        val y = (1 - cropHeightFraction) / 2
        return RectD(x, y, cropWidthFraction, cropHeightFraction)
    }

    fun zoomedCropRect(cropRect: RectD, factor: Double): RectD {
        if (factor <= 0 || !factor.isFinite()) return cropRect
        val minDimension = 0.02
        val newWidth = min(1.0, max(minDimension, cropRect.width / factor))
        val newHeight = min(1.0, max(minDimension, cropRect.height / factor))
        var newX = cropRect.midX - newWidth / 2
        var newY = cropRect.midY - newHeight / 2
        newX = min(max(0.0, newX), 1 - newWidth)
        newY = min(max(0.0, newY), 1 - newHeight)
        return RectD(newX, newY, newWidth, newHeight)
    }

    fun pannedCropRect(cropRect: RectD, deltaXFraction: Double, deltaYFraction: Double): RectD {
        var newX = cropRect.x + deltaXFraction * cropRect.width
        var newY = cropRect.y + deltaYFraction * cropRect.height
        newX = min(max(0.0, newX), 1 - cropRect.width)
        newY = min(max(0.0, newY), 1 - cropRect.height)
        return RectD(newX, newY, cropRect.width, cropRect.height)
    }

    /** Largest centered crop rect matching [aspectRatio] that also centers a
     * detected face with typical ID-photo headroom — an assistive heuristic,
     * not a guarantee of meeting any official authority's exact requirements. */
    fun faceCenteredCropRect(imageSize: SizeD, aspectRatio: Double, faceBoundingBox: RectD): RectD {
        if (imageSize.width <= 0 || imageSize.height <= 0 || aspectRatio <= 0 || faceBoundingBox.height <= 0) {
            return centeredCropRect(imageSize, aspectRatio)
        }
        val cropHeightFraction = min(1.0, max(0.05, faceBoundingBox.height * 2.2))
        val cropWidthFraction = min(1.0, cropHeightFraction * aspectRatio * (imageSize.height / imageSize.width))

        var originX = faceBoundingBox.midX - cropWidthFraction / 2
        var originY = faceBoundingBox.midY - cropHeightFraction * 0.45
        originX = min(max(0.0, originX), 1 - cropWidthFraction)
        originY = min(max(0.0, originY), 1 - cropHeightFraction)
        return RectD(originX, originY, cropWidthFraction, cropHeightFraction)
    }

    /** Crop + flip + (legacy whole-image) rotate + background-swap — without
     * color adjustment or the interactive transform, matching the iOS "base
     * image" used as CropSheet's live-gesture starting point. */
    fun renderBaseImage(
        bitmap: Bitmap,
        cropRect: RectD,
        rotationDegrees: Double,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        backgroundReplacement: PhotoBackgroundReplacement? = null
    ): Bitmap {
        val pixelCropX = (cropRect.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val pixelCropY = (cropRect.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val pixelCropW = (cropRect.width * bitmap.width).toInt().coerceIn(1, bitmap.width - pixelCropX)
        val pixelCropH = (cropRect.height * bitmap.height).toInt().coerceIn(1, bitmap.height - pixelCropY)

        var result = Bitmap.createBitmap(bitmap, pixelCropX, pixelCropY, pixelCropW, pixelCropH)

        if (flipHorizontal || flipVertical) {
            result = flipped(result, flipHorizontal, flipVertical)
        }
        if (rotationDegrees.mod(360.0) != 0.0) {
            result = rotated(result, rotationDegrees)
        }
        if (backgroundReplacement != null) {
            result = runCatching {
                BackgroundRemovalService.replacingBackgroundSync(result, backgroundReplacement.resolvedColor)
            }.getOrDefault(result)
        }
        return result
    }

    /** Full pipeline: base image, free rotate/zoom/pan transform, `.fit`
     * letterboxing, color adjustment, border, and text overlay — the exact
     * pixel content drawn on both the on-screen preview and export. */
    fun renderCroppedImage(
        bitmap: Bitmap,
        cropRect: RectD,
        rotationDegrees: Double,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        brightness: Double = 0.0,
        contrast: Double = 1.0,
        backgroundReplacement: PhotoBackgroundReplacement? = null,
        transform: PhotoTransform = PhotoTransform.identity,
        fitMode: PhotoFitMode = PhotoFitMode.FILL,
        border: BorderStyle? = null,
        textOverlay: TextOverlay? = null,
        targetPointSize: SizeD
    ): Bitmap {
        var result = renderBaseImage(bitmap, cropRect, rotationDegrees, flipHorizontal, flipVertical, backgroundReplacement)

        if (fitMode == PhotoFitMode.FILL) {
            result = applyTransform(result, transform)
        }
        if (fitMode == PhotoFitMode.FIT && targetPointSize.width > 0 && targetPointSize.height > 0) {
            result = letterboxed(result, targetPointSize.width / targetPointSize.height)
        }
        if (brightness != 0.0 || contrast != 1.0) {
            result = colorAdjusted(result, brightness, contrast)
        }
        if (border != null || (textOverlay != null && textOverlay.text.isNotEmpty())) {
            result = withOverlays(result, border, textOverlay, targetPointSize)
        }
        return result
    }

    /** Applies the interactive rotate/zoom/pan [transform] to an already
     * frame-shaped bitmap, redrawing it into a canvas of the *same* pixel
     * size so the frame's aspect ratio never changes. */
    fun applyTransform(bitmap: Bitmap, transform: PhotoTransform): Bitmap {
        if (transform == PhotoTransform.identity) return bitmap
        if (bitmap.width <= 0 || bitmap.height <= 0) return bitmap

        val canvasSize = SizeD(bitmap.width.toDouble(), bitmap.height.toDouble())
        val aspectRatio = canvasSize.width / canvasSize.height
        val radians = transform.rotationDegrees * Math.PI / 180
        val coverMultiplier = ImageTransformMath.coverMultiplier(aspectRatio, radians)
        val finalScale = (coverMultiplier * max(1.0, transform.zoom)).toFloat()

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val matrix = Matrix()
        matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
        matrix.postRotate(transform.rotationDegrees.toFloat())
        matrix.postScale(finalScale, finalScale)
        matrix.postTranslate(
            (canvasSize.width / 2 + transform.offsetXFraction * canvasSize.width).toFloat(),
            (canvasSize.height / 2 + transform.offsetYFraction * canvasSize.height).toFloat()
        )
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(bitmap, matrix, paint)
        return output
    }

    /** Pads [bitmap] with [fillColor] on whichever axis is needed so its aspect
     * ratio matches [targetAspect] exactly, without cropping any content. */
    private fun letterboxed(bitmap: Bitmap, targetAspect: Double, fillColor: Int = Color.WHITE): Bitmap {
        if (targetAspect <= 0 || bitmap.width <= 0 || bitmap.height <= 0) return bitmap
        val imageAspect = bitmap.width.toDouble() / bitmap.height
        if (abs(imageAspect - targetAspect) <= 0.001) return bitmap

        var canvasWidth = bitmap.width.toDouble()
        var canvasHeight = bitmap.height.toDouble()
        if (imageAspect > targetAspect) canvasHeight = bitmap.width / targetAspect else canvasWidth = bitmap.height * targetAspect

        val output = Bitmap.createBitmap(canvasWidth.toInt(), canvasHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(fillColor)
        val left = ((canvasWidth - bitmap.width) / 2).toFloat()
        val top = ((canvasHeight - bitmap.height) / 2).toFloat()
        canvas.drawBitmap(bitmap, left, top, null)
        return output
    }

    /** Draws an optional border stroke and text overlay on top of [bitmap],
     * scaling both from physical points to this bitmap's own pixel
     * resolution via the ratio between its size and [targetPointSize]. */
    private fun withOverlays(bitmap: Bitmap, border: BorderStyle?, textOverlay: TextOverlay?, targetPointSize: SizeD): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)
        val scale = if (targetPointSize.width > 0) bitmap.width / targetPointSize.width else 1.0

        if (border != null && border.widthPt > 0) {
            val lineWidth = (border.widthPt * scale).toFloat()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = lineWidth
                color = border.color.argb
            }
            val rect = RectF(lineWidth / 2, lineWidth / 2, bitmap.width - lineWidth / 2, bitmap.height - lineWidth / 2)
            canvas.drawRect(rect, paint)
        }

        if (textOverlay != null && textOverlay.text.isNotEmpty()) {
            val fontSize = max(4.0, textOverlay.fontSizePt * scale).toFloat()
            val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = fontSize
                color = Color.argb((textOverlay.opacity * 255).toInt().coerceIn(0, 255), (textOverlay.color.red * 255).toInt(), (textOverlay.color.green * 255).toInt(), (textOverlay.color.blue * 255).toInt())
                isFakeBoldText = true
            }
            val textWidth = textPaint.measureText(textOverlay.text)
            val fm = textPaint.fontMetrics
            val textHeight = fm.descent - fm.ascent
            val padding = fontSize * 0.35f

            var originX: Float
            var originY: Float
            when (textOverlay.position) {
                OverlayPosition.TOP_LEFT -> { originX = padding; originY = padding }
                OverlayPosition.TOP_CENTER -> { originX = (bitmap.width - textWidth) / 2; originY = padding }
                OverlayPosition.TOP_RIGHT -> { originX = bitmap.width - textWidth - padding; originY = padding }
                OverlayPosition.BOTTOM_LEFT -> { originX = padding; originY = bitmap.height - textHeight - padding }
                OverlayPosition.BOTTOM_CENTER -> { originX = (bitmap.width - textWidth) / 2; originY = bitmap.height - textHeight - padding }
                OverlayPosition.BOTTOM_RIGHT -> { originX = bitmap.width - textWidth - padding; originY = bitmap.height - textHeight - padding }
                OverlayPosition.CENTER -> { originX = (bitmap.width - textWidth) / 2; originY = (bitmap.height - textHeight) / 2 }
            }

            if (textOverlay.hasBackgroundPill) {
                val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb((0.38 * 255 * textOverlay.opacity).toInt().coerceIn(0, 255), 0, 0, 0)
                }
                val pillRect = RectF(
                    originX - padding * 0.6f, originY - padding * 0.4f,
                    originX + textWidth + padding * 0.6f, originY + textHeight + padding * 0.4f
                )
                canvas.drawRoundRect(pillRect, pillRect.height() / 2, pillRect.height() / 2, pillPaint)
            }

            canvas.drawText(textOverlay.text, originX, originY - fm.ascent, textPaint)
        }

        return output
    }

    /** Generates a plain solid-color placeholder bitmap — the blank canvas a
     * text label/sticker starts from before any crop/photo content exists. */
    fun blankImage(pixelSize: SizeD, color: Int = Color.WHITE): Bitmap {
        val bitmap = Bitmap.createBitmap(pixelSize.width.toInt().coerceAtLeast(1), pixelSize.height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(color)
        return bitmap
    }

    private fun flipped(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply { postScale(if (horizontal) -1f else 1f, if (vertical) -1f else 1f) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Applies brightness/contrast via [ColorMatrix], matching exactly what
     * the printed output will contain. Brightness in iOS's CIColorControls
     * terms is additive (roughly -1...1); contrast is multiplicative
     * (roughly 0.5...1.5) around mid-gray. */
    private fun colorAdjusted(bitmap: Bitmap, brightness: Double, contrast: Double): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val b = (brightness * 255).toFloat()
        val c = contrast.toFloat()
        val translate = (-0.5f * c + 0.5f) * 255f + b
        val matrix = ColorMatrix(
            floatArrayOf(
                c, 0f, 0f, 0f, translate,
                0f, c, 0f, 0f, translate,
                0f, 0f, c, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(matrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return output
    }

    private fun rotated(bitmap: Bitmap, degrees: Double): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
