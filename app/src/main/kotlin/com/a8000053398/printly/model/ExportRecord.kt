package com.a8000053398.printly.model

import com.a8000053398.printly.core.UUIDSerializer
import kotlinx.serialization.Serializable
import java.util.UUID

/** The file format an [ExportRecord] was generated in. */
@Serializable
enum class ExportedFileFormat(val displayName: String, val iconName: String) {
    PDF("PDF", "description"),
    JPG("JPG", "image"),
    PNG("PNG", "image");
}

/** One past export: which project it came from, in what format, when, and the
 * filenames of the copies kept in `ExportHistoryStore`'s own directory. */
@Serializable
data class ExportRecord(
    @Serializable(with = UUIDSerializer::class) val id: UUID = UUID.randomUUID(),
    val projectName: String,
    val format: ExportedFileFormat,
    val fileNames: List<String>,
    val createdAt: Long = System.currentTimeMillis()
) {
    val pageCount: Int get() = fileNames.size
}
