package com.a8000053398.printly.ui.templates

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.model.TemplateCategory
import com.a8000053398.printly.service.persistence.CustomTemplateStore
import com.a8000053398.printly.ui.components.IconBadge
import com.a8000053398.printly.ui.components.MiniLayoutPreview
import com.a8000053398.printly.ui.components.PassportPhotoIconBadge
import com.a8000053398.printly.ui.components.SectionHeaderText
import com.a8000053398.printly.ui.components.cardStyle
import com.a8000053398.printly.ui.components.previewBadgeContainer
import com.a8000053398.printly.ui.components.templatePreview
import com.a8000053398.printly.ui.theme.IconMapping
import com.a8000053398.printly.ui.theme.Metrics
import com.a8000053398.printly.viewmodel.HomeViewModel

@Composable
fun TemplatesScreen(
    homeViewModel: HomeViewModel,
    initialCategory: TemplateCategory?,
    onNewProject: (PrintProject) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    val customTemplates by CustomTemplateStore.shared.templates.collectAsState()
    val allTemplates = homeViewModel.templates + customTemplates
    val filteredTemplates = if (selectedCategory == null) allTemplates else allTemplates.filter { it.category == selectedCategory }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = Metrics.spacingL.dp),
        verticalArrangement = Arrangement.spacedBy(Metrics.spacingXL.dp)
    ) {
        item {
            CategoryPicker(selected = selectedCategory, onSelect = { selectedCategory = it })
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SectionHeaderText(
                    selectedCategory?.displayName ?: "Built-in Templates",
                    modifier = Modifier.padding(horizontal = (Metrics.spacingL + 4).dp)
                )

                if (filteredTemplates.isEmpty() && selectedCategory == TemplateCategory.MY_TEMPLATES) {
                    Text(
                        "No Custom Templates — save one from the editor's More menu to see it here.",
                        modifier = Modifier.padding(horizontal = Metrics.spacingL.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(modifier = Modifier.padding(horizontal = Metrics.spacingL.dp), verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                        for (template in filteredTemplates) {
                            val isCustom = customTemplates.any { it.id == template.id }
                            TemplateRow(
                                template = template,
                                onClick = { onNewProject(homeViewModel.newProject(template)) },
                                onDelete = if (isCustom) { { CustomTemplateStore.shared.delete(template) } } else null
                            )
                        }
                    }
                }

                val footerText = when (selectedCategory) {
                    TemplateCategory.ID_AND_DOCUMENTS -> "Sizes shown are common defaults, not guaranteed to meet a specific country's or authority's official requirements. Verify before submitting."
                    TemplateCategory.LABELS -> "After choosing a size, use Photos > Add Text Label in the editor to add your label's text. Avery sheet layouts are common defaults — verify alignment before bulk printing."
                    TemplateCategory.MY_TEMPLATES -> if (filteredTemplates.isNotEmpty()) "Hold a template to delete it." else null
                    else -> "Choose a template to start a new project with its page size, photo size, margins, and spacing already set."
                }
                if (footerText != null) {
                    Text(
                        footerText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = (Metrics.spacingL + 4).dp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)) {
                SectionHeaderText("Start From Scratch", modifier = Modifier.padding(horizontal = (Metrics.spacingL + 4).dp))
                Box(modifier = Modifier.padding(horizontal = Metrics.spacingL.dp)) {
                    TemplateRow(
                        title = "Custom",
                        subtitle = "Start blank and configure everything yourself",
                        iconName = "slider.horizontal.3",
                        onClick = { onNewProject(homeViewModel.newProject()) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryPicker(selected: TemplateCategory?, onSelect: (TemplateCategory?) -> Unit) {
    LazyRow(
        modifier = Modifier
            .padding(horizontal = Metrics.spacingL.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { CategoryPill("All", selected == null) { onSelect(null) } }
        items(TemplateCategory.entries) { category -> CategoryPill(category.displayName, selected == category) { onSelect(category) } }
    }
}

@Composable
private fun CategoryPill(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TemplateRow(
    template: PrintTemplate? = null,
    title: String = template?.name ?: "",
    subtitle: String = template?.let { "${it.pageSize.displayName} · ${it.photoSize.displayName} · ${it.suggestedCopies} copies" } ?: "",
    iconName: String = template?.iconName ?: "photo",
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cardStyle()
                .combinedClickable(onClick = onClick, onLongClick = if (onDelete != null) { { showMenu = true } } else null)
                .padding(Metrics.spacingM.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Metrics.spacingM.dp)
        ) {
            if (template != null && template.photoSize.preset == com.a8000053398.printly.model.PhotoSizePreset.PASSPORT) {
                PassportPhotoIconBadge(size = 44.dp)
            } else if (template != null) {
                val (pageSize, placements) = templatePreview(template)
                MiniLayoutPreview(pageSize, placements, modifier = Modifier.previewBadgeContainer(44.dp, MaterialTheme.colorScheme.primary))
            } else {
                IconBadge(IconMapping.icon(iconName), size = 44.dp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (onDelete != null) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Delete Template") }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { showMenu = false; onDelete() })
            }
        }
    }
}
