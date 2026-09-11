@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.export

import android.content.Intent
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.a8000053398.printly.service.pdf.ImageExportService
import com.a8000053398.printly.ui.components.PrimaryButton
import com.a8000053398.printly.ui.components.SecondaryButton
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.MeasurementFormatter
import com.a8000053398.printly.util.PrintlyConstants
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel

private enum class ExportFormat(val label: String) { PDF("PDF"), JPG("JPG"), PNG("PNG") }

/** Final export step: generates a print-ready PDF or print-quality JPG/PNG,
 * shows the "print at 100%" instruction for PDF, and hands off to
 * Print / Share / Save. Kotlin/Compose port of iOS's `ExportView`. */
@Composable
fun ExportScreen(viewModel: ProjectEditorViewModel, onClose: () -> Unit) {
    var format by remember { mutableStateOf(ExportFormat.PDF) }
    var exportDPI by remember { mutableStateOf(PrintlyConstants.RECOMMENDED_DPI) }
    val context = LocalContext.current
    var printUnavailableAlert by remember { mutableStateOf(false) }
    var savedToFilesConfirmation by remember { mutableStateOf(false) }
    var saveToFilesError by remember { mutableStateOf<String?>(null) }

    // Multi-page JPG/PNG exports need a whole folder (SAF can only create one
    // document per `CreateDocument` launch), so a tree pick stashes which
    // files/format to write once the user has chosen a destination folder.
    var pendingTreeSaveFiles by remember { mutableStateOf<List<java.io.File>>(emptyList()) }
    var pendingTreeSaveFormat by remember { mutableStateOf<ExportFormat?>(null) }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        if (uri != null) {
            val sourceFile = if (format == ExportFormat.PDF) viewModel.exportedPDFFile else viewModel.exportedImageFiles.firstOrNull()
            if (sourceFile != null) {
                val success = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out -> sourceFile.inputStream().use { it.copyTo(out) } }
                        ?: error("no output stream")
                }.isSuccess
                if (success) savedToFilesConfirmation = true else saveToFilesError = "Couldn't save the file to that location."
            }
        }
    }

    val openTreeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
        if (treeUri != null) {
            val fmt = pendingTreeSaveFormat
            val files = pendingTreeSaveFiles
            if (fmt != null && files.isNotEmpty()) {
                val success = runCatching {
                    val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
                    for (file in files) {
                        val newDocUri = DocumentsContract.createDocument(context.contentResolver, parentUri, mimeTypeFor(fmt), file.name)
                            ?: continue
                        context.contentResolver.openOutputStream(newDocUri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                    }
                }.isSuccess
                if (success) savedToFilesConfirmation = true else saveToFilesError = "Couldn't save the files to that folder."
            }
            pendingTreeSaveFiles = emptyList()
            pendingTreeSaveFormat = null
        }
    }

    fun saveToFiles() {
        val baseName = viewModel.project.name.trim().ifEmpty { "Printly" }
        if (format == ExportFormat.PDF) {
            createDocumentLauncher.launch("$baseName.${extensionFor(format)}")
        } else {
            val files = viewModel.exportedImageFiles
            if (files.size <= 1) {
                createDocumentLauncher.launch("$baseName.${extensionFor(format)}")
            } else {
                pendingTreeSaveFiles = files
                pendingTreeSaveFormat = format
                openTreeLauncher.launch(null)
            }
        }
    }

    val hasOutput = if (format == ExportFormat.PDF) viewModel.exportedPDFFile != null else viewModel.exportedImageFiles.isNotEmpty()

    fun regenerate() {
        viewModel.clearExportedFiles()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Export") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") } }
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingS.dp)) {
            ExportFormat.entries.forEachIndexed { index, f ->
                SegmentedButton(selected = format == f, onClick = { format = f; regenerate() }, shape = SegmentedButtonDefaults.itemShape(index, ExportFormat.entries.size)) { Text(f.label) }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 320.dp).padding(Metrics.spacingL.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when {
                format == ExportFormat.PDF && viewModel.exportedPDFFile != null -> {
                    Text("PDF ready — ${viewModel.exportedPDFFile?.name}", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                format != ExportFormat.PDF && viewModel.exportedImageFiles.isNotEmpty() -> {
                    val bitmap = remember(viewModel.exportedImageFiles.first()) { android.graphics.BitmapFactory.decodeFile(viewModel.exportedImageFiles.first().path) }
                    if (bitmap != null) {
                        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.heightIn(max = 260.dp))
                    }
                    if (viewModel.exportedImageFiles.size > 1) Text("+${viewModel.exportedImageFiles.size - 1} more pages", fontSize = 12.sp)
                }
                viewModel.isExporting -> {
                    androidx.compose.material3.CircularProgressIndicator()
                    Text("Generating ${format.label}…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> {
                    Text("Ready to generate your ${format.label} export", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }

        if (format == ExportFormat.PDF) {
            Text(
                PrintlyConstants.PRINT_INSTRUCTION_TEXT, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 13.sp
            )
        } else {
            Text(
                "Images are rendered at the DPI below for sharing or uploading. For a guaranteed exact-size print, use PDF.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp)
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingS.dp)) {
                val dpiOptions = listOf(150.0, 300.0, 600.0)
                dpiOptions.forEachIndexed { index, dpi ->
                    SegmentedButton(selected = exportDPI == dpi, onClick = { exportDPI = dpi; regenerate() }, shape = SegmentedButtonDefaults.itemShape(index, dpiOptions.size)) { Text("${dpi.toInt()} DPI") }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SummaryRow("Page Size", "${viewModel.project.pageSize.displayName} (${MeasurementFormatter.sizeString(viewModel.project.pageSize.physicalSize)})")
            SummaryRow("Photo Size", MeasurementFormatter.sizeString(viewModel.project.photoSize.physicalSize))
            SummaryRow("Total Copies", "${viewModel.totalPlacedCount} of ${viewModel.project.totalRequestedCopies} requested")
            SummaryRow("Pages", "${viewModel.totalPages}")
            if (viewModel.hasAnyLowResolutionPhoto) {
                Text("Some photos are low resolution and may look blurry when printed.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            if (!hasOutput) {
                PrimaryButton(
                    text = "Generate ${format.label}",
                    enabled = !viewModel.isExporting && viewModel.project.photos.isNotEmpty(),
                    onClick = {
                        when (format) {
                            ExportFormat.PDF -> viewModel.exportPDF(context.cacheDir)
                            ExportFormat.JPG -> viewModel.exportImages(context.cacheDir, ImageExportService.Format.JPEG, exportDPI)
                            ExportFormat.PNG -> viewModel.exportImages(context.cacheDir, ImageExportService.Format.PNG, exportDPI)
                        }
                    }
                )
                if (viewModel.project.photos.isEmpty()) Text("Add at least one photo before exporting.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                if (format == ExportFormat.PDF) {
                    PrimaryButton(text = "Print", onClick = {
                        val file = viewModel.exportedPDFFile ?: return@PrimaryButton
                        val printed = printPdf(context, file, viewModel.project.name)
                        if (!printed) printUnavailableAlert = true
                    })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                    SecondaryButton("Share", modifier = Modifier.weight(1f), onClick = { shareExport(context, viewModel, format) })
                    SecondaryButton("Save to Files", modifier = Modifier.weight(1f), onClick = { saveToFiles() })
                }
            }
        }
    }

    if (viewModel.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.errorMessage = null },
            title = { Text("Export Error") },
            text = { Text(viewModel.errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { viewModel.errorMessage = null }) { Text("OK") } }
        )
    }
    if (printUnavailableAlert) {
        AlertDialog(
            onDismissRequest = { printUnavailableAlert = false },
            title = { Text("Printing Unavailable") },
            text = { Text("This device can't print this file directly. Use Share instead to send it to a printing app.") },
            confirmButton = { TextButton(onClick = { printUnavailableAlert = false }) { Text("OK") } }
        )
    }
    if (savedToFilesConfirmation) {
        AlertDialog(
            onDismissRequest = { savedToFilesConfirmation = false },
            title = { Text("Saved to Files") },
            confirmButton = { TextButton(onClick = { savedToFilesConfirmation = false }) { Text("OK") } }
        )
    }
    if (saveToFilesError != null) {
        AlertDialog(
            onDismissRequest = { saveToFilesError = null },
            title = { Text("Couldn't Save") },
            text = { Text(saveToFilesError ?: "") },
            confirmButton = { TextButton(onClick = { saveToFilesError = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

private fun printPdf(context: android.content.Context, file: java.io.File, jobName: String): Boolean {
    val printManager = context.getSystemService(android.content.Context.PRINT_SERVICE) as? android.print.PrintManager ?: return false
    val adapter = object : android.print.PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: android.print.PrintAttributes?, newAttributes: android.print.PrintAttributes, cancellationSignal: android.os.CancellationSignal?, callback: LayoutResultCallback, extras: android.os.Bundle?) {
            if (cancellationSignal?.isCanceled == true) { callback.onLayoutCancelled(); return }
            val info = android.print.PrintDocumentInfo.Builder("$jobName.pdf").setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(pages: Array<out android.print.PageRange>?, destination: android.os.ParcelFileDescriptor, cancellationSignal: android.os.CancellationSignal?, callback: WriteResultCallback) {
            try {
                java.io.FileInputStream(file).use { input -> java.io.FileOutputStream(destination.fileDescriptor).use { output -> input.copyTo(output) } }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }
    }
    printManager.print(jobName, adapter, android.print.PrintAttributes.Builder().build())
    return true
}

private fun shareExport(context: android.content.Context, viewModel: ProjectEditorViewModel, format: ExportFormat) {
    val files = if (format == ExportFormat.PDF) listOfNotNull(viewModel.exportedPDFFile) else viewModel.exportedImageFiles
    if (files.isEmpty()) return
    val uris = files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }
    val intent = Intent(if (uris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND).apply {
        type = if (format == ExportFormat.PDF) "application/pdf" else "image/*"
        if (uris.size > 1) putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris)) else putExtra(Intent.EXTRA_STREAM, uris.first())
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share"))
}

private fun mimeTypeFor(format: ExportFormat): String = when (format) {
    ExportFormat.PDF -> "application/pdf"
    ExportFormat.JPG -> "image/jpeg"
    ExportFormat.PNG -> "image/png"
}

private fun extensionFor(format: ExportFormat): String = when (format) {
    ExportFormat.PDF -> "pdf"
    ExportFormat.JPG -> "jpg"
    ExportFormat.PNG -> "png"
}
