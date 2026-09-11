@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.poster

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.a8000053398.printly.core.SizeD
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSizePreset
import com.a8000053398.printly.service.PhysicalMeasurementService
import com.a8000053398.printly.service.PosterTilingService
import com.a8000053398.printly.service.persistence.ExportHistoryStore
import com.a8000053398.printly.service.persistence.PosterSettingsStore
import com.a8000053398.printly.ui.components.IconBadge
import com.a8000053398.printly.ui.components.PrimaryButton
import com.a8000053398.printly.ui.components.SecondaryButton
import com.a8000053398.printly.ui.components.SectionHeaderText
import com.a8000053398.printly.ui.components.cardStyle
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.model.ExportedFileFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Self-contained "poster print" screen: pick one photo, choose a page size
 * and a columns x rows grid, and generate a multi-page PDF where each page
 * is one full-size tile of the enlarged image. Direct port of iOS's
 * `PosterTilingView`. */
@Composable
fun PosterTilingScreen(onClose: () -> Unit) {
    val settings = PosterSettingsStore.shared
    val pageSizePreset by settings.pageSizePreset.collectAsState()
    val columns by settings.columns.collectAsState()
    val rows by settings.rows.collectAsState()
    val overlapMM by settings.overlapMM.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScopeCompat()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPageSizeMenu by remember { mutableStateOf(false) }

    val pageSize = PageSize.standard(pageSizePreset)
    val overlapPoints = PhysicalMeasurementService.points(overlapMM, com.a8000053398.printly.model.MeasurementUnit.MILLIMETER)

    val posterSizeText = remember(columns, rows, pageSizePreset, overlapMM) {
        val poster = PosterTilingService.posterSize(columns, rows, pageSize.pointSize, overlapPoints)
        val widthIn = poster.width / PhysicalMeasurementService.POINTS_PER_INCH
        val heightIn = poster.height / PhysicalMeasurementService.POINTS_PER_INCH
        "≈ %.1f” × %.1f” assembled".format(widthIn, heightIn)
    }

    val pickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                sourceBitmap = BitmapFactory.decodeStream(stream)
                generatedFile = null
            }
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Poster Print") },
            navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(Metrics.spacingL.dp),
            verticalArrangement = Arrangement.spacedBy(Metrics.spacingXL.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().cardStyle().padding(Metrics.spacingL.dp),
                    verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
                ) {
                    SectionHeaderText("Photo")
                    val bitmap = sourceBitmap
                    val pickAction: () -> Unit = { pickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp)
                                .clickable(onClick = pickAction)
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = Metrics.spacingL.dp).clickable(onClick = pickAction),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)
                        ) {
                            IconBadge(Icons.Filled.PhotoLibrary, size = 44.dp)
                            Text("Choose a Photo", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().cardStyle().padding(Metrics.spacingL.dp),
                    verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
                ) {
                    SectionHeaderText("Page Size")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Page Size", fontWeight = FontWeight.Medium)
                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = { showPageSizeMenu = true }) { Text(pageSizePreset.displayName) }
                            DropdownMenu(expanded = showPageSizeMenu, onDismissRequest = { showPageSizeMenu = false }) {
                                for (preset in PageSizePreset.entries.filter { it != PageSizePreset.CUSTOM }) {
                                    DropdownMenuItem(text = { Text(preset.displayName) }, onClick = { showPageSizeMenu = false; settings.setPageSizePreset(preset) })
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().cardStyle().padding(Metrics.spacingL.dp),
                        verticalArrangement = Arrangement.spacedBy(Metrics.spacingL.dp)
                    ) {
                        SectionHeaderText("Grid")
                        StepperRow("Columns", columns, 1..6) { settings.setColumns(it) }
                        StepperRow("Rows", rows, 1..6) { settings.setRows(it) }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Overlap", fontWeight = FontWeight.Medium)
                                Text("${overlapMM.toInt()} mm", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                            Slider(value = overlapMM.toFloat(), onValueChange = { settings.setOverlapMM(it.toDouble()) }, valueRange = 0f..20f, steps = 19)
                        }
                    }
                    Text(
                        "Overlap is trimmed away when you tape adjacent pages together, so the image lines up. $posterSizeText",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                    PrimaryButton(
                        text = if (isGenerating) "Generating…" else "Generate PDF",
                        enabled = sourceBitmap != null && !isGenerating,
                        onClick = {
                            val bitmap = sourceBitmap ?: return@PrimaryButton
                            isGenerating = true
                            scope.launch {
                                val result = withContext(Dispatchers.Default) {
                                    runCatching {
                                        val data = PosterTilingService.generatePDF(bitmap, columns, rows, pageSize.pointSize, overlapPoints)
                                        val file = File(context.cacheDir, "Poster-${UUID.randomUUID()}.pdf")
                                        file.writeBytes(data)
                                        file
                                    }
                                }
                                isGenerating = false
                                result.onSuccess {
                                    generatedFile = it
                                    ExportHistoryStore.shared.record(listOf(it), "Poster Print", ExportedFileFormat.PDF)
                                }.onFailure { errorMessage = "Couldn't generate the poster PDF. Try a different grid size." }
                            }
                        }
                    )
                    if (generatedFile != null) {
                        SecondaryButton(text = "Share / Print", onClick = {
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", generatedFile!!)
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share"))
                        })
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Couldn't Generate Poster") },
            text = { Text(errorMessage ?: "") },
            confirmButton = { TextButton(onClick = { errorMessage = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun StepperRow(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text("$label: $value", fontWeight = FontWeight.Medium)
        Row {
            IconButton(onClick = { if (value > range.first) onChange(value - 1) }) { Text("−") }
            IconButton(onClick = { if (value < range.last) onChange(value + 1) }) { Text("+") }
        }
    }
}

@Composable
private fun rememberCoroutineScopeCompat() = androidx.compose.runtime.rememberCoroutineScope()

