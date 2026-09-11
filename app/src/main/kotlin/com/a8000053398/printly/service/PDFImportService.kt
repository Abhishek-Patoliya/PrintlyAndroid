package com.a8000053398.printly.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.a8000053398.printly.util.PrintlyConstants

/** Imports an existing PDF file (e.g. a document, form, or scanned page) as
 * one photo per page, via [PdfRenderer] — the Android equivalent of iOS's
 * `PDFKit`-based import, so it can be arranged and printed through the exact
 * same layout/export pipeline as any other photo. */
object PDFImportService {

    class UnreadableDocumentException : Exception("Couldn't open this PDF")
    class NoPagesException : Exception("This PDF has no pages")

    /** Rasterizes up to [PrintlyConstants.MAX_PDF_IMPORT_PAGES] pages of the
     * PDF at [uri] into full-resolution bitmaps, one per page, in page order. */
    fun importPages(context: Context, uri: Uri, renderScale: Float = 3f): List<Bitmap> {
        val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw UnreadableDocumentException()

        descriptor.use { fd ->
            val renderer = runCatching { PdfRenderer(fd) }.getOrElse { throw UnreadableDocumentException() }
            renderer.use {
                if (it.pageCount <= 0) throw NoPagesException()
                val pageCount = minOf(it.pageCount, PrintlyConstants.MAX_PDF_IMPORT_PAGES)
                val images = mutableListOf<Bitmap>()
                for (index in 0 until pageCount) {
                    it.openPage(index).use { page ->
                        val width = (page.width * renderScale).toInt().coerceAtLeast(1)
                        val height = (page.height * renderScale).toInt().coerceAtLeast(1)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        images.add(bitmap)
                    }
                }
                return images
            }
        }
    }
}
