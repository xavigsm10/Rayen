package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Portado del índice comunitario a proveedor propio de Rayen:
 * `canción + artista -> vídeo en loop`, publicado como un único JSON.
 *
 * Es la SEGUNDA OPCIÓN de portadas animadas: solo se consulta cuando los
 * proveedores principales fallan, y está diseñado para no consumir recursos:
 * - Un único fetch para todo el índice, retenido 30 min (luego lookups locales).
 * - Si el fetch falla se sirve lo ya retenido en vez de reintentar.
 * - Timeouts cortos (3s) y sin WebView, sin auth, sin polling.
 * - Los resultados (hit o miss) pasan por la caché de UnifiedCanvasProvider.
 */
object RayenCanvasProvider {

    private const val MANIFEST = "https://vivimusicanvas.mkmdevilmi.workers.dev/canvas.json"
    private const val TTL_MS = 30L * 60 * 1000
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/122.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private data class Entry(
        val song: String,
        val artist: String,
        val album: String,
        val url: String,
    )

    @Volatile private var entries: List<Entry> = emptyList()
    @Volatile private var fetchedAtMs = 0L

    suspend fun getBySongArtist(
        song: String,
        artist: String,
        album: String? = null
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (song.isBlank() || artist.isBlank()) return@withContext null
        val index = manifest().ifEmpty { return@withContext null }

        val wantTitle = song.normalizeForCanvasMatch()
        val wantArtist = artist.normalizeForCanvasMatch()
        val wantAlbum = album?.normalizeForCanvasMatch()

        // Los contribuidores escriben los títulos a su manera, así que aquí se
        // iguala por contención y no por igualdad; el álbum (si se conoce) es
        // lo que mantiene honesto el match.
        val hit = index.firstOrNull { entry ->
            val listed = entry.song.normalizeForCanvasMatch()
            val credited = entry.artist.normalizeForCanvasMatch()
            val listedAlbum = entry.album.normalizeForCanvasMatch()
            val titleOk = listed.isNotBlank() &&
                (wantTitle.contains(listed) || listed.contains(wantTitle))
            val artistOk = credited.isNotBlank() &&
                (wantArtist.contains(credited) || credited.contains(wantArtist))
            val albumOk = listedAlbum.isBlank() || wantAlbum.isNullOrBlank() || listedAlbum == wantAlbum
            titleOk && artistOk && albumOk &&
                CanvasArtwork.isValidVideoUrl(entry.url)
        } ?: return@withContext null

        CanvasArtwork(
            name = hit.song,
            artist = hit.artist,
            albumName = hit.album.takeIf { it.isNotBlank() },
            videoUrl = hit.url,
            animated = hit.url
        )
    }

    /**
     * Clip contribuido para un lanzamiento, para la página del álbum. El índice
     * va por canción, así que se toma la primera pista cubierta del álbum.
     */
    suspend fun getByAlbumArtist(
        album: String,
        artist: String
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (album.isBlank() || artist.isBlank()) return@withContext null
        val index = manifest().ifEmpty { return@withContext null }

        val wantAlbum = album.normalizeForCanvasMatch()
        val wantArtist = artist.normalizeForCanvasMatch()
        if (wantAlbum.isBlank()) return@withContext null

        val hit = index.firstOrNull { entry ->
            val listed = entry.album.normalizeForCanvasMatch()
            val credited = entry.artist.normalizeForCanvasMatch()
            listed == wantAlbum && credited.isNotBlank() &&
                (wantArtist.contains(credited) || credited.contains(wantArtist)) &&
                CanvasArtwork.isValidVideoUrl(entry.url)
        } ?: return@withContext null

        CanvasArtwork(
            name = hit.album,
            artist = hit.artist,
            albumName = hit.album,
            videoUrl = hit.url,
            animated = hit.url
        )
    }

    @Synchronized
    private fun manifest(): List<Entry> {
        val now = System.currentTimeMillis()
        if (entries.isNotEmpty() && now - fetchedAtMs < TTL_MS) return entries

        val body = runCatching {
            val quickClient = CanvasNetworkClient.okHttpClient.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(MANIFEST).header("User-Agent", UA).build()
            quickClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        }.getOrNull()
        if (body == null) {
            // Servir lo ya retenido en vez de perder el canvas media hora
            // porque un fetch falló.
            fetchedAtMs = now
            return entries
        }

        val parsed = runCatching {
            json.parseToJsonElement(body).jsonObject["items"]?.jsonArray
                ?.mapNotNull { item ->
                    val obj = item.jsonObject
                    val url = obj["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    Entry(
                        song = obj["song"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                        artist = obj["artist"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                        album = obj["album"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        url = url
                    )
                }
        }.getOrNull().orEmpty()

        if (parsed.isNotEmpty()) entries = parsed
        fetchedAtMs = now
        return entries
    }

    /**
     * Mayúsculas, tildes y puntuación difieren entre servicios para el mismo
     * lanzamiento: se pliegan antes de comparar.
     */
    private fun String.normalizeForCanvasMatch(): String =
        Normalizer.normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
