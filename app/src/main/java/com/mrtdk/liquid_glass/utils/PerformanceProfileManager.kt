package com.mrtdk.liquid_glass.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PerformanceTier {
    LOW_END,
    MID_RANGE,
    HIGH_END
}

data class PerformanceConfig(
    val tier: PerformanceTier,
    val fluidScale: Float,
    val fluidBlurDp: Float,
    val lyricsBlurEnabled: Boolean,
    val maxThumbnailSizeDp: Int,
    val maxCoverArtSizeDp: Int,
    val reflectionBlurRadius: Float,
    val reflectionMeshSize: Int,
    val maxHistorySize: Int,
    val motionCoverIntervalMs: Long
)

object PerformanceProfileManager {
    private val LOW_CONFIG = PerformanceConfig(
        tier = PerformanceTier.LOW_END,
        fluidScale = 0.0f,
        fluidBlurDp = 0f,
        lyricsBlurEnabled = false,
        maxThumbnailSizeDp = 180,
        maxCoverArtSizeDp = 480,
        reflectionBlurRadius = 0f,
        reflectionMeshSize = 4,
        maxHistorySize = 50,
        motionCoverIntervalMs = 250L
    )

    private val MID_CONFIG = PerformanceConfig(
        tier = PerformanceTier.MID_RANGE,
        fluidScale = 0.50f,
        fluidBlurDp = 24f,
        lyricsBlurEnabled = true,
        maxThumbnailSizeDp = 200,
        maxCoverArtSizeDp = 600,
        reflectionBlurRadius = 55f,
        reflectionMeshSize = 8,
        maxHistorySize = 80,
        motionCoverIntervalMs = 160L
    )

    private val HIGH_CONFIG = PerformanceConfig(
        tier = PerformanceTier.HIGH_END,
        fluidScale = 0.75f,
        fluidBlurDp = 48f,
        lyricsBlurEnabled = true,
        maxThumbnailSizeDp = 280,
        maxCoverArtSizeDp = 900,
        reflectionBlurRadius = 95f,
        reflectionMeshSize = 12,
        maxHistorySize = 150,
        motionCoverIntervalMs = 90L
    )

    private val _config = MutableStateFlow(MID_CONFIG)
    val config: StateFlow<PerformanceConfig> = _config.asStateFlow()

    fun getConfig(): PerformanceConfig {
        return _config.value
    }

    fun isLowEndDevice(): Boolean = _config.value.tier == PerformanceTier.LOW_END
    fun isMidRangeDevice(): Boolean = _config.value.tier == PerformanceTier.MID_RANGE
    fun isHighEndDevice(): Boolean = _config.value.tier == PerformanceTier.HIGH_END

    fun getOptimalBackdropScale(): Float = when (_config.value.tier) {
        PerformanceTier.LOW_END -> 0.25f
        PerformanceTier.MID_RANGE -> 0.33f
        PerformanceTier.HIGH_END -> 0.50f
    }

    fun getBeyondViewportPageCount(): Int = if (isLowEndDevice()) 0 else 1

    fun init(context: Context) {
        val isCalibrated = LibraryManager.getBoolean("device_profile_calibrated", false)
        val savedTier = LibraryManager.getString("performance_tier_preference", "auto")

        val resolvedTier = if (!isCalibrated || savedTier == "auto") {
            val detected = detectTier(context)
            val tierString = when (detected) {
                PerformanceTier.LOW_END -> "low"
                PerformanceTier.MID_RANGE -> "mid"
                PerformanceTier.HIGH_END -> "high"
            }
            LibraryManager.saveString("performance_tier_preference", tierString)
            LibraryManager.saveBoolean("device_profile_calibrated", true)
            detected
        } else {
            when (savedTier) {
                "low" -> PerformanceTier.LOW_END
                "mid" -> PerformanceTier.MID_RANGE
                "high" -> PerformanceTier.HIGH_END
                else -> detectTier(context)
            }
        }
        setTier(resolvedTier)
    }

    fun setTier(tier: PerformanceTier) {
        _config.value = when (tier) {
            PerformanceTier.LOW_END -> LOW_CONFIG
            PerformanceTier.MID_RANGE -> MID_CONFIG
            PerformanceTier.HIGH_END -> HIGH_CONFIG
        }
    }

    fun detectTier(context: Context): PerformanceTier {
        if (com.mrtdk.liquid_glass.BuildConfig.IS_LITE) {
            return PerformanceTier.LOW_END
        }
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val isLowRam = am?.isLowRamDevice == true
        val cpuCores = Runtime.getRuntime().availableProcessors()

        return when {
            isLowRam || totalRamMb < 4200 || cpuCores <= 4 || Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> {
                PerformanceTier.LOW_END
            }
            totalRamMb < 7500 || cpuCores < 8 -> {
                PerformanceTier.MID_RANGE
            }
            else -> {
                PerformanceTier.HIGH_END
            }
        }
    }
}
