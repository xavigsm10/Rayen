package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.roundToInt

/**
 * A Coil [Transformation] that applies an intense blur effect to an image.
 * Usa downscaling (sampling) + box-blur puro en Java.
 *
 * Mismo resultado visual que el anterior RenderScript (fondo/reflejo difuminado),
 * pero sin crear un contexto RenderScript por imagen (deprecated API 31+, costoso
 * en CPU/batería/GC y con riesgo de leak si se pasa un Activity context).
 */
class BlurTransformation(
    context: Context,
    private val radius: Float = 25f,
    private val sampling: Float = 4f
) : Transformation {

    // Usa applicationContext para no retener Activities en caché de Coil.
    private val appContext = context.applicationContext

    override val cacheKey: String = "BlurTransformation2(radius=$radius, sampling=$sampling)"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        val width = (input.width / sampling).toInt().coerceAtLeast(1)
        val height = (input.height / sampling).toInt().coerceAtLeast(1)

        val scaledBitmap = Bitmap.createScaledBitmap(input, width, height, true)
        // Box-blur rápido: 2 pasadas con radio derivado. A esta resolución
        // equivale al ScriptIntrinsicBlur(25) pero sin allocs nativas.
        val passes = 2
        val blurRadius = ((radius / 25f * 3f).roundToInt()).coerceIn(1, 3)
        var bitmap = scaledBitmap
        repeat(passes) {
            bitmap = fastBoxBlur(bitmap, blurRadius)
        }
        return bitmap
    }

    private fun fastBoxBlur(src: Bitmap, r: Int): Bitmap {
        if (r <= 0) return src
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        // Horizontal
        boxBlurPass(pixels, w, h, r, horizontal = true)
        // Vertical
        boxBlurPass(pixels, w, h, r, horizontal = false)
        val out = if (src.isMutable) src else Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(pixels, 0, w, 0, 0, w, h)
        if (out !== src && out !== scaledRef(src)) {
            // src es el bitmap escalado intermedio: reciclar para no picos de RAM.
            try { src.recycle() } catch (_: Exception) {}
        }
        return out
    }

    private fun scaledRef(@Suppress("UNUSED_PARAMETER") b: Bitmap): Bitmap? = null

    private fun boxBlurPass(pixels: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val div = r * 2 + 1
        if (horizontal) {
            for (y in 0 until h) {
                var rSum = 0; var gSum = 0; var bSum = 0
                val row = y * w
                // Ventana inicial
                for (i in -r..r) {
                    val x = i.coerceIn(0, w - 1)
                    val c = pixels[row + x]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                }
                for (x in 0 until w) {
                    val a = (pixels[row + x] ushr 24) and 0xFF
                    pixels[row + x] = (a shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                    val outX = (x - r).coerceIn(0, w - 1)
                    val inX = (x + r + 1).coerceIn(0, w - 1)
                    val outC = pixels[row + outX]
                    val inC = pixels[row + inX]
                    rSum += ((inC shr 16) and 0xFF) - ((outC shr 16) and 0xFF)
                    gSum += ((inC shr 8) and 0xFF) - ((outC shr 8) and 0xFF)
                    bSum += (inC and 0xFF) - (outC and 0xFF)
                }
            }
        } else {
            for (x in 0 until w) {
                var rSum = 0; var gSum = 0; var bSum = 0
                for (i in -r..r) {
                    val y = i.coerceIn(0, h - 1)
                    val c = pixels[y * w + x]
                    rSum += (c shr 16) and 0xFF
                    gSum += (c shr 8) and 0xFF
                    bSum += c and 0xFF
                }
                for (y in 0 until h) {
                    val idx = y * w + x
                    val a = (pixels[idx] ushr 24) and 0xFF
                    pixels[idx] = (a shl 24) or ((rSum / div) shl 16) or ((gSum / div) shl 8) or (bSum / div)
                    val outY = (y - r).coerceIn(0, h - 1)
                    val inY = (y + r + 1).coerceIn(0, h - 1)
                    val outC = pixels[outY * w + x]
                    val inC = pixels[inY * w + x]
                    rSum += ((inC shr 16) and 0xFF) - ((outC shr 16) and 0xFF)
                    gSum += ((inC shr 8) and 0xFF) - ((outC shr 8) and 0xFF)
                    bSum += (inC and 0xFF) - (outC and 0xFF)
                }
            }
        }
    }
}
