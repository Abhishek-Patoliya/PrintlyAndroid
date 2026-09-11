package com.a8000053398.printly.service.persistence

import android.content.Context
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.ExportRecord
import com.a8000053398.printly.model.ExportedFileFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID

/** Persists a rolling history of generated exports so leaving the Export
 * screen (or the app) doesn't lose access to something already generated.
 * Keeps its own copies in a dedicated internal-storage subdirectory, capped
 * to a bounded count. */
class ExportHistoryStore(context: Context = PrintlyApplication.appContext) {

    private val json = Json { ignoreUnknownKeys = true }
    private val maxRecords = 30

    private val exportsDirectory: File = File(context.filesDir, "Exports").apply { mkdirs() }
    private val metadataFile = File(context.filesDir, "ExportHistory.json")

    private val _records = MutableStateFlow<List<ExportRecord>>(load())
    val records: StateFlow<List<ExportRecord>> = _records.asStateFlow()

    /** Copies every file at [sourceFiles] into this store's own directory and
     * records one new [ExportRecord] for the whole set. Newest first; pruning
     * the oldest record (and its files) once [maxRecords] is exceeded. */
    fun record(sourceFiles: List<File>, projectName: String, format: ExportedFileFormat): ExportRecord? {
        if (sourceFiles.isEmpty()) return null

        val storedFileNames = mutableListOf<String>()
        for (source in sourceFiles) {
            val storedName = "${UUID.randomUUID()}.${source.extension}"
            val destination = File(exportsDirectory, storedName)
            val copied = runCatching { source.copyTo(destination, overwrite = true) }.isSuccess
            if (copied) storedFileNames.add(storedName)
        }
        if (storedFileNames.isEmpty()) return null

        val name = projectName.trim().ifEmpty { "Printly" }
        val entry = ExportRecord(projectName = name, format = format, fileNames = storedFileNames)
        var updated = listOf(entry) + _records.value

        while (updated.size > maxRecords) {
            val oldest = updated.last()
            deleteFiles(oldest)
            updated = updated.dropLast(1)
        }
        _records.value = updated
        persist()
        return entry
    }

    fun delete(record: ExportRecord) {
        _records.value = _records.value.filterNot { it.id == record.id }
        deleteFiles(record)
        persist()
    }

    /** Absolute files for every page of this export, in order. */
    fun files(record: ExportRecord): List<File> = record.fileNames.map { File(exportsDirectory, it) }

    private fun deleteFiles(record: ExportRecord) {
        for (fileName in record.fileNames) File(exportsDirectory, fileName).delete()
    }

    private fun persist() {
        runCatching { metadataFile.writeText(json.encodeToString(ListSerializer(ExportRecord.serializer()), _records.value)) }
    }

    private fun load(): List<ExportRecord> {
        if (!metadataFile.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(ExportRecord.serializer()), metadataFile.readText())
        }.getOrDefault(emptyList())
    }

    companion object {
        val shared: ExportHistoryStore by lazy { ExportHistoryStore() }
    }
}
