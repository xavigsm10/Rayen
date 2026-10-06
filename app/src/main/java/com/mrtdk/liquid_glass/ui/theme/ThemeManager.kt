package com.mrtdk.liquid_glass.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ThemeManager {
    private const val KEY_THEME_MODE = "app_theme_mode"
    private const val KEY_DYNAMIC_THEME = "app_dynamic_theme"
    private const val KEY_PURE_BLACK = "app_pure_black"
    private const val KEY_SELECTED_THEME_COLOR = "app_selected_theme_color"

    const val MODE_SYSTEM = "SYSTEM"
    const val MODE_DARK = "DARK"
    const val MODE_LIGHT = "LIGHT"
    const val MODE_AMOLED = "AMOLED"

    val DefaultThemeColor = Color(0xFFFA243C)

    private val _themeMode = MutableStateFlow(MODE_SYSTEM)
    val themeMode: StateFlow<String> = _themeMode

    private val _pureBlack = MutableStateFlow(false)
    val pureBlack: StateFlow<Boolean> = _pureBlack

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _isDynamicTheme = MutableStateFlow(true)
    val isDynamicTheme: StateFlow<Boolean> = _isDynamicTheme

    private val _selectedThemeColor = MutableStateFlow(DefaultThemeColor)
    val selectedThemeColor: StateFlow<Color> = _selectedThemeColor

    // Compose snapshot states to ensure immediate and reliable recomposition across all UI
    var isDarkCompose by mutableStateOf(true)
        private set
    var pureBlackCompose by mutableStateOf(false)
        private set
    var dynamicThemeCompose by mutableStateOf(true)
        private set
    var selectedThemeColorCompose by mutableStateOf(DefaultThemeColor)
        private set

    private var lastSystemDark: Boolean = false

    fun updateSystemDark(isDark: Boolean) {
        lastSystemDark = isDark
        if (_themeMode.value == MODE_SYSTEM) {
            updateEffectiveDarkMode(isDark)
        }
    }

    fun init() {
        val savedMode = LibraryManager.getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
        _themeMode.value = savedMode

        val savedPureBlack = LibraryManager.getString(KEY_PURE_BLACK, "false") == "true"
        val effectivePureBlack = savedPureBlack || savedMode == MODE_AMOLED
        _pureBlack.value = effectivePureBlack
        pureBlackCompose = effectivePureBlack

        val savedDynamic = LibraryManager.getString(KEY_DYNAMIC_THEME, "true") != "false"
        _isDynamicTheme.value = savedDynamic
        dynamicThemeCompose = savedDynamic

        val savedColorInt = LibraryManager.getString(KEY_SELECTED_THEME_COLOR, null)?.toIntOrNull()
        if (savedColorInt != null) {
            _selectedThemeColor.value = Color(savedColorInt)
            selectedThemeColorCompose = Color(savedColorInt)
        } else {
            _selectedThemeColor.value = DefaultThemeColor
            selectedThemeColorCompose = DefaultThemeColor
        }

        val isDark = when (savedMode) {
            MODE_DARK, MODE_AMOLED -> true
            MODE_LIGHT -> false
            else -> savedMode != MODE_LIGHT
        }
        _isDarkMode.value = isDark
        isDarkCompose = isDark
    }

    fun setThemeMode(mode: String, isSystemDark: Boolean = lastSystemDark) {
        _themeMode.value = mode
        LibraryManager.saveString(KEY_THEME_MODE, mode)
        if (mode == MODE_AMOLED) {
            setPureBlack(true)
        } else if (mode == MODE_LIGHT || mode == MODE_DARK) {
            setPureBlack(false)
        }
        val isDark = when (mode) {
            MODE_DARK, MODE_AMOLED -> true
            MODE_LIGHT -> false
            else -> isSystemDark
        }
        updateEffectiveDarkMode(isDark)
    }

    fun setPureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
        pureBlackCompose = enabled
        LibraryManager.saveString(KEY_PURE_BLACK, enabled.toString())
    }

    fun setDynamicTheme(enabled: Boolean) {
        _isDynamicTheme.value = enabled
        dynamicThemeCompose = enabled
        LibraryManager.saveString(KEY_DYNAMIC_THEME, enabled.toString())
    }

    fun setSelectedThemeColor(color: Color) {
        _selectedThemeColor.value = color
        selectedThemeColorCompose = color
        LibraryManager.saveString(KEY_SELECTED_THEME_COLOR, color.toArgb().toString())
    }

    fun updateEffectiveDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        isDarkCompose = isDark
    }

    fun getThemeMode(): String {
        return _themeMode.value
    }

    val isEffectiveAmoled: Boolean
        get() = isDarkCompose && (pureBlackCompose || _themeMode.value == MODE_AMOLED)

    val backgroundColor: Color
        get() = if (isDarkCompose) Color(0xFF000000) else Color(0xFFF2F2F7)

    val surfaceColor: Color
        get() = if (isDarkCompose) {
            if (isEffectiveAmoled) Color(0xFF000000) else Color(0xFF1C1C1E)
        } else Color(0xFFFFFFFF)

    val textColor: Color
        get() = if (isDarkCompose) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

    val subtextColor: Color
        get() = if (isDarkCompose) Color(0xFFAAAAAA) else Color(0xFF636366)

    val dividerColor: Color
        get() = if (isDarkCompose) Color.DarkGray.copy(alpha = 0.5f) else Color(0xFFE5E5EA)

    val glassContainerColor: Color
        get() = if (isDarkCompose) {
            if (isEffectiveAmoled) Color(0xFF000000).copy(alpha = 0.85f) else Color(0xFF1C1C1E).copy(alpha = 0.8f)
        } else Color(0xFFFFFFFF).copy(alpha = 0.9f)

    val accentColor: Color
        get() = if (dynamicThemeCompose) DefaultThemeColor else selectedThemeColorCompose
}
