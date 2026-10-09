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

    private fun colorToHsl(color: Color): FloatArray {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val l = (max + min) / 2f
        val s = if (delta == 0f) 0f else delta / (1f - kotlin.math.abs(2f * l - 1f))
        val h = when {
            delta == 0f -> 0f
            max == r -> (((g - b) / delta) % 6f) * 60f
            max == g -> (((b - r) / delta) + 2f) * 60f
            else -> (((r - g) / delta) + 4f) * 60f
        }.let { if (it < 0f) it + 360f else it }
        return floatArrayOf(h, s, l)
    }

    private fun hslToColor(h: Float, s: Float, l: Float, alpha: Float = 1f): Color {
        val c = (1f - kotlin.math.abs(2f * l - 1f)) * s
        val x = c * (1f - kotlin.math.abs(((h / 60f) % 2f) - 1f))
        val m = l - c / 2f
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return Color(
            red = (r1 + m).coerceIn(0f, 1f),
            green = (g1 + m).coerceIn(0f, 1f),
            blue = (b1 + m).coerceIn(0f, 1f),
            alpha = alpha
        )
    }

    /**
     * Neutral base background color (pure black in dark, neutral gray in light).
     * Used by detail screens (Artist, Album, Playlist, Categoria) that should stay neutral.
     */
    val backgroundColor: Color
        get() = if (isDarkCompose) Color(0xFF000000) else Color(0xFFF2F2F7)

    /**
     * Material 3 Expressive tinted background.
     * Only active for Inicio, Novedades, Radio, Búsqueda, Biblioteca and Settings when Dynamic Palette is disabled.
     */
    val expressiveBackgroundColor: Color
        get() {
            if (dynamicThemeCompose) return backgroundColor
            val hsl = colorToHsl(selectedThemeColorCompose)
            val h = hsl[0]
            val s = hsl[1]
            if (s < 0.04f) return backgroundColor

            return if (isDarkCompose) {
                if (isEffectiveAmoled) Color(0xFF000000)
                else {
                    // M3 Expressive Dark Background tone (tonal level 6 ~ 6.5% lightness)
                    val bgSat = (s * 0.32f).coerceIn(0.10f, 0.28f)
                    hslToColor(h, bgSat, 0.065f)
                }
            } else {
                // M3 Expressive Light Background tone (tonal level 98 ~ 97% lightness)
                val bgSat = (s * 0.18f).coerceIn(0.06f, 0.16f)
                hslToColor(h, bgSat, 0.97f)
            }
        }

    val surfaceColor: Color
        get() {
            if (dynamicThemeCompose) {
                return if (isDarkCompose) {
                    if (isEffectiveAmoled) Color(0xFF000000) else Color(0xFF1C1C1E)
                } else Color(0xFFFFFFFF)
            }
            val hsl = colorToHsl(selectedThemeColorCompose)
            val h = hsl[0]
            val s = hsl[1]
            if (s < 0.04f) {
                return if (isDarkCompose) {
                    if (isEffectiveAmoled) Color(0xFF000000) else Color(0xFF1C1C1E)
                } else Color(0xFFFFFFFF)
            }

            return if (isDarkCompose) {
                if (isEffectiveAmoled) Color(0xFF000000)
                else {
                    // M3 Expressive Surface Container (tonal level 12 ~ 11.5% lightness)
                    val surfSat = (s * 0.26f).coerceIn(0.09f, 0.24f)
                    hslToColor(h, surfSat, 0.115f)
                }
            } else {
                // M3 Expressive Light Surface Container
                val surfSat = (s * 0.08f).coerceIn(0.02f, 0.08f)
                hslToColor(h, surfSat, 0.995f)
            }
        }

    val textColor: Color
        get() {
            if (dynamicThemeCompose) {
                return if (isDarkCompose) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
            }
            val hsl = colorToHsl(selectedThemeColorCompose)
            val h = hsl[0]
            val s = hsl[1]
            if (s < 0.04f) {
                return if (isDarkCompose) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)
            }
            return if (isDarkCompose) {
                // M3 Expressive on-surface / on-background (tonal level 95)
                val textSat = (s * 0.14f).coerceIn(0.05f, 0.16f)
                hslToColor(h, textSat, 0.95f)
            } else {
                // M3 Expressive on-surface / on-background (tonal level 12)
                val textSat = (s * 0.32f).coerceIn(0.12f, 0.32f)
                hslToColor(h, textSat, 0.12f)
            }
        }

    val subtextColor: Color
        get() {
            if (dynamicThemeCompose) {
                return if (isDarkCompose) Color(0xFFAAAAAA) else Color(0xFF636366)
            }
            val hsl = colorToHsl(selectedThemeColorCompose)
            val h = hsl[0]
            val s = hsl[1]
            if (s < 0.04f) {
                return if (isDarkCompose) Color(0xFFAAAAAA) else Color(0xFF636366)
            }
            return if (isDarkCompose) {
                // M3 Expressive on-surface-variant (tonal level 72)
                val subSat = (s * 0.18f).coerceIn(0.07f, 0.20f)
                hslToColor(h, subSat, 0.72f)
            } else {
                // M3 Expressive on-surface-variant (tonal level 42)
                val subSat = (s * 0.26f).coerceIn(0.10f, 0.28f)
                hslToColor(h, subSat, 0.42f)
            }
        }

    val dividerColor: Color
        get() {
            if (dynamicThemeCompose) {
                return if (isDarkCompose) Color.DarkGray.copy(alpha = 0.5f) else Color(0xFFE5E5EA)
            }
            val hsl = colorToHsl(selectedThemeColorCompose)
            val h = hsl[0]
            val s = hsl[1]
            if (s < 0.04f) {
                return if (isDarkCompose) Color.DarkGray.copy(alpha = 0.5f) else Color(0xFFE5E5EA)
            }
            return if (isDarkCompose) {
                val divSat = (s * 0.20f).coerceIn(0.08f, 0.22f)
                hslToColor(h, divSat, 0.24f, alpha = 0.55f)
            } else {
                val divSat = (s * 0.16f).coerceIn(0.05f, 0.18f)
                hslToColor(h, divSat, 0.88f)
            }
        }

    val glassContainerColor: Color
        get() = if (isDarkCompose) {
            if (isEffectiveAmoled) Color(0xFF000000).copy(alpha = 0.85f) else surfaceColor.copy(alpha = 0.82f)
        } else surfaceColor.copy(alpha = 0.90f)

    val accentColor: Color
        get() = if (dynamicThemeCompose) DefaultThemeColor else selectedThemeColorCompose
}
