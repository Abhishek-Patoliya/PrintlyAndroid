package com.a8000053398.printly.service.persistence

import android.content.Context
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.PrintTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import java.io.File

/** Persists user-saved templates — a project's page size, photo size, margins,
 * and spacing, saved under a name so it can be reused for a future project. */
class CustomTemplateStore(context: Context = PrintlyApplication.appContext) {

    private val json = Json { ignoreUnknownKeys = true }
    private val file = File(context.filesDir, "CustomTemplates.json")

    private val _templates = MutableStateFlow<List<PrintTemplate>>(load())
    val templates: StateFlow<List<PrintTemplate>> = _templates.asStateFlow()

    fun save(template: PrintTemplate) {
        _templates.value = listOf(template) + _templates.value
        persist()
    }

    fun delete(template: PrintTemplate) {
        _templates.value = _templates.value.filterNot { it.id == template.id }
        persist()
    }

    private fun persist() {
        runCatching { file.writeText(json.encodeToString(PrintTemplate.serializer().let { kotlinx.serialization.builtins.ListSerializer(it) }, _templates.value)) }
    }

    private fun load(): List<PrintTemplate> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(PrintTemplate.serializer()), file.readText())
        }.getOrDefault(emptyList())
    }

    companion object {
        val shared: CustomTemplateStore by lazy { CustomTemplateStore() }
    }
}
