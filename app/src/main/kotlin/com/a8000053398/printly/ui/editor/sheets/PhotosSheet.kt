package com.a8000053398.printly.ui.editor.sheets

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.a8000053398.printly.model.PhotoItem
import com.a8000053398.printly.ui.components.IconBadge
import com.a8000053398.printly.ui.theme.IconMapping
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.PrintlyConstants
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import java.util.UUID

/** Bottom sheet for managing the source photo list: add, delete, duplicate,
 * and reorder (long-press the drag handle). Kotlin/Compose port of iOS's
 * `PhotosSheet` — reordering is hold-the-handle-and-drag rather than iOS's
 * Edit-mode list reordering, the more common Android pattern for this. */
@Composable
fun PhotosSheet(viewModel: ProjectEditorViewModel, onAddLabel: () -> Unit) {
    val context = LocalContext.current
    var pdfImportError by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        if (uris.isNotEmpty()) viewModel.importPhotos(context, uris)
    }
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri.value
        if (success && uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)?.let { viewModel.addPhoto(it); viewModel.recomputeLayout() }
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.importPDF(context, uri) { error -> pdfImportError = error }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Photos", style = MaterialTheme.typography.titleMedium)

        SourceRow("photo.on.rectangle.fill", "Choose from Library") { pickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        SourceRow("camera.fill", "Take Photo") {
            val uri = createTempImageUri(context)
            cameraUri.value = uri
            cameraLauncher.launch(uri)
        }
        SourceRow("textformat", "Add Text Label", onAddLabel)
        SourceRow("doc.richtext", "Import PDF") { pdfLauncher.launch(arrayOf("application/pdf")) }

        HorizontalDivider(modifier = Modifier.padding(vertical = Metrics.spacingS.dp))

        if (viewModel.project.photos.isEmpty()) {
            Text("No Photos Yet — add photos from your library or camera to start arranging your page.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        } else {
            Text("Photos (${viewModel.project.photos.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Hold the handle to reorder.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            ReorderablePhotosList(viewModel)
        }

        if (viewModel.isImportingPhotos) {
            Text("Importing…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (pdfImportError != null) {
        AlertDialog(
            onDismissRequest = { pdfImportError = null },
            title = { Text("Couldn't Import PDF") },
            text = { Text(pdfImportError ?: "") },
            confirmButton = { TextButton(onClick = { pdfImportError = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun SourceRow(iconName: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
    ) {
        IconBadge(IconMapping.icon(iconName), size = 34.dp)
        Text(title, fontWeight = FontWeight.Medium)
    }
}

/** A manually-driven reorderable list: dragging a row's handle accumulates a
 * pixel offset and, each time it crosses half a row's height, swaps that row
 * with its neighbor via `viewModel.movePhotos` and compensates the offset by
 * one row height — the standard technique for a reorderable `LazyColumn`
 * without a third-party dependency. */
@Composable
private fun ReorderablePhotosList(viewModel: ProjectEditorViewModel) {
    val sortedPhotos = viewModel.project.photos.sortedBy { it.sortIndex }
    var draggedID by remember { mutableStateOf<UUID?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    LazyColumn(modifier = Modifier.fillMaxWidth().height((sortedPhotos.size * 76).coerceAtMost(420).dp)) {
        itemsIndexed(sortedPhotos, key = { _, item -> item.id }) { _, photo ->
            val isDragged = draggedID == photo.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { if (rowHeightPx <= 0f) rowHeightPx = it.size.height.toFloat() }
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffsetPx else 0f }
                    .then(if (isDragged) Modifier.shadow(4.dp) else Modifier)
                    .background(if (isDragged) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
            ) {
                val bitmap = remember(photo.id, viewModel.project) { viewModel.resolvedImage(photo) }
                if (bitmap != null) {
                    Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(Metrics.radiusSmall.dp)))
                } else {
                    Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(Metrics.radiusSmall.dp)).background(MaterialTheme.colorScheme.surfaceVariant))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("${photo.copies} ${if (photo.copies == 1) "copy" else "copies"}", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.setCopies((photo.copies - 1).coerceAtLeast(0), photo) }) { Text("−") }
                        IconButton(onClick = { viewModel.setCopies((photo.copies + 1).coerceAtMost(PrintlyConstants.MAX_COPIES), photo) }) { Text("+") }
                    }
                    viewModel.qualityWarning(photo)?.let {
                        Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                    }
                }
                IconButton(onClick = { viewModel.duplicatePhoto(photo) }) {
                    Icon(IconMapping.icon("plus.square.on.square"), contentDescription = "Duplicate", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.deletePhoto(photo) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
                Icon(
                    Icons.Filled.DragHandle,
                    contentDescription = "Reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .pointerInput(photo.id) {
                            var localIndex = -1
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedID = photo.id
                                    dragOffsetPx = 0f
                                    localIndex = viewModel.project.photos.sortedBy { it.sortIndex }.indexOfFirst { it.id == photo.id }
                                },
                                onDragEnd = { draggedID = null; dragOffsetPx = 0f },
                                onDragCancel = { draggedID = null; dragOffsetPx = 0f }
                            ) { change, dragAmount ->
                                change.consume()
                                dragOffsetPx += dragAmount.y
                                val rowHeight = rowHeightPx
                                if (rowHeight <= 0f || localIndex < 0) return@detectDragGesturesAfterLongPress
                                val current = viewModel.project.photos
                                if (dragOffsetPx > rowHeight / 2 && localIndex < current.size - 1) {
                                    viewModel.movePhotos(localIndex, localIndex + 1)
                                    localIndex += 1
                                    dragOffsetPx -= rowHeight
                                } else if (dragOffsetPx < -rowHeight / 2 && localIndex > 0) {
                                    viewModel.movePhotos(localIndex, localIndex - 1)
                                    localIndex -= 1
                                    dragOffsetPx += rowHeight
                                }
                            }
                        }
                )
            }
        }
    }
}

private fun createTempImageUri(context: android.content.Context): Uri {
    val file = java.io.File.createTempFile("camera_", ".jpg", java.io.File(context.cacheDir, "camera").apply { mkdirs() })
    return androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
