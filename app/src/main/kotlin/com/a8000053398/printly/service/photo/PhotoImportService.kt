package com.a8000053398.printly.service.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.InputStream

/** Loads full-resolution image data from content [Uri]s picked via the system
 * Photo Picker — the Android equivalent of iOS's `PhotosPicker` loading. */
object PhotoImportService {

    /** Loads each picked item, preserving selection order. Failures for
     * individual items are skipped rather than failing the whole batch. */
    fun loadImages(context: Context, uris: List<Uri>): List<Bitmap> =
        uris.mapNotNull { runCatching { loadImage(context, it) }.getOrNull() }

    fun loadImage(context: Context, uri: Uri): Bitmap {
        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        } ?: throw IllegalArgumentException("Unsupported image data")
        return fixOrientation(context, uri, bitmap)
    }

    /** Redraws the bitmap with any EXIF rotation baked in, so downstream
     * cropping/layout math can ignore orientation — matches iOS's
     * `UIImage.fixedOrientation()`. */
    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = readExifOrientation(context, uri) ?: return bitmap
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int? {
        val stream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        return stream.use {
            runCatching { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }.getOrNull()
        }
    }
}
