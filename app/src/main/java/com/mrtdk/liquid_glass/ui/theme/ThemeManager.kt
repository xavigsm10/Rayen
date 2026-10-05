package com.mrtdk.liquid_glass.ui.theme

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

    fun init() {
        val savedMode = LibraryManager.getString(KEY_THEME_MODE, MODE_SYSTEM) ?: MODE_SYSTEM
        _themeMode.value = savedMode

        val savedPureBlack = LibraryManager.getString(KEY_PURE_BLACK, "false") == "true"
        _pureBlack.value = savedPureBlack || savedMode == MODE_AMOLED

        val savedDynamic = LibraryManager.getString(KEY_DYNAMIC_THEME, "true") != "false"
        _isDynamicTheme.value = savedDynamic

        val savedColorInt = LibraryManager.getString(KEY_SELECTED_THEME_COLOR, null)?.toIntOrNull()
        if (savedColorInt != null) {
            _selectedThemeColor.value = Color(savedColorInt)
        } else {
            _selectedThemeColor.value = DefaultThemeColor
        }

        _isDarkMode.value = savedMode != MODE_LIGHT
    }

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
        LibraryManager.saveString(KEY_THEME_MODE, mode)
        if (mode == MODE_AMOLED) {
            setPureBlack(true)
        } else if (mode == MODE_LIGHT || mode == MODE_DARK) {
            setPureBlack(false)
        }
    }

    fun setPureBlack(enabled: Boolean) {
        _pureBlack.value = enabled
        LibraryManager.saveString(KEY_PURE_BLACK, enabled.toString())
    }

    fun setDynamicTheme(enabled: Boolean) {
        _isDynamicTheme.value = enabled
        LibraryManager.saveString(KEY_DYNAMIC_THEME, enabled.toString())
    }

    fun setSelectedThemeColor(color: Color) {
        _selectedThemeColor.value = color
        LibraryManager.saveString(KEY_SELECTED_THEME_COLOR, color.toArgb().toString())
    }

    fun updateEffectiveDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun getThemeMode(): String {
        return _themeMode.value
    }

    val isEffectiveAmoled: Boolean
        get() = _isDarkMode.value && (_pureBlack.value || _themeMode.value == MODE_AMOLED)

    val backgroundColor: Color
        get() = if (_isDarkMode.value) Color(0xFF000000) else Color(0xFFF2F2F7)

    val surfaceColor: Color
        get() = if (_isDarkMode.value) {
            if (isEffectiveAmoled) Color(0xFF000000) else Color(0xFF1C1C1E)
        } else Color(0xFFFFFFFF)

    val textColor: Color
        get() = if (_isDarkMode.value) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

    val subtextColor: Color
        get() = if (_isDarkMode.value) Color(0xFFAAAAAA) else Color(0xFF636366)

    val dividerColor: Color
        get() = if (_isDarkMode.value) Color.DarkGray.copy(alpha = 0.5f) else Color(0xFFE5E5EA)

    val glassContainerColor: Color
        get() = if (_isDarkMode.value) {
            if (isEffectiveAmoled) Color(0xFF000000).copy(alpha = 0.85f) else Color(0xFF1C1C1E).copy(alpha = 0.8f)
        } else Color(0xFFFFFFFF).copy(alpha = 0.9f)

    val accentColor: Color
        get() = if (_isDynamicTheme.value) DefaultThemeColor else _selectedThemeColor.value
}
