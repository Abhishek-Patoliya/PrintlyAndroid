package com.a8000053398.printly.service.persistence

import android.content.Context
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.PhotoSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Remembers the most recently used photo sizes so the size picker can offer
 * a quick-access "Recent" row. Backed by [android.content.SharedPreferences],
 * capped to a small count, most-recent-first, de-duplicated. */
class RecentSizesStore(context: Context = PrintlyApplication.appContext) {
    private val json = Json
    private val prefs = context.getSharedPreferences("printly_recent_sizes", Context.MODE_PRIVATE)
    private val key = "recentPhotoSizes"
    private val maxCount = 6

    private val _recentPhotoSizes = MutableStateFlow(load())
    val recentPhotoSizes: StateFlow<List<PhotoSize>> = _recentPhotoSizes.asStateFlow()

    fun record(size: PhotoSize) {
        var updated = _recentPhotoSizes.value.filterNot { it == size }
        updated = listOf(size) + updated
        if (updated.size > maxCount) updated = updated.take(maxCount)
        _recentPhotoSizes.value = updated
        persist()
    }

    private fun persist() {
        runCatching {
            prefs.edit().putString(key, json.encodeToString(ListSerializer(PhotoSize.serializer()), _recentPhotoSizes.value)).apply()
        }
    }

    private fun load(): List<PhotoSize> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching { json.decodeFromString(ListSerializer(PhotoSize.serializer()), raw) }.getOrDefault(emptyList())
    }

    companion object {
        val shared: RecentSizesStore by lazy { RecentSizesStore() }
    }
}
