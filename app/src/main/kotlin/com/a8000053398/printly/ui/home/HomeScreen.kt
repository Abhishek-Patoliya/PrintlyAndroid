package com.a8000053398.printly.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.TemplateCategory
import com.a8000053398.printly.ui.components.IconBadge
import com.a8000053398.printly.ui.components.MiniLayoutPreview
import com.a8000053398.printly.ui.components.PassportPhotoIconBadge
import com.a8000053398.printly.ui.components.PrintlyBrandLockup
import com.a8000053398.printly.ui.components.PrintlyMark
import com.a8000053398.printly.ui.components.SectionHeaderText
import com.a8000053398.printly.ui.components.cardStyle
import com.a8000053398.printly.ui.components.previewBadgeContainer
import com.a8000053398.printly.ui.components.templatePreview
import com.a8000053398.printly.ui.components.projectPreview
import com.a8000053398.printly.ui.theme.IconMapping
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.viewmodel.EditorInitialAction
import com.a8000053398.printly.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenProject: (PrintProject) -> Unit,
    onNewFromQuickAction: (PrintProject, EditorInitialAction?) -> Unit,
    onOpenTemplatesCategory: (TemplateCategory?) -> Unit,
    onOpenPoster: () -> Unit
) {
    var isSelecting by remember { mutableStateOf(false) }
    var selectedIDs by remember { mutableStateOf(setOf<UUID>()) }
    var projectToRename by remember { mutableStateOf<PrintProject?>(null) }
    var renameText by remember { mutableStateOf("") }
    var isShowingDeleteConfirmation by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (viewModel.recentProjects.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingS.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    isSelecting = !isSelecting
                    selectedIDs = emptySet()
                }) { Text(if (isSelecting) "Cancel" else "Select") }
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = Metrics.spacingL.dp),
                verticalArrangement = Arrangement.spacedBy(Metrics.spacingXL.dp)
            ) {
                if (!isSelecting) {
                    item {
                        PrintlyBrandLockup(modifier = Modifier.padding(horizontal = Metrics.spacingL.dp))
                    }
                    item {
                        QuickActionsGrid(
                            onPassport = { onOpenTemplatesCategory(TemplateCategory.ID_AND_DOCUMENTS) },
                            onPhotoPrints = { onOpenTemplatesCategory(TemplateCategory.PHOTO_PRINTS) },
                            onCollage = { onOpenTemplatesCategory(TemplateCategory.COLLAGES) },
                            onCustomSize = { onNewFromQuickAction(viewModel.newProject(), EditorInitialAction.CUSTOM_PHOTO_SIZE) },
                            onLabels = { onNewFromQuickAction(viewModel.newLabelProject(), EditorInitialAction.ADD_LABEL) },
                            onPoster = onOpenPoster
                        )
                    }
                }

                if (viewModel.recentProjects.isEmpty()) {
                    item { EmptyState() }
                } else {
                    item {
                        SectionHeaderText("Recent Projects", modifier = Modifier.padding(horizontal = (Metrics.spacingL + 4).dp))
                    }
                    items(viewModel.recentProjects, key = { it.id }) { project ->
                        var showMenu by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.padding(horizontal = Metrics.spacingL.dp, vertical = 4.dp)) {
                            ProjectRow(
                                project = project,
                                isSelecting = isSelecting,
                                isSelected = selectedIDs.contains(project.id),
                                onClick = {
                                    if (isSelecting) {
                                        selectedIDs = if (selectedIDs.contains(project.id)) selectedIDs - project.id else selectedIDs + project.id
                                    } else {
                                        onOpenProject(project)
                                    }
                                },
                                onLongClick = { if (!isSelecting) showMenu = true }
                            )
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Filled.Edit, null) }, onClick = {
                                    showMenu = false; projectToRename = project; renameText = project.name
                                })
                                DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }, onClick = {
                                    showMenu = false; viewModel.duplicate(project)
                                })
                                DropdownMenuItem(text = { Text("Delete") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = {
                                    showMenu = false; viewModel.deleteProject(project)
                                })
                            }
                        }
                    }
                }
            }
        }

        if (isSelecting) {
            SelectionActionBar(
                totalCount = viewModel.recentProjects.size,
                selectedCount = selectedIDs.size,
                onSelectAllToggle = {
                    selectedIDs = if (selectedIDs.size == viewModel.recentProjects.size) emptySet() else viewModel.recentProjects.map { it.id }.toSet()
                },
                onDelete = { isShowingDeleteConfirmation = true }
            )
        }
    }

    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project") },
            text = { TextField(value = renameText, onValueChange = { renameText = it }) },
            confirmButton = {
                TextButton(onClick = {
                    projectToRename?.let { viewModel.rename(it, renameText) }
                    projectToRename = null
                }) { Text("Rename") }
            },
            dismissButton = { TextButton(onClick = { projectToRename = null }) { Text("Cancel") } }
        )
    }

    if (isShowingDeleteConfirmation) {
        val count = selectedIDs.size
        AlertDialog(
            onDismissRequest = { isShowingDeleteConfirmation = false },
            title = { Text("Delete $count ${if (count == 1) "Project" else "Projects"}") },
            text = { Text("This can't be undone. Photos added to these projects will also be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteProjects(selectedIDs)
                    selectedIDs = emptySet()
                    isSelecting = false
                    isShowingDeleteConfirmation = false
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { isShowingDeleteConfirmation = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun QuickActionsGrid(
    onPassport: () -> Unit,
    onPhotoPrints: () -> Unit,
    onCollage: () -> Unit,
    onCustomSize: () -> Unit,
    onLabels: () -> Unit,
    onPoster: () -> Unit
) {
    val photoPrintsTemplate = com.a8000053398.printly.service.TemplateService.builtInTemplates.first { it.category == TemplateCategory.PHOTO_PRINTS && it.suggestedCopies == 1 }
    val collageTemplate = com.a8000053398.printly.service.TemplateService.builtInTemplates.first { it.category == TemplateCategory.COLLAGES && it.name.startsWith("4-") }

    Column(modifier = Modifier.padding(horizontal = Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            QuickActionCard("Passport / ID Photos", isPassportSize = true, modifier = Modifier.weight(1f), onClick = onPassport)
            QuickActionCard("Photo Prints", template = photoPrintsTemplate, modifier = Modifier.weight(1f), onClick = onPhotoPrints)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            QuickActionCard("Collage", template = collageTemplate, modifier = Modifier.weight(1f), onClick = onCollage)
            QuickActionCard("Custom Size", iconName = "ruler.fill", modifier = Modifier.weight(1f), onClick = onCustomSize)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
            QuickActionCard("Labels & Stickers", iconName = "tag.fill", modifier = Modifier.weight(1f), onClick = onLabels)
            QuickActionCard("Poster Print", iconName = "rectangle.split.3x3.fill", modifier = Modifier.weight(1f), onClick = onPoster)
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    modifier: Modifier = Modifier,
    iconName: String? = null,
    template: com.a8000053398.printly.model.PrintTemplate? = null,
    isPassportSize: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .cardStyle()
            .clickable(onClick = onClick)
            .padding(vertical = Metrics.spacingM.dp, horizontal = Metrics.spacingS.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Metrics.spacingS.dp)
    ) {
        when {
            isPassportSize -> PassportPhotoIconBadge(size = 44.dp)
            template != null -> {
                val (pageSize, placements) = templatePreview(template)
                MiniLayoutPreview(pageSize, placements, modifier = Modifier.previewBadgeContainer(44.dp, MaterialTheme.colorScheme.primary))
            }
            else -> IconBadge(IconMapping.icon(iconName ?: "photo"), size = 44.dp)
        }
        Text(
            title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) { PrintlyMark(size = 48.dp) }
        Text("No Projects Yet", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Text(
            "Start a new project or pick a template to see it here.",
            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = Metrics.spacingXXL.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ProjectRow(project: PrintProject, isSelecting: Boolean, isSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .cardStyle()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(Metrics.spacingM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
    ) {
        if (isSelecting) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.Circle,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }
        val (pageSize, placements) = projectPreview(project)
        MiniLayoutPreview(pageSize, placements, modifier = Modifier.previewBadgeContainer(44.dp, MaterialTheme.colorScheme.primary))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(project.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${project.pageSize.displayName} · ${project.photoSize.displayName} · ${project.totalRequestedCopies} copies",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Text("Edited ${dateFormat.format(Date(project.modifiedAt))}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        }

        if (!isSelecting) {
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectionActionBar(totalCount: Int, selectedCount: Int, onSelectAllToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = Metrics.spacingL.dp, vertical = Metrics.spacingM.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onSelectAllToggle) { Text(if (selectedCount == totalCount) "Deselect All" else "Select All") }
        TextButton(onClick = onDelete, enabled = selectedCount > 0) {
            Text(if (selectedCount == 0) "Delete" else "Delete ($selectedCount)", color = MaterialTheme.colorScheme.error)
        }
    }
}
