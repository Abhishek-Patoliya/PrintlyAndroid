package com.a8000053398.printly.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PhotoItem
import com.a8000053398.printly.model.PhotoSize
import com.a8000053398.printly.model.PhysicalSize
import com.a8000053398.printly.model.PrintProject
import com.a8000053398.printly.model.PrintTemplate
import com.a8000053398.printly.service.TemplateService
import com.a8000053398.printly.service.persistence.AppSettingsStore
import com.a8000053398.printly.service.persistence.ProjectPersistenceService
import java.util.UUID

class HomeViewModel(
    private val persistence: ProjectPersistenceService = ProjectPersistenceService.shared,
    private val settings: AppSettingsStore = AppSettingsStore.shared
) : ViewModel() {

    var recentProjects by mutableStateOf<List<PrintProject>>(emptyList())
        private set

    val templates: List<PrintTemplate> = TemplateService.builtInTemplates

    init {
        loadProjects()
    }

    fun loadProjects() {
        recentProjects = persistence.loadAllProjects()
    }

    fun deleteProject(project: PrintProject) {
        persistence.deleteProject(project)
        loadProjects()
    }

    fun deleteProjects(ids: Set<UUID>) {
        for (project in recentProjects) if (ids.contains(project.id)) persistence.deleteProject(project)
        loadProjects()
    }

    fun rename(project: PrintProject, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val updated = project.copy(name = trimmed, modifiedAt = System.currentTimeMillis())
        persistence.save(updated)
        loadProjects()
    }

    /** Duplicates a project, including its own copies of every image file on
     * disk — projects never share image files. */
    fun duplicate(project: PrintProject) {
        val photos = project.photos.map { photo ->
            photo.copy(
                id = UUID.randomUUID(),
                originalFileName = duplicatedFile(photo.originalFileName) ?: photo.originalFileName,
                editedFileName = photo.editedFileName?.let { duplicatedFile(it) }
            )
        }
        val copy = PrintProject(
            id = UUID.randomUUID(),
            name = project.name + " Copy",
            pageSize = project.pageSize,
            photoSize = project.photoSize,
            margins = project.margins,
            spacing = project.spacing,
            photos = photos,
            manualPlacementOverrides = emptyMap(),
            preferredUnit = project.preferredUnit,
            allowRotationToFit = project.allowRotationToFit,
            fillPageEdgeToEdge = project.fillPageEdgeToEdge,
            backgroundColor = project.backgroundColor,
            showCutGuides = project.showCutGuides,
            templateID = project.templateID,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        persistence.save(copy)
        loadProjects()
    }

    private fun duplicatedFile(fileName: String): String? {
        val bitmap: Bitmap = persistence.loadImage(fileName) ?: return null
        return persistence.saveImage(bitmap)
    }

    /** Builds a blank project seeded from the user's configured defaults. */
    fun newProject(): PrintProject = PrintProject(
        pageSize = settings.defaultPageSize,
        margins = settings.defaultMargins,
        spacing = settings.defaultSpacing,
        preferredUnit = settings.defaultUnit.value
    )

    fun newProject(template: PrintTemplate): PrintProject = TemplateService.apply(template, PrintProject())

    /** A blank project pre-sized for a small text label (2×1in) — the
     * starting point for Home's "Labels & Stickers" quick action. */
    fun newLabelProject(): PrintProject = PrintProject(
        name = "Label Sheet",
        pageSize = settings.defaultPageSize,
        photoSize = PhotoSize(com.a8000053398.printly.model.PhotoSizePreset.CUSTOM, PhysicalSize(2.0, 1.0, MeasurementUnit.INCH)),
        margins = settings.defaultMargins,
        spacing = settings.defaultSpacing,
        preferredUnit = settings.defaultUnit.value
    )
}
