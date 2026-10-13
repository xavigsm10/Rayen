package com.mrtdk.liquid_glass.playback

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import com.mrtdk.liquid_glass.utils.InnerTubeXResolver
import com.mrtdk.liquid_glass.utils.PlayerClient
import java.io.InterruptedIOException

/**
 * Portado de Echo-Music (playback/ChunkedDataSource.kt): descarga un stream
 * como una serie de rangos acotados en vez de una sola lectura abierta.
 *
 * googlevideo regula una respuesta continua hasta más o menos la velocidad de
 * reproducción: pedir una pista entera en una petición llega a ~15kB/s, justo
 * por delante del consumo. Pedir los mismos bytes como rangos acotados sirve
 * cada uno a velocidad de línea (~5.7 MB/s medidos en la misma pista, misma
 * conexión, con segundos de diferencia).
 *
 * Es la diferencia entre un reproductor capaz de construir colchón y uno que
 * solo aguanta el tipo, y por eso un stall nunca se recupera con margen: no
 * hay ancho de sobra para ponerse al día.
 *
 * Transparente para todo lo que hay encima. [CacheDataSource] ve un único
 * stream continuo de la longitud pedida. Las peticiones acotadas también se
 * trocean, así que nadie puede superar [PlayerClient.rangeBytesFor].
 */
@UnstableApi
class ChunkedDataSource(
    private val upstream: DataSource,
    private val chunkBytes: Long,
) : DataSource {

    private var baseSpec: DataSpec? = null
    private var position = 0L
    private var bytesRemaining = 0L
    private var chunkRemaining = 0L
    private var chunkOpen = false
    private var rangeBytes = chunkBytes

    /** Se activa cuando la petición no se puede mejorar y se reenvía tal cual. */
    private var passthrough = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    /**
     * El tamaño total tiene que conocerse por adelantado o no hay forma de
     * decir dónde termina el último rango. Cada URL progresiva de googlevideo
     * lo trae como `clen`, que no cuesta nada leer; lo demás se reenvía tal cual.
     */
    override fun open(dataSpec: DataSpec): Long {
        baseSpec = dataSpec
        position = dataSpec.position

        val total = dataSpec.uri.getQueryParameter("clen")?.toLongOrNull()
        if (total == null) {
            passthrough = true
            chunkOpen = true
            try {
                return upstream.open(dataSpec)
            } catch (e: Exception) {
                if (e !is InterruptedIOException) {
                    val who = dataSpec.uri.host ?: dataSpec.uri.toString().take(120)
                    Log.w(TAG, "$who refused the stream: ${e.message}")
                    report(dataSpec, e)
                }
                throw e
            }
        }

        passthrough = false
        val end =
            if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                total
            } else {
                minOf(total, position + dataSpec.length)
            }
        bytesRemaining = (end - position).coerceAtLeast(0L)
        rangeBytes = minOf(chunkBytes, PlayerClient.rangeBytesFor(dataSpec.uri.toString()))
        if (bytesRemaining > 0) openChunk()
        return bytesRemaining
    }

    private fun openChunk() {
        val length = minOf(rangeBytes, bytesRemaining)
        val spec = requireNotNull(baseSpec).buildUpon().setPosition(position).setLength(length).build()
        try {
            upstream.open(spec)
        } catch (e: Exception) {
            if (e !is InterruptedIOException) {
                Log.w(
                    TAG,
                    "range $position-${position + length - 1} refused for " +
                        "${spec.uri.getQueryParameter("c")}: ${e.message}",
                )
                report(spec, e)
            }
            throw e
        }
        chunkRemaining = length
        chunkOpen = true
    }

    /**
     * Devuelve un rechazo a quien acuñó la URL rechazada.
     * [InnerTubeXResolver.onRefused] discrimina si la URL era suya.
     */
    private fun report(spec: DataSpec, e: Exception) {
        if (e is HttpDataSource.InvalidResponseCodeException) {
            InnerTubeXResolver.onRefused(spec.uri.toString())
        }
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (passthrough) return upstream.read(buffer, offset, length)
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        // Un rango que termina antes se reabre por la parte que no llegó,
        // que es también como se avanza al siguiente rango. El límite de
        // intentos impide que un servidor que decidió no enviar nada gire aquí
        // para siempre.
        repeat(MAX_EMPTY_RANGES) {
            if (chunkRemaining == 0L) {
                closeChunk()
                openChunk()
            }
            val read = upstream.read(buffer, offset, minOf(length.toLong(), chunkRemaining).toInt())
            if (read != C.RESULT_END_OF_INPUT) {
                position += read
                chunkRemaining -= read
                bytesRemaining -= read
                return read
            }
            chunkRemaining = 0L
        }
        return C.RESULT_END_OF_INPUT
    }

    private fun closeChunk() {
        if (chunkOpen) {
            upstream.close()
            chunkOpen = false
        }
    }

    override fun getUri(): Uri? = upstream.uri ?: baseSpec?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        closeChunk()
        baseSpec = null
        bytesRemaining = 0L
        chunkRemaining = 0L
    }

    private companion object {
        const val TAG = "ChunkedDataSource"

        /** Suficiente para sobrevivir un rango truncado, no para colgarse en uno muerto. */
        const val MAX_EMPTY_RANGES = 3
    }

    /** Envuelve las fuentes de [upstream] para que todo lo abierto pase por rangos. */
    class Factory(
        private val upstream: DataSource.Factory,
        private val chunkBytes: Long,
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource =
            ChunkedDataSource(upstream.createDataSource(), chunkBytes)
    }
}
