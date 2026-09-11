@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.a8000053398.printly.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.model.TemplateCategory
import com.a8000053398.printly.service.persistence.CustomTemplateStore
import com.a8000053398.printly.ui.components.IconGlassButton
import com.a8000053398.printly.ui.editor.sheets.ArrangeSheet
import com.a8000053398.printly.ui.editor.sheets.BackgroundColorSheet
import com.a8000053398.printly.ui.editor.sheets.CopiesSheet
import com.a8000053398.printly.ui.editor.sheets.ExactMeasurementSheet
import com.a8000053398.printly.ui.editor.sheets.MarginsSheet
import com.a8000053398.printly.ui.editor.sheets.MoreTool
import com.a8000053398.printly.ui.editor.sheets.MoreToolsSheet
import com.a8000053398.printly.ui.editor.sheets.PhotosSheet
import com.a8000053398.printly.ui.editor.sheets.SpacingSheet
import com.a8000053398.printly.ui.editor.sheets.PageSizeSheet
import com.a8000053398.printly.ui.editor.sheets.PhotoSizeSheet
import com.a8000053398.printly.ui.export.ExportScreen
import com.a8000053398.printly.ui.theme.IconMapping
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.util.MeasurementFormatter
import com.a8000053398.printly.viewmodel.EditorInitialAction
import com.a8000053398.printly.viewmodel.ProjectEditorViewModel
import kotlinx.coroutines.delay

private enum class EditorTool(val label: String, val iconName: String) {
    PHOTOS("Photos", "photo.on.rectangle.angled"),
    SIZE("Size", "aspectratio"),
    COPIES("Copies", "square.on.square"),
    SPACING("Spacing", "arrow.left.and.right"),
    MARGINS("Margins", "square.dashed"),
    MORE("More", "ellipsis.circle")
}

private sealed interface EditorRoute {
    object Photos : EditorRoute
    object PhotoSize : EditorRoute
    object PageSize : EditorRoute
    object Copies : EditorRoute
    object Spacing : EditorRoute
    object Margins : EditorRoute
    object More : EditorRoute
    object Arrange : EditorRoute
    object Background : EditorRoute
    data class Crop(val photoID: java.util.UUID) : EditorRoute
    data class ExactMeasurement(val placementID: java.util.UUID) : EditorRoute
}

/** The main page editor: top bar (Back / Page Size / Print / Save), the
 * printable page preview, a contextual toolbar for the selected photo, and a
 * bottom tool strip. Kotlin/Compose port of iOS's `EditorView`. */
@Composable
fun EditorScreen(viewModel: ProjectEditorViewModel, initialAction: EditorInitialAction?, onBack: () -> Unit) {
    var route by remember { mutableStateOf<EditorRoute?>(null) }
    var isShowingExport by remember { mutableStateOf(false) }
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(viewModel.project.name) }
    var isSavingTemplate by remember { mutableStateOf(false) }
    var templateNameText by remember { mutableStateOf(viewModel.project.name) }
    var savedConfirmation by remember { mutableStateOf(false) }
    var templateSavedConfirmation by remember { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler { viewModel.save(); onBack() }

    LaunchedEffect(Unit) {
        when (initialAction) {
            EditorInitialAction.PHOTOS -> route = EditorRoute.Photos
            EditorInitialAction.CUSTOM_PHOTO_SIZE -> {
                val current = viewModel.project.photoSize.physicalSize
                viewModel.setPhotoSize(com.a8000053398.printly.model.PhotoSize(com.a8000053398.printly.model.PhotoSizePreset.CUSTOM, current))
                route = EditorRoute.PhotoSize
            }
            EditorInitialAction.ADD_LABEL -> {
                val id = viewModel.addBlankLabel()
                if (id != null) route = EditorRoute.Crop(id)
            }
            null -> {}
        }
    }

    DisposableEffectSaveOnLeave(viewModel)

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = Metrics.spacingL.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconGlassButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onClick = { viewModel.save(); onBack() })
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { route = EditorRoute.PageSize }
                    .padding(horizontal = Metrics.spacingM.dp, vertical = 8.dp)
            ) {
                Text(viewModel.project.pageSize.displayName, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp), verticalAlignment = Alignment.CenterVertically) {
                IconGlassButton(Icons.Filled.Print, "Print", onClick = { isShowingExport = true })
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { viewModel.save(); savedConfirmation = true }
                        .padding(horizontal = Metrics.spacingM.dp, vertical = 8.dp)
                ) {
                    Text("Save", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Info strip
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp).padding(top = Metrics.spacingS.dp, bottom = Metrics.spacingM.dp)
                .clickable { renameText = viewModel.project.name; isRenaming = true },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${viewModel.project.name} · ${MeasurementFormatter.sizeString(viewModel.project.photoSize.physicalSize)} · ${viewModel.totalPlacedCount}/${viewModel.project.totalRequestedCopies} placed",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                IconButton(onClick = { viewModel.undo() }, enabled = viewModel.canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { viewModel.redo() }, enabled = viewModel.canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        if (viewModel.totalPages > 1) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.goToPreviousPage() }, enabled = viewModel.currentPageIndex > 0) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous page", tint = MaterialTheme.colorScheme.primary)
                }
                Text("Page ${viewModel.currentPageIndex + 1} of ${viewModel.totalPages}", fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { viewModel.goToNextPage() }, enabled = viewModel.currentPageIndex < viewModel.totalPages - 1) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next page", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(Metrics.spacingL.dp), contentAlignment = Alignment.Center) {
            PageCanvas(viewModel = viewModel, onTapEmptyArea = { route = EditorRoute.Photos }, modifier = Modifier.fillMaxSize())
        }

        val layoutError = viewModel.layoutError
        if (layoutError != null) {
            Text(
                layoutError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = Metrics.spacingL.dp, vertical = 4.dp)
            )
        }

        if (viewModel.selectedPlacementID != null && !viewModel.isSelectingMultiplePlacements) {
            SelectedPhotoToolbar(
                onEdit = { viewModel.selectedPlacementID?.let { id -> photoIDForPlacement(viewModel, id)?.let { route = EditorRoute.Crop(it) } } },
                onRotate = { viewModel.selectedPlacementID?.let { viewModel.rotatePlacement(it) } },
                onDuplicate = { viewModel.selectedPlacementID?.let { viewModel.duplicatePlacement(it) } },
                onPosition = { viewModel.selectedPlacementID?.let { route = EditorRoute.ExactMeasurement(it) } },
                onDelete = { viewModel.selectedPlacementID?.let { viewModel.deletePlacement(it) } }
            )
        }

        if (viewModel.isSelectingMultiplePlacements) {
            MultiSelectActionBar(viewModel)
        } else {
            EditorToolStrip(onSelect = { tool ->
                route = when (tool) {
                    EditorTool.PHOTOS -> EditorRoute.Photos
                    EditorTool.SIZE -> EditorRoute.PhotoSize
                    EditorTool.COPIES -> EditorRoute.Copies
                    EditorTool.SPACING -> EditorRoute.Spacing
                    EditorTool.MARGINS -> EditorRoute.Margins
                    EditorTool.MORE -> EditorRoute.More
                }
            })
        }
    }

    val currentRoute = route
    if (currentRoute != null) {
        ModalBottomSheet(onDismissRequest = { route = null }) {
            when (currentRoute) {
                is EditorRoute.Photos -> PhotosSheet(viewModel = viewModel, onAddLabel = {
                    route = null
                    val id = viewModel.addBlankLabel()
                    if (id != null) route = EditorRoute.Crop(id)
                })
                is EditorRoute.PhotoSize -> PhotoSizeSheet(photoSize = viewModel.project.photoSize, onChange = { viewModel.setPhotoSize(it) })
                is EditorRoute.PageSize -> PageSizeSheet(pageSize = viewModel.project.pageSize, onChange = { viewModel.setPageSize(it) })
                is EditorRoute.Copies -> CopiesSheet(viewModel)
                is EditorRoute.Spacing -> SpacingSheet(viewModel)
                is EditorRoute.Margins -> MarginsSheet(viewModel)
                is EditorRoute.More -> MoreToolsSheet(hasPlacements = viewModel.layout.placements.isNotEmpty(), onSelect = { tool ->
                    route = null
                    when (tool) {
                        MoreTool.ARRANGE -> route = EditorRoute.Arrange
                        MoreTool.PAGE_SETTINGS -> route = EditorRoute.PageSize
                        MoreTool.BACKGROUND -> route = EditorRoute.Background
                        MoreTool.SAVE_TEMPLATE -> { templateNameText = viewModel.project.name; isSavingTemplate = true }
                        MoreTool.SELECT_MULTIPLE -> viewModel.setMultiSelectMode(true)
                    }
                })
                is EditorRoute.Arrange -> ArrangeSheet(viewModel)
                is EditorRoute.Background -> BackgroundColorSheet(viewModel)
                is EditorRoute.Crop -> {
                    val photo = viewModel.project.photos.firstOrNull { it.id == currentRoute.photoID }
                    if (photo != null) {
                        com.a8000053398.printly.ui.editor.crop.CropScreen(viewModel = viewModel, photo = photo, onDone = { route = null })
                    }
                }
                is EditorRoute.ExactMeasurement -> ExactMeasurementSheet(viewModel, currentRoute.placementID)
            }
        }
    }

    if (isShowingExport) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isShowingExport = false }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            ExportScreen(viewModel = viewModel, onClose = { isShowingExport = false })
        }
    }

    if (isRenaming) {
        AlertDialog(
            onDismissRequest = { isRenaming = false },
            title = { Text("Rename Project") },
            text = { TextField(value = renameText, onValueChange = { renameText = it }) },
            confirmButton = { TextButton(onClick = { viewModel.rename(renameText); isRenaming = false }) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { isRenaming = false }) { Text("Cancel") } }
        )
    }

    if (isSavingTemplate) {
        AlertDialog(
            onDismissRequest = { isSavingTemplate = false },
            title = { Text("Save as Template") },
            text = {
                Column {
                    Text("Saves this project's page size, photo size, margins, and spacing as a reusable template under My Templates.", fontSize = 12.sp)
                    TextField(value = templateNameText, onValueChange = { templateNameText = it })
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = templateNameText.trim()
                    if (trimmed.isNotEmpty()) {
                        val template = PrintTemplate(
                            name = trimmed, iconName = "square.grid.2x2", category = TemplateCategory.MY_TEMPLATES,
                            pageSize = viewModel.project.pageSize, photoSize = viewModel.project.photoSize,
                            margins = viewModel.project.margins, spacing = viewModel.project.spacing,
                            suggestedCopies = (viewModel.project.photos.firstOrNull()?.copies ?: 1).coerceAtLeast(1),
                            fillPageEdgeToEdge = viewModel.project.fillPageEdgeToEdge
                        )
                        CustomTemplateStore.shared.save(template)
                        templateSavedConfirmation = true
                    }
                    isSavingTemplate = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { isSavingTemplate = false }) { Text("Cancel") } }
        )
    }

    if (savedConfirmation) {
        AlertDialog(onDismissRequest = { savedConfirmation = false }, title = { Text("Project Saved") }, confirmButton = { TextButton(onClick = { savedConfirmation = false }) { Text("OK") } })
    }
    if (templateSavedConfirmation) {
        AlertDialog(onDismissRequest = { templateSavedConfirmation = false }, title = { Text("Template Saved") }, confirmButton = { TextButton(onClick = { templateSavedConfirmation = false }) { Text("OK") } })
    }
}

@Composable
private fun DisposableEffectSaveOnLeave(viewModel: ProjectEditorViewModel) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { viewModel.save() }
    }
}

private fun photoIDForPlacement(viewModel: ProjectEditorViewModel, placementID: java.util.UUID): java.util.UUID? =
    viewModel.layout.placements.firstOrNull { it.id == placementID }?.photoID

@Composable
private fun SelectedPhotoToolbar(onEdit: () -> Unit, onRotate: () -> Unit, onDuplicate: () -> Unit, onPosition: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingS.dp)
            .clip(RoundedCornerShape(Metrics.radiusMedium.dp)).background(MaterialTheme.colorScheme.surface).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ContextButton(Icons.Filled.Edit, "Edit", onClick = onEdit)
        ContextButton(IconMapping.icon("rotate.right"), "Rotate", onClick = onRotate)
        ContextButton(Icons.Filled.ContentCopy, "Duplicate", onClick = onDuplicate)
        ContextButton(IconMapping.icon("ruler.fill"), "Position", onClick = onPosition)
        ContextButton(Icons.Filled.Delete, "Delete", onClick = onDelete, isDestructive = true)
    }
}

@Composable
private fun ContextButton(icon: ImageVector, label: String, onClick: () -> Unit, isDestructive: Boolean = false) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = Metrics.spacingS.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 10.sp, color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MultiSelectActionBar(viewModel: ProjectEditorViewModel) {
    val allIDs = viewModel.layout.placements.map { it.id }.toSet()
    val isAllSelected = allIDs.isNotEmpty() && viewModel.multiSelectedPlacementIDs == allIDs
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingM.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = {
            if (isAllSelected) viewModel.multiSelectedPlacementIDs = emptySet() else viewModel.selectAllPlacementsForMultiSelect()
        }) { Text(if (isAllSelected) "Deselect All" else "Select All") }
        IconButton(onClick = { viewModel.rotateMultiSelectedPlacements() }, enabled = viewModel.multiSelectedPlacementIDs.isNotEmpty()) {
            Icon(IconMapping.icon("rotate.right"), contentDescription = "Rotate")
        }
        TextButton(onClick = { viewModel.deleteMultiSelectedPlacements() }, enabled = viewModel.multiSelectedPlacementIDs.isNotEmpty()) {
            Text(if (viewModel.multiSelectedPlacementIDs.isEmpty()) "Delete" else "Delete (${viewModel.multiSelectedPlacementIDs.size})", color = MaterialTheme.colorScheme.error)
        }
        TextButton(onClick = { viewModel.setMultiSelectMode(false) }) { Text("Done", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun EditorToolStrip(onSelect: (EditorTool) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).height(68.dp).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (tool in EditorTool.entries) {
            Column(
                modifier = Modifier.clickable { onSelect(tool) }.padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(IconMapping.icon(tool.iconName), contentDescription = tool.label)
                Text(tool.label, fontSize = 10.sp)
            }
        }
    }
}
