package com.a8000053398.printly.service.persistence

import android.content.Context
import com.a8000053398.printly.PrintlyApplication
import com.a8000053398.printly.model.AppearanceMode
import com.a8000053398.printly.model.MeasurementUnit
import com.a8000053398.printly.model.PageMargins
import com.a8000053398.printly.model.PageOrientation
import com.a8000053398.printly.model.PageSize
import com.a8000053398.printly.model.PageSizePreset
import com.a8000053398.printly.model.PageSpacing
import com.a8000053398.printly.service.PhysicalMeasurementService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** User-configurable defaults applied to every *new* project (existing
 * projects keep whatever settings they were created or last edited with).
 * Backed by [android.content.SharedPreferences]; each property auto-persists
 * on write, mirroring the iOS `@Published ... { didSet { ... } }` pattern. */
class AppSettingsStore(context: Context = PrintlyApplication.appContext) {
    private val prefs = context.getSharedPreferences("printly_settings", Context.MODE_PRIVATE)

    private object Keys {
        const val DEFAULT_UNIT = "settings.defaultUnit"
        const val DEFAULT_PAGE_SIZE_PRESET = "settings.defaultPageSizePreset"
        const val DEFAULT_ORIENTATION = "settings.defaultOrientation"
        const val DEFAULT_MARGINS_MM = "settings.defaultMarginsMM"
        const val DEFAULT_SPACING_MM = "settings.defaultSpacingMM"
        const val APPEARANCE_MODE = "settings.appearanceMode"
    }

    private val _defaultUnit = MutableStateFlow(
        prefs.getString(Keys.DEFAULT_UNIT, null)?.let { runCatching { MeasurementUnit.valueOf(it) }.getOrNull() } ?: MeasurementUnit.MILLIMETER
    )
    val defaultUnit: StateFlow<MeasurementUnit> = _defaultUnit.asStateFlow()
    fun setDefaultUnit(value: MeasurementUnit) {
        _defaultUnit.value = value
        prefs.edit().putString(Keys.DEFAULT_UNIT, value.name).apply()
    }

    private val _defaultPageSizePreset = MutableStateFlow(
        prefs.getString(Keys.DEFAULT_PAGE_SIZE_PRESET, null)?.let { runCatching { PageSizePreset.valueOf(it) }.getOrNull() } ?: PageSizePreset.A4
    )
    val defaultPageSizePreset: StateFlow<PageSizePreset> = _defaultPageSizePreset.asStateFlow()
    fun setDefaultPageSizePreset(value: PageSizePreset) {
        _defaultPageSizePreset.value = value
        prefs.edit().putString(Keys.DEFAULT_PAGE_SIZE_PRESET, value.name).apply()
    }

    private val _defaultOrientation = MutableStateFlow(
        prefs.getString(Keys.DEFAULT_ORIENTATION, null)?.let { runCatching { PageOrientation.valueOf(it) }.getOrNull() } ?: PageOrientation.PORTRAIT
    )
    val defaultOrientation: StateFlow<PageOrientation> = _defaultOrientation.asStateFlow()
    fun setDefaultOrientation(value: PageOrientation) {
        _defaultOrientation.value = value
        prefs.edit().putString(Keys.DEFAULT_ORIENTATION, value.name).apply()
    }

    /** Stored in millimeters regardless of [defaultUnit]. */
    private val _defaultMarginsMM = MutableStateFlow(prefs.getFloat(Keys.DEFAULT_MARGINS_MM, 10f).toDouble())
    val defaultMarginsMM: StateFlow<Double> = _defaultMarginsMM.asStateFlow()
    fun setDefaultMarginsMM(value: Double) {
        _defaultMarginsMM.value = value
        prefs.edit().putFloat(Keys.DEFAULT_MARGINS_MM, value.toFloat()).apply()
    }

    private val _defaultSpacingMM = MutableStateFlow(prefs.getFloat(Keys.DEFAULT_SPACING_MM, 3f).toDouble())
    val defaultSpacingMM: StateFlow<Double> = _defaultSpacingMM.asStateFlow()
    fun setDefaultSpacingMM(value: Double) {
        _defaultSpacingMM.value = value
        prefs.edit().putFloat(Keys.DEFAULT_SPACING_MM, value.toFloat()).apply()
    }

    private val _appearanceMode = MutableStateFlow(
        prefs.getString(Keys.APPEARANCE_MODE, null)?.let { runCatching { AppearanceMode.valueOf(it) }.getOrNull() } ?: AppearanceMode.SYSTEM
    )
    val appearanceMode: StateFlow<AppearanceMode> = _appearanceMode.asStateFlow()
    fun setAppearanceMode(value: AppearanceMode) {
        _appearanceMode.value = value
        prefs.edit().putString(Keys.APPEARANCE_MODE, value.name).apply()
    }

    /** Builds the margins/spacing a new project should start with, converting
     * the stored millimeter values into points. */
    val defaultMargins: PageMargins
        get() = PageMargins.uniform(PhysicalMeasurementService.points(_defaultMarginsMM.value, MeasurementUnit.MILLIMETER))

    val defaultSpacing: PageSpacing
        get() = PageSpacing.uniform(PhysicalMeasurementService.points(_defaultSpacingMM.value, MeasurementUnit.MILLIMETER))

    val defaultPageSize: PageSize
        get() = PageSize.standard(_defaultPageSizePreset.value, _defaultOrientation.value)

    companion object {
        val shared: AppSettingsStore by lazy { AppSettingsStore() }
    }
}
