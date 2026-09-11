package com.a8000053398.printly.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.a8000053398.printly.core.SizeD
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Generates QR codes and Code 128 barcodes as flat bitmaps that can be
 * dropped into a label slot exactly like any other photo — the Android
 * equivalent of iOS's `CIQRCodeGenerator`/`CICode128BarcodeGenerator`, using
 * ZXing (fully offline, no network). */
object CodeGeneratorService {
    enum class CodeType(val displayName: String, val iconName: String) {
        QR("QR Code", "qrcode"),
        BARCODE("Barcode (Code 128)", "barcode")
    }

    class EmptyTextException : Exception("Text is empty")
    class RenderingFailedException : Exception("Rendering failed")

    /** Renders [text] as a QR code or barcode, composited on a white canvas
     * of exactly [pixelSize] with a small quiet-zone margin. */
    fun generate(text: String, type: CodeType, pixelSize: SizeD): Bitmap {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw EmptyTextException()

        val margin = 0.12
        val availableWidth = (pixelSize.width * (1 - margin * 2)).toInt().coerceAtLeast(1)
        val availableHeight = (pixelSize.height * (1 - margin * 2)).toInt().coerceAtLeast(1)

        val format = if (type == CodeType.QR) BarcodeFormat.QR_CODE else BarcodeFormat.CODE_128
        val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0)

        val matrix: BitMatrix = runCatching {
            MultiFormatWriter().encode(trimmed, format, availableWidth, availableHeight, hints)
        }.getOrElse { throw RenderingFailedException() }

        val codeBitmap = matrixToBitmap(matrix)

        val output = Bitmap.createBitmap(pixelSize.width.toInt().coerceAtLeast(1), pixelSize.height.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(Color.WHITE)
        val left = (output.width - codeBitmap.width) / 2f
        val top = (output.height - codeBitmap.height) / 2f
        canvas.drawBitmap(codeBitmap, left, top, Paint())
        return output
    }

    private fun matrixToBitmap(matrix: BitMatrix): Bitmap {
        val bitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                bitmap.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
