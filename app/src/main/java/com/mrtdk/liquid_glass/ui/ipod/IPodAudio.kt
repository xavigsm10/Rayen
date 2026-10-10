package com.mrtdk.liquid_glass.ui.ipod

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.view.View

/**
 * High-performance, ultra low-latency synthesized mechanical click sound and haptic pulse generator
 * faithfully replicating the iconic iPod Click Wheel feedback.
 */
object IPodAudio {
    private var clickTrack: AudioTrack? = null
    private var isInitialized = false
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        if (isInitialized) return
        try {
            // Setup vibrator
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            // Synthesize crisp mechanical click PCM: 4ms exponential decaying tick at 3200Hz
            val sampleRate = 44100
            val durationMs = 0.0035 // 3.5ms
            val numSamples = (sampleRate * durationMs).toInt()
            val generatedSnd = ByteArray(2 * numSamples)
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val decay = Math.exp(-t * 2200.0) // sharp mechanical transient
                val freq = 3100.0
                val angle = 2.0 * Math.PI * freq * t
                val sample = (Math.sin(angle) * decay * 28000.0).toInt().coerceIn(-32767, 32767).toShort()
                generatedSnd[2 * i] = (sample.toInt() and 0x00ff).toByte()
                generatedSnd[2 * i + 1] = ((sample.toInt() and 0xff00) shr 8).toByte()
            }

            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val format = AudioFormat.Builder()
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .build()

            clickTrack = AudioTrack(
                attributes,
                format,
                generatedSnd.size,
                AudioTrack.MODE_STATIC,
                android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
            ).apply {
                write(generatedSnd, 0, generatedSnd.size)
            }
            isInitialized = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Triggered on every rotary step of the Click Wheel.
     */
    fun tick(view: View?, playAudio: Boolean = true, triggerHaptic: Boolean = true) {
        if (playAudio) {
            try {
                clickTrack?.let {
                    it.stop()
                    it.reloadStaticData()
                    it.play()
                } ?: run {
                    view?.playSoundEffect(SoundEffectConstants.CLICK)
                }
            } catch (_: Exception) {
                view?.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }

        if (triggerHaptic) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(VibrationEffect.createOneShot(8, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
            } catch (_: Exception) {
                view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    /**
     * Triggered on physical quadrant/center button press.
     */
    fun buttonClick(view: View?, playAudio: Boolean = true) {
        tick(view, playAudio, triggerHaptic = true)
    }

    fun release() {
        try {
            clickTrack?.release()
            clickTrack = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
