@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.editor.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.PageBackground
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.service.PhysicalMeasurementService
import com.a8000053398.printly.ui.components.MiniLayoutPreview
import com.a8000053398.printly.ui.components.layoutPreviewPlacements
import com.a8000053398.printly.ui.theme.IconMapping
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.PrintlyConstants
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import java.util.UUID

@Composable
fun CopiesSheet(viewModel: ProjectEditorViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Copies", style = MaterialTheme.typography.titleMedium)
        if (viewModel.project.photos.isEmpty()) {
            Text("No Photos — add photos first to set copies.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Total Copies", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${viewModel.totalPlacedCount} of ${viewModel.project.totalRequestedCopies}", style = MaterialTheme.typography.headlineSmall)
                }
                if (viewModel.layoutError == null) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF34A853))
            }
            HorizontalDivider()
            for (photo in viewModel.project.photos.sortedBy { it.sortIndex }) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${photo.copies} ${if (photo.copies == 1) "copy" else "copies"}", fontWeight = FontWeight.Medium)
                    Row {
                        androidx.compose.material3.IconButton(onClick = { viewModel.setCopies((photo.copies - 1).coerceAtLeast(0), photo) }) { Text("−") }
                        androidx.compose.material3.IconButton(onClick = { viewModel.setCopies((photo.copies + 1).coerceAtMost(PrintlyConstants.MAX_COPIES), photo) }) { Text("+") }
                    }
                }
            }
        }
    }
}

@Composable
fun SpacingSheet(viewModel: ProjectEditorViewModel) {
    val unit = viewModel.project.preferredUnit
    var horizontal by remember { mutableStateOf(PhysicalMeasurementService.value(viewModel.project.spacing.horizontal, unit)) }
    var vertical by remember { mutableStateOf(PhysicalMeasurementService.value(viewModel.project.spacing.vertical, unit)) }
    var linked by remember { mutableStateOf(kotlin.math.abs(horizontal - vertical) < 0.001) }
    val range = when (unit) {
        com.a8000053398.printly.model.MeasurementUnit.MILLIMETER -> 0.0..50.0
        com.a8000053398.printly.model.MeasurementUnit.CENTIMETER -> 0.0..5.0
        com.a8000053398.printly.model.MeasurementUnit.INCH -> 0.0..2.0
    }

    fun apply() {
        val h = PhysicalMeasurementService.points(horizontal, unit)
        val v = PhysicalMeasurementService.points(if (linked) horizontal else vertical, unit)
        viewModel.setSpacing(PageSpacing(h, v))
    }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Spacing", style = MaterialTheme.typography.titleMedium)
        val (pageSize, placements) = layoutPreviewPlacements(viewModel.project.pageSize, viewModel.project.margins, viewModel.project.photoSize, PageSpacing(PhysicalMeasurementService.points(horizontal, unit), PhysicalMeasurementService.points(if (linked) horizontal else vertical, unit)), count = viewModel.project.totalRequestedCopies.coerceIn(4, 9), fillPageEdgeToEdge = viewModel.project.fillPageEdgeToEdge)
        MiniLayoutPreview(pageSize, placements, modifier = Modifier.fillMaxWidth().androidx_height(130))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Link Horizontal & Vertical")
            Switch(checked = linked, onCheckedChange = { linked = it; apply() })
        }
        SliderField("Horizontal Spacing", horizontal, range, unit.displayName) { horizontal = it; if (linked) vertical = it; apply() }
        if (!linked) SliderField("Vertical Spacing", vertical, range, unit.displayName) { vertical = it; apply() }
    }
}

@Composable
fun MarginsSheet(viewModel: ProjectEditorViewModel) {
    val unit = viewModel.project.preferredUnit
    val m = viewModel.project.margins
    var top by remember { mutableStateOf(PhysicalMeasurementService.value(m.top, unit)) }
    var bottom by remember { mutableStateOf(PhysicalMeasurementService.value(m.bottom, unit)) }
    var left by remember { mutableStateOf(PhysicalMeasurementService.value(m.left, unit)) }
    var right by remember { mutableStateOf(PhysicalMeasurementService.value(m.right, unit)) }
    var linked by remember { mutableStateOf(listOf(bottom, left, right).all { kotlin.math.abs(it - top) < 0.001 }) }
    val range = when (unit) {
        com.a8000053398.printly.model.MeasurementUnit.MILLIMETER -> 0.0..50.0
        com.a8000053398.printly.model.MeasurementUnit.CENTIMETER -> 0.0..5.0
        com.a8000053398.printly.model.MeasurementUnit.INCH -> 0.0..2.0
    }

    fun apply() {
        viewModel.setMargins(
            PageMargins(
                top = PhysicalMeasurementService.points(top, unit),
                bottom = PhysicalMeasurementService.points(if (linked) top else bottom, unit),
                left = PhysicalMeasurementService.points(if (linked) top else left, unit),
                right = PhysicalMeasurementService.points(if (linked) top else right, unit)
            )
        )
    }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Margins", style = MaterialTheme.typography.titleMedium)
        val previewMargins = PageMargins(PhysicalMeasurementService.points(top, unit), PhysicalMeasurementService.points(if (linked) top else bottom, unit), PhysicalMeasurementService.points(if (linked) top else left, unit), PhysicalMeasurementService.points(if (linked) top else right, unit))
        val (pageSize, placements) = layoutPreviewPlacements(viewModel.project.pageSize, previewMargins, viewModel.project.photoSize, viewModel.project.spacing, count = viewModel.project.totalRequestedCopies.coerceAtLeast(1), fillPageEdgeToEdge = viewModel.project.fillPageEdgeToEdge)
        MiniLayoutPreview(pageSize, placements, modifier = Modifier.fillMaxWidth().androidx_height(130))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("All Sides Equal")
            Switch(checked = linked, onCheckedChange = { linked = it; apply() })
        }
        SliderField(if (linked) "All Sides" else "Top", top, range, unit.displayName) { top = it; apply() }
        if (!linked) {
            SliderField("Bottom", bottom, range, unit.displayName) { bottom = it; apply() }
            SliderField("Left", left, range, unit.displayName) { left = it; apply() }
            SliderField("Right", right, range, unit.displayName) { right = it; apply() }
        }
    }
}

@Composable
private fun SliderField(label: String, value: Double, range: ClosedFloatingPointRange<Double>, unitLabel: String, onChange: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("%.1f %s".format(value, unitLabel), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toDouble()) }, valueRange = range.start.toFloat()..range.endInclusive.toFloat())
    }
}

@Composable
fun BackgroundColorSheet(viewModel: ProjectEditorViewModel) {
    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Background", style = MaterialTheme.typography.titleMedium)
        for (background in PageBackground.entries) {
            val isSelected = viewModel.project.backgroundColor == background
            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.setBackgroundColor(background) }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(background.color)
                        .then(if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                )
                Text(background.displayName, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                if (isSelected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Text("The background color prints exactly as shown, including on transparent-photo areas.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class LayoutMode { AUTO, GRID, FILL_PAGE }

@Composable
fun ArrangeSheet(viewModel: ProjectEditorViewModel) {
    val mode = when {
        viewModel.project.fillPageEdgeToEdge -> LayoutMode.FILL_PAGE
        viewModel.project.allowRotationToFit -> LayoutMode.AUTO
        else -> LayoutMode.GRID
    }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingL.dp)) {
        Text("Arrange", style = MaterialTheme.typography.titleMedium)

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LayoutMode.entries.forEachIndexed { index, m ->
                SegmentedButton(
                    selected = mode == m,
                    onClick = {
                        when (m) {
                            LayoutMode.AUTO -> { viewModel.setFillPageEdgeToEdge(false); viewModel.setAllowRotationToFit(true) }
                            LayoutMode.GRID -> { viewModel.setFillPageEdgeToEdge(false); viewModel.setAllowRotationToFit(false) }
                            LayoutMode.FILL_PAGE -> viewModel.setFillPageEdgeToEdge(true)
                        }
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, LayoutMode.entries.size)
                ) { Text(if (m == LayoutMode.AUTO) "Auto Arrange" else if (m == LayoutMode.GRID) "Grid" else "Fill Page") }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabeledStat("Columns × Rows", "${viewModel.layout.columns} × ${viewModel.layout.rows}")
            LabeledStat("Fits per Page", "${viewModel.layout.maxCapacity}")
            LabeledStat("Placed", "${viewModel.totalPlacedCount} of ${viewModel.project.totalRequestedCopies}")
            if (viewModel.totalPages > 1) LabeledStat("Pages", "${viewModel.totalPages}")
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Show Cut Guides")
            Switch(checked = viewModel.project.showCutGuides, onCheckedChange = { viewModel.setShowCutGuides(it) })
        }

        if (viewModel.selectedPlacementID != null) {
            val id = viewModel.selectedPlacementID!!
            Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.centerPlacement(id) }.padding(vertical = 8.dp)) {
                Text("Center on Page", color = MaterialTheme.colorScheme.primary)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.numberPhotosForContactSheet() }.padding(vertical = 8.dp)) {
            Text("Number Photos (Contact Sheet Style)", color = MaterialTheme.colorScheme.primary)
        }
        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.clearContactSheetNumbers() }.padding(vertical = 8.dp)) {
            Text("Clear Numbers", color = MaterialTheme.colorScheme.error)
        }
        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.resetManualArrangement() }.padding(vertical = 8.dp)) {
            Text("Reset Layout", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun LabeledStat(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

enum class MoreTool(val label: String, val iconName: String) {
    ARRANGE("Arrange & Align", "square.grid.2x2"),
    PAGE_SETTINGS("Page Settings", "doc"),
    BACKGROUND("Background", "paintpalette"),
    SAVE_TEMPLATE("Save as Template", "square.and.arrow.down"),
    SELECT_MULTIPLE("Select Multiple", "checkmark.circle")
}

@Composable
fun MoreToolsSheet(hasPlacements: Boolean, onSelect: (MoreTool) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)) {
        Text("More Tools", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Metrics.spacingS.dp))
        for (tool in MoreTool.entries) {
            val disabled = tool == MoreTool.SELECT_MULTIPLE && !hasPlacements
            Row(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !disabled) { onSelect(tool) }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
            ) {
                com.a8000053398.printly.ui.components.IconBadge(IconMapping.icon(tool.iconName), size = 34.dp, tint = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                Text(tool.label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), color = if (disabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (!disabled) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ExactMeasurementSheet(viewModel: ProjectEditorViewModel, placementID: UUID) {
    val unit = viewModel.project.preferredUnit
    val placement = viewModel.layout.placements.firstOrNull { it.id == placementID }

    Column(modifier = Modifier.fillMaxWidth().padding(Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Text("Exact Position", style = MaterialTheme.typography.titleMedium)
        if (placement == null) {
            Text("No Selection — tap a photo on the page to select it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }

        var x by remember(placement.id) { mutableStateOf(PhysicalMeasurementService.value(placement.origin.x, unit)) }
        var y by remember(placement.id) { mutableStateOf(PhysicalMeasurementService.value(placement.origin.y, unit)) }
        var width by remember(placement.id) { mutableStateOf(PhysicalMeasurementService.value(placement.size.width, unit)) }
        var height by remember(placement.id) { mutableStateOf(PhysicalMeasurementService.value(placement.size.height, unit)) }

        NumberField("X", x, unit.displayName) { x = it }
        NumberField("Y", y, unit.displayName) { y = it }
        NumberField("Width", width, unit.displayName) { width = it }
        NumberField("Height", height, unit.displayName) { height = it }

        Row(modifier = Modifier.fillMaxWidth().clickable { viewModel.rotatePlacement(placementID) }.padding(vertical = 8.dp)) {
            Text("Rotate 90°", color = MaterialTheme.colorScheme.primary)
        }

        androidx.compose.material3.Button(onClick = {
            val minSize = PhysicalMeasurementService.value(PrintlyConstants.minResizeDimensionPoints, unit)
            val maxSize = PhysicalMeasurementService.value(PrintlyConstants.maxResizeDimensionPoints, unit)
            val clampedWidth = width.coerceIn(minSize, maxSize)
            val clampedHeight = height.coerceIn(minSize, maxSize)
            viewModel.resizePlacement(
                placementID,
                com.a8000053398.printly.core.RectD(
                    PhysicalMeasurementService.points(x, unit), PhysicalMeasurementService.points(y, unit),
                    PhysicalMeasurementService.points(clampedWidth, unit), PhysicalMeasurementService.points(clampedHeight, unit)
                )
            )
        }, modifier = Modifier.fillMaxWidth()) { Text("Apply") }
    }
}

@Composable
private fun NumberField(label: String, value: Double, unitLabel: String, onChange: (Double) -> Unit) {
    var text by remember(value) { mutableStateOf("%.2f".format(value)) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(value = text, onValueChange = { text = it; it.toDoubleOrNull()?.let(onChange) }, modifier = Modifier.width(90.dp), singleLine = true)
            Text(unitLabel, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

private fun Modifier.androidx_height(dpValue: Int): Modifier = this.height(dpValue.dp)
