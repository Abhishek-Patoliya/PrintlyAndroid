package com.a8000053398.printly.service.persistence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.PrintProject
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Persists [PrintProject]s and their associated image files to the app's
 * internal files directory (the Android equivalent of iOS's Documents
 * directory) so projects survive relaunch. */
class ProjectPersistenceService(context: Context = PrintlyApplication.appContext) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val projectsDirectory: File = File(context.filesDir, "Projects").apply { mkdirs() }
    private val imagesDirectory: File = File(context.filesDir, "Images").apply { mkdirs() }

    fun projectFile(id: UUID): File = File(projectsDirectory, "$id.json")

    fun save(project: PrintProject) {
        val data = json.encodeToString(PrintProject.serializer(), project)
        projectFile(project.id).writeText(data)
    }

    fun loadAllProjects(): List<PrintProject> {
        val files = projectsDirectory.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching { json.decodeFromString(PrintProject.serializer(), file.readText()) }.getOrNull()
        }.sortedByDescending { it.modifiedAt }
    }

    fun deleteProject(project: PrintProject) {
        projectFile(project.id).delete()
        for (photo in project.photos) {
            deleteImage(photo.originalFileName)
            photo.editedFileName?.let { deleteImage(it) }
        }
    }

    // MARK: - Images

    /** Saves [bitmap] as JPEG under a new unique filename and returns that filename. */
    fun saveImage(bitmap: Bitmap, quality: Int = 95): String {
        val fileName = "${UUID.randomUUID()}.jpg"
        FileOutputStream(File(imagesDirectory, fileName)).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return fileName
    }

    fun loadImage(fileName: String): Bitmap? {
        val file = File(imagesDirectory, fileName)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun deleteImage(fileName: String) {
        File(imagesDirectory, fileName).delete()
    }

    companion object {
        val shared: ProjectPersistenceService by lazy { ProjectPersistenceService() }
    }
}
