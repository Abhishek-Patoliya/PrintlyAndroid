@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.editor.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.IDDocumentPresetCatalog
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PageOrientation
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSizePreset
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PhotoSizePreset
import com.a8000053398.printly.model.PhysicalSize
import com.a8000053398.printly.service.PhysicalMeasurementService
import com.a8000053398.printly.service.clampedToValidRange
import com.a8000053398.printly.service.persistence.RecentSizesStore
import com.a8000053398.printly.ui.components.SecondaryButton
import com.a8000053398.printly.ui.components.SectionHeaderText
import com.a8000053398.printly.ui.components.SizePresetCard
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.PrintlyConstants

@Composable
fun PageSizeSheet(pageSize: PageSize, onChange: (PageSize) -> Unit) {
    var customSize by remember { mutableStateOf(if (pageSize.preset == PageSizePreset.CUSTOM) pageSize.customSize else PhysicalSize(210.0, 297.0, MeasurementUnit.MILLIMETER)) }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingXL.dp)) {
        Text("Page Size", style = MaterialTheme.typography.titleMedium)

        Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            SectionHeaderText("Orientation")
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                PageOrientation.entries.forEachIndexed { index, orientation ->
                    SegmentedButton(
                        selected = pageSize.orientation == orientation,
                        onClick = { onChange(pageSize.copy(orientation = orientation)) },
                        shape = SegmentedButtonDefaults.itemShape(index, PageOrientation.entries.size)
                    ) { Text(orientation.displayName) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            SectionHeaderText("Page Size")
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightForGrid(PageSizePreset.entries.size), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp), horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                items(PageSizePreset.entries) { preset ->
                    val aspect = preset.basePortraitSize?.let { it.width / it.height } ?: (customSize.width / customSize.height.coerceAtLeast(1.0))
                    val subtitle = preset.basePortraitSize?.let { com.a8000053398.printly.util.MeasurementFormatter.sizeString(it) } ?: com.a8000053398.printly.util.MeasurementFormatter.sizeString(customSize)
                    SizePresetCard(preset.displayName, subtitle, aspect, pageSize.preset == preset) {
                        onChange(pageSize.copy(preset = preset, customSize = if (preset == PageSizePreset.CUSTOM) customSize else pageSize.customSize))
                    }
                }
            }
        }

        if (pageSize.preset == PageSizePreset.CUSTOM) {
            Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SectionHeaderText("Custom Dimensions")
                CustomSizeEditor(customSize) { updated ->
                    customSize = updated
                    onChange(pageSize.copy(customSize = updated))
                }
            }
        }
    }
}

@Composable
fun PhotoSizeSheet(photoSize: PhotoSize, onChange: (PhotoSize) -> Unit) {
    var customSize by remember { mutableStateOf(if (photoSize.preset == PhotoSizePreset.CUSTOM) photoSize.customSize else PhysicalSize(2.0, 2.0, MeasurementUnit.INCH)) }
    var isShowingIDCatalog by remember { mutableStateOf(false) }
    val recentSizes by RecentSizesStore.shared.recentPhotoSizes.collectAsState()

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingXL.dp)) {
        Text("Photo Size", style = MaterialTheme.typography.titleMedium)

        if (recentSizes.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SectionHeaderText("Recent")
                LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightForGrid(recentSizes.size), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp), horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                    items(recentSizes) { recent ->
                        val physical = recent.physicalSize
                        SizePresetCard(recent.displayName, com.a8000053398.printly.util.MeasurementFormatter.sizeString(physical), physical.width / physical.height.coerceAtLeast(1.0), photoSize == recent) {
                            onChange(recent)
                            if (recent.preset == PhotoSizePreset.CUSTOM) customSize = recent.customSize
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            SectionHeaderText("Photo Size")
            LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.heightForGrid(PhotoSizePreset.entries.size), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp), horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                items(PhotoSizePreset.entries) { preset ->
                    val aspect = preset.baseSize?.let { it.width / it.height } ?: (customSize.width / customSize.height.coerceAtLeast(1.0))
                    val subtitle = preset.baseSize?.let { com.a8000053398.printly.util.MeasurementFormatter.sizeString(it) } ?: com.a8000053398.printly.util.MeasurementFormatter.sizeString(customSize)
                    SizePresetCard(preset.displayName, subtitle, aspect, photoSize.preset == preset) {
                        onChange(photoSize.copy(preset = preset, customSize = if (preset == PhotoSizePreset.CUSTOM) customSize else photoSize.customSize))
                    }
                }
            }
            SecondaryButton("Browse ID & Passport Sizes by Country", onClick = { isShowingIDCatalog = true })
        }

        if (photoSize.preset == PhotoSizePreset.CUSTOM) {
            Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SectionHeaderText("Custom Dimensions")
                CustomSizeEditor(customSize) { updated ->
                    customSize = updated
                    onChange(photoSize.copy(customSize = updated))
                }
            }
        }
    }

    if (isShowingIDCatalog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isShowingIDCatalog = false }) {
            IDDocumentPresetPicker(onSelect = { preset ->
                customSize = preset.size
                onChange(PhotoSize(PhotoSizePreset.CUSTOM, preset.size))
                RecentSizesStore.shared.record(PhotoSize(PhotoSizePreset.CUSTOM, preset.size))
                isShowingIDCatalog = false
            }, onClose = { isShowingIDCatalog = false })
        }
    }
}

@Composable
fun CustomSizeEditor(size: PhysicalSize, onChange: (PhysicalSize) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp),
        verticalArrangement = Arrangement.spacedBy(Metrics.spacingL.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            MeasurementUnit.entries.forEachIndexed { index, unit ->
                SegmentedButton(
                    selected = size.unit == unit,
                    onClick = {
                        val w = PhysicalMeasurementService.convert(size.width, size.unit, unit)
                        val h = PhysicalMeasurementService.convert(size.height, size.unit, unit)
                        onChange(PhysicalSize(w, h, unit))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, MeasurementUnit.entries.size)
                ) { Text(unit.displayName) }
            }
        }

        DimensionField("Width", size.width, size.unit) { onChange(size.copy(width = it)) }
        HorizontalDivider()
        DimensionField("Height", size.height, size.unit) { onChange(size.copy(height = it)) }

        SecondaryButton("Swap Width & Height", onClick = { onChange(PhysicalSize(size.height, size.width, size.unit)) })
    }
}

@Composable
private fun DimensionField(label: String, value: Double, unit: MeasurementUnit, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value == value.toLong().toDouble()) value.toLong().toString() else "%.2f".format(value)) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label)
        Row {
            TextField(
                value = text,
                onValueChange = { newText ->
                    text = newText
                    newText.toDoubleOrNull()?.let { onChange(it) }
                },
                modifier = Modifier.width(90.dp),
                singleLine = true
            )
            Text(unit.displayName, modifier = Modifier.padding(start = 4.dp, top = 14.dp))
        }
    }
}

@Composable
fun IDDocumentPresetPicker(onSelect: (com.a8000053398.printly.model.IDDocumentPreset) -> Unit, onClose: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query) { IDDocumentPresetCatalog.matching(query) }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp)) {
        Text("ID & Passport Sizes", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Metrics.spacingM.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Search country or document") }, modifier = Modifier.fillMaxWidth())
        LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = Metrics.spacingM.dp).heightForGrid(results.size, itemHeight = 56)) {
            items(results, key = { it.id }) { preset ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickableRow { onSelect(preset) },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(preset.country, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                        Text(preset.documentName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(com.a8000053398.printly.util.MeasurementFormatter.sizeString(preset.size), color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                }
            }
        }
        Text(
            "Sizes shown are common defaults, not guaranteed to meet a specific country's or authority's current official requirements. Verify before submitting.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = Metrics.spacingM.dp)
        )
        SecondaryButton("Close", onClick = onClose, modifier = Modifier.padding(top = Metrics.spacingM.dp))
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)

private fun Modifier.heightForGrid(itemCount: Int, itemHeight: Int = 90): Modifier {
    val rows = ((itemCount + 1) / 2).coerceAtLeast(1)
    return this.height((rows * itemHeight).dp)
}
