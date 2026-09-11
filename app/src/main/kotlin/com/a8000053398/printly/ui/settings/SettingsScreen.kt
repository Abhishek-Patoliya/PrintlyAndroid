@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.AppearanceMode
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PageOrientation
import com.a8000053398.printly.model.PageSizePreset
import com.a8000053398.printly.service.persistence.AppSettingsStore
import com.a8000053398.printly.ui.components.PrintlyMark
import com.a8000053398.printly.ui.components.cardStyle
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.PrintlyConstants

@Composable
fun SettingsScreen(onOpenExportHistory: () -> Unit) {
    val settings = AppSettingsStore.shared
    val unit by settings.defaultUnit.collectAsState()
    val pageSizePreset by settings.defaultPageSizePreset.collectAsState()
    val orientation by settings.defaultOrientation.collectAsState()
    val marginsMM by settings.defaultMarginsMM.collectAsState()
    val spacingMM by settings.defaultSpacingMM.collectAsState()
    val appearance by settings.appearanceMode.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Metrics.spacingL.dp),
        verticalArrangement = Arrangement.spacedBy(Metrics.spacingL.dp)
    ) {
        item {
            SettingsSection(title = "New Project Defaults", footer = "Applied whenever you start a new blank project. Existing projects are unaffected.") {
                DropdownRow("Default Unit", unit.displayName, MeasurementUnit.entries.map { it to it.displayName }) { settings.setDefaultUnit(it) }
                HorizontalDivider()
                DropdownRow("Default Paper Size", pageSizePreset.displayName, PageSizePreset.entries.filter { it != PageSizePreset.CUSTOM }.map { it to it.displayName }) { settings.setDefaultPageSizePreset(it) }
                HorizontalDivider()
                DropdownRow("Default Orientation", orientation.displayName, PageOrientation.entries.map { it to it.displayName }) { settings.setDefaultOrientation(it) }
            }
        }

        item {
            SettingsSection(title = "Default Margins") {
                SliderRow("All Sides", marginsMM, 0.0..50.0, "mm") { settings.setDefaultMarginsMM(it) }
            }
        }

        item {
            SettingsSection(title = "Default Spacing") {
                SliderRow("Between Photos", spacingMM, 0.0..50.0, "mm") { settings.setDefaultSpacingMM(it) }
            }
        }

        item {
            SettingsSection(title = "Appearance") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    AppearanceMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = appearance == mode,
                            onClick = { settings.setAppearanceMode(mode) },
                            shape = SegmentedButtonDefaultsShape(index, AppearanceMode.entries.size)
                        ) { Text(mode.displayName) }
                    }
                }
            }
        }

        item {
            SettingsSection(title = null, footer = "PDFs and images you've generated from the Export screen, kept until you delete them or 30 more replace them.") {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenExportHistory),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
                ) {
                    Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Export History")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            SettingsSection(title = "About Printly", footer = "Printly processes your photos entirely on this device. Photos are never uploaded to a server.") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                    PrintlyMark(size = 34.dp)
                    Column {
                        Text("Printly", fontWeight = FontWeight.Bold)
                        Text("Photo & Document Printing", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Version")
                    Text("1.0.0 (1)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)) {
                    Icon(Icons.Filled.Print, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(16.dp))
                    Text(PrintlyConstants.PRINT_INSTRUCTION_TEXT, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String?, footer: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)) {
        if (title != null) {
            Text(title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(
            modifier = Modifier.fillMaxWidth().cardStyle().padding(Metrics.spacingM.dp),
            verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
        ) { content() }
        if (footer != null) {
            Text(footer, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun <T> DropdownRow(label: String, valueLabel: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(valueLabel, color = MaterialTheme.colorScheme.primary)
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for ((value, text) in options) {
                    DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelect(value) })
                }
            }
        }
    }
}

@Composable
private fun SliderRow(label: String, value: Double, range: ClosedFloatingPointRange<Double>, unitLabel: String, onChange: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text("%.1f %s".format(value, unitLabel), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        Slider(value = value.toFloat(), onValueChange = { onChange(it.toDouble()) }, valueRange = range.start.toFloat()..range.endInclusive.toFloat())
    }
}

@Composable
private fun SegmentedButtonDefaultsShape(index: Int, count: Int) =
    androidx.compose.material3.SegmentedButtonDefaults.itemShape(index = index, count = count)
