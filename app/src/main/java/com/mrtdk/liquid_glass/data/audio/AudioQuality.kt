package com.mrtdk.liquid_glass.data.audio

import com.mrtdk.liquid_glass.data.LibraryManager

/**
 * Configuración de calidad de audio portada de LastWave-Native.
 *
 * Fuente: LastWave-Native
 *  - data/lossless/LosslessMusicApi.kt:124-174 (constantes + getQualityAttemptOrder)
 *  - ui/settings/SettingsScreen.kt:2098-2107 (tiers mostrados)
 *  - data/local/SettingsPreferences.kt (lw_lossless_quality / lw_download_quality, default 27)
 *
 * En RayMusic (solo YouTube, sin backend addon/Tidal por ahora) los tiers
 * se conservan 1:1 para paridad de UX y se resuelven con best-effort sobre
 * los formatos InnerTube disponibles (cap de bitrate + preferencia de códec).
 * [toQualityParam] / [getQualityAttemptOrder] quedan listos para conectar
 * un backend lossless (addon) sin cambiar la UI ni las keys.
 */
object AudioQuality {
    // Idénticos a LosslessMusicApi de LastWave-Native
    const val QUALITY_DOLBY_ATMOS = 28 // Dolby Atmos Spatial Audio
    const val QUALITY_MAX_HI_RES = 27 // Up to 24-bit / 192 kHz
    const val QUALITY_HI_RES_96 = 7 // 24-bit / 96 kHz
    const val QUALITY_CD_LOSSLESS = 6 // 16-bit / 44.1 kHz FLAC
    const val QUALITY_MP3_320 = 5 // 320 kbps MP3 / AAC
    const val QUALITY_DATA_SAVER = 4 // 96 kbps HE-AAC
    const val QUALITY_YOUTUBE = -1 // YouTube Music nativo 128-256 kbps

    const val STREAMING_KEY = "audio_quality"
    const val DOWNLOAD_KEY = "download_quality"

    const val DEFAULT_STREAMING = QUALITY_MAX_HI_RES
    const val DEFAULT_DOWNLOAD = QUALITY_MAX_HI_RES

    val VALID_IDS = setOf(28, 27, 7, 6, 5, 4, -1)

    data class Tier(val id: Int, val badge: String)

    /** Mismo orden y texto que LastWave SettingsScreen Triple(id, título, badge). */
    val TIERS: List<Tier> = listOf(
        Tier(28, "ATMOS"),
        Tier(27, "24-BIT / 192k"),
        Tier(7, "24-BIT / 96k"),
        Tier(6, "16-BIT / 44.1k"),
        Tier(5, "320 kbps"),
        Tier(4, "96 kbps"),
        Tier(-1, "YOUTUBE"),
    )

    /**
     * Copia exacta de LosslessMusicApi.getQualityAttemptOrder().
     * YouTube es estricto: sin intento lossless. Atmos solo si se elige.
     * Hi-Res solo baja, nunca sube a Atmos.
     */
    fun getQualityAttemptOrder(preferred: Int): List<Int> {
        if (preferred == QUALITY_YOUTUBE) return emptyList()
        if (preferred == QUALITY_DOLBY_ATMOS) {
            return listOf(
                QUALITY_DOLBY_ATMOS,
                QUALITY_MAX_HI_RES,
                QUALITY_HI_RES_96,
                QUALITY_CD_LOSSLESS,
                QUALITY_MP3_320,
            )
        }
        if (preferred == QUALITY_MAX_HI_RES) {
            return listOf(
                QUALITY_MAX_HI_RES,
                QUALITY_HI_RES_96,
                QUALITY_CD_LOSSLESS,
                QUALITY_MP3_320,
            )
        }
        if (preferred == QUALITY_HI_RES_96) {
            return listOf(
                QUALITY_HI_RES_96,
                QUALITY_CD_LOSSLESS,
                QUALITY_MP3_320,
            )
        }
        val tiersAscending = listOf(
            QUALITY_DATA_SAVER,
            QUALITY_MP3_320,
            QUALITY_CD_LOSSLESS,
            QUALITY_HI_RES_96,
            QUALITY_MAX_HI_RES,
        )
        val index = tiersAscending.indexOf(preferred)
        if (index == -1) return listOf(QUALITY_MAX_HI_RES, QUALITY_HI_RES_96, QUALITY_CD_LOSSLESS, QUALITY_MP3_320)
        val result = mutableListOf<Int>()
        result.add(preferred)
        for (i in index + 1 until tiersAscending.size) result.add(tiersAscending[i])
        for (i in index - 1 downTo 0) {
            val tier = tiersAscending[i]
            if (tier != QUALITY_DATA_SAVER || preferred == QUALITY_DATA_SAVER) result.add(tier)
        }
        return result
    }

    /** Mapeo id -> qualityParam del addon (LastWave). Null = YouTube directo. */
    fun toQualityParam(id: Int): String? = when (id) {
        QUALITY_DOLBY_ATMOS -> "lossless" // + atmos=true
        QUALITY_MAX_HI_RES, QUALITY_HI_RES_96 -> "hi_res"
        QUALITY_CD_LOSSLESS -> "lossless"
        QUALITY_MP3_320 -> "high"
        QUALITY_DATA_SAVER -> "low"
        else -> null
    }

    fun wantsAtmos(id: Int): Boolean = id == QUALITY_DOLBY_ATMOS

    /**
     * Cap de bitrate para resolver sobre streams YouTube (bps -> kbps).
     * YouTube real da 48-256 kbps; los tiers lossless piden "lo mejor disponible"
     * hasta que exista backend addon. Data Saver fuerza lo mínimo.
     */
    fun maxBitrateKbps(id: Int): Int = when (id) {
        QUALITY_DATA_SAVER -> 96
        QUALITY_MP3_320 -> 320
        QUALITY_YOUTUBE -> 256
        else -> Int.MAX_VALUE // 6, 7, 27, 28: mejor disponible
    }

    fun isDataSaver(id: Int): Boolean = id == QUALITY_DATA_SAVER

    /** Migración de la key antigua ("auto"/"high"/"low") al int de LastWave. */
    fun fromStoredString(raw: String?, default: Int = DEFAULT_STREAMING): Int {
        if (raw == null) return default
        raw.toIntOrNull()?.let { if (it in VALID_IDS) return it }
        return when (raw.lowercase()) {
            "low" -> QUALITY_DATA_SAVER
            "high" -> QUALITY_MP3_320
            "auto" -> DEFAULT_STREAMING
            else -> default
        }
    }

    fun readStreamingQuality(): Int =
        try {
            fromStoredString(LibraryManager.getString(STREAMING_KEY, DEFAULT_STREAMING.toString()), DEFAULT_STREAMING)
        } catch (_: Exception) { DEFAULT_STREAMING }

    fun readDownloadQuality(): Int =
        try {
            fromStoredString(LibraryManager.getString(DOWNLOAD_KEY, DEFAULT_DOWNLOAD.toString()), DEFAULT_DOWNLOAD)
        } catch (_: Exception) { DEFAULT_DOWNLOAD }

    fun saveStreamingQuality(id: Int) {
        LibraryManager.saveString(STREAMING_KEY, id.toString())
    }

    fun saveDownloadQuality(id: Int) {
        LibraryManager.saveString(DOWNLOAD_KEY, id.toString())
    }

    fun badgeFor(id: Int): String = TIERS.firstOrNull { it.id == id }?.badge ?: "YOUTUBE"

    /** Etiqueta corta para la descripción del ajuste (sin Context para no acoplar). */
    fun shortLabel(id: Int): String = when (id) {
        QUALITY_DOLBY_ATMOS -> "Dolby Atmos"
        QUALITY_MAX_HI_RES -> "Hi-Res Audio (Max)"
        QUALITY_HI_RES_96 -> "Hi-Res Audio"
        QUALITY_CD_LOSSLESS -> "CD Lossless"
        QUALITY_MP3_320 -> "Standard Quality"
        QUALITY_DATA_SAVER -> "Data Saver"
        else -> "YouTube Music"
    }

    fun shortDesc(id: Int): String = when (id) {
        QUALITY_DOLBY_ATMOS -> "Spatial Immersive Audio • Master Audio"
        QUALITY_MAX_HI_RES -> "Up to 24-bit / 192 kHz • Lossless Studio FLAC"
        QUALITY_HI_RES_96 -> "24-bit / 96 kHz • Lossless Studio FLAC"
        QUALITY_CD_LOSSLESS -> "16-bit / 44.1 kHz • Lossless CD FLAC"
        QUALITY_MP3_320 -> "320 kbps • MP3 / AAC"
        QUALITY_DATA_SAVER -> "96 kbps • High Efficiency AAC"
        else -> "128-256 kbps • YouTube Music AAC / Opus stream"
    }
}
