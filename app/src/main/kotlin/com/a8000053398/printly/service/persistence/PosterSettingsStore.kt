package com.a8000053398.printly.service.persistence

import android.content.Context
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.PageSizePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembers the last-used Poster Print settings (page size, grid, overlap)
 * across launches. */
class PosterSettingsStore(context: Context = PrintlyApplication.appContext) {
    private val prefs = context.getSharedPreferences("printly_poster_settings", Context.MODE_PRIVATE)

    private object Keys {
        const val PAGE_SIZE_PRESET = "poster.pageSizePreset"
        const val COLUMNS = "poster.columns"
        const val ROWS = "poster.rows"
        const val OVERLAP_MM = "poster.overlapMM"
    }

    private val _pageSizePreset = MutableStateFlow(
        prefs.getString(Keys.PAGE_SIZE_PRESET, null)?.let { runCatching { PageSizePreset.valueOf(it) }.getOrNull() } ?: PageSizePreset.A4
    )
    val pageSizePreset: StateFlow<PageSizePreset> = _pageSizePreset.asStateFlow()
    fun setPageSizePreset(value: PageSizePreset) {
        _pageSizePreset.value = value
        prefs.edit().putString(Keys.PAGE_SIZE_PRESET, value.name).apply()
    }

    private val _columns = MutableStateFlow(prefs.getInt(Keys.COLUMNS, 2))
    val columns: StateFlow<Int> = _columns.asStateFlow()
    fun setColumns(value: Int) {
        _columns.value = value
        prefs.edit().putInt(Keys.COLUMNS, value).apply()
    }

    private val _rows = MutableStateFlow(prefs.getInt(Keys.ROWS, 2))
    val rows: StateFlow<Int> = _rows.asStateFlow()
    fun setRows(value: Int) {
        _rows.value = value
        prefs.edit().putInt(Keys.ROWS, value).apply()
    }

    private val _overlapMM = MutableStateFlow(prefs.getFloat(Keys.OVERLAP_MM, 5f).toDouble())
    val overlapMM: StateFlow<Double> = _overlapMM.asStateFlow()
    fun setOverlapMM(value: Double) {
        _overlapMM.value = value
        prefs.edit().putFloat(Keys.OVERLAP_MM, value.toFloat()).apply()
    }

    companion object {
        val shared: PosterSettingsStore by lazy { PosterSettingsStore() }
    }
}
