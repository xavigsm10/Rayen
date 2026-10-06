package com.mrtdk.liquid_glass.canvas

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Request

@Serializable
data class EchoMusicCanvasManifest(
    val items: List<EchoMusicCanvasItem> = emptyList()
)

@Serializable
data class EchoMusicCanvasItem(
    val song: String = "",
    val artist: String = "",
    val url: String = ""
)

object EchoMusicCanvasProvider {
    private const val BASE_URL = "https://canvas.echomusic.fun/canvas.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private data class CacheEntry(
        val value: EchoMusicCanvasManifest?,
        val expiresAtMs: Long,
    )

    private var manifestCache: CacheEntry? = null
    private const val TTL_MS = 1000L * 60 * 60 * 24 // 24 hours

    private suspend fun fetchManifest(): EchoMusicCanvasManifest? = withContext(Dispatchers.IO) {
        val currentCache = manifestCache
        if (currentCache != null && currentCache.expiresAtMs > System.currentTimeMillis()) {
            return@withContext currentCache.value
        }

        try {
            val quickClient = CanvasNetworkClient.okHttpClient.newBuilder()
                .connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(2, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(BASE_URL).build()
            val response = quickClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val manifest = json.decodeFromString<EchoMusicCanvasManifest>(body)
                manifestCache = CacheEntry(
                    value = manifest,
                    expiresAtMs = System.currentTimeMillis() + TTL_MS
                )
                manifest
            } else {
                manifestCache = CacheEntry(null, System.currentTimeMillis() + 1000L * 60 * 30)
                null
            }
        } catch (e: Exception) {
            manifestCache = CacheEntry(null, System.currentTimeMillis() + 1000L * 60 * 30)
            null
        }
    }

    suspend fun prefetch() {
        fetchManifest()
    }

    suspend fun getBySongArtist(
        song: String,
        artist: String,
    ): CanvasArtwork? = withContext(Dispatchers.IO) {
        if (song.isBlank() || artist.isBlank()) return@withContext null

        val manifest = fetchManifest() ?: return@withContext null

        val cleanSong = song.lowercase().trim()
        val cleanArtist = artist.lowercase().trim()

        val target = manifest.items.firstOrNull { item ->
            val itemSong = item.song.lowercase().trim()
            val itemArtist = item.artist.lowercase().trim()
            if (itemSong.isBlank() || itemArtist.isBlank()) return@firstOrNull false

            val matchSong = if (cleanSong.length <= 3 || itemSong.length <= 3) {
                cleanSong == itemSong
            } else {
                cleanSong.contains(itemSong) || itemSong.contains(cleanSong)
            }
            val matchArtist = if (cleanArtist.length <= 3 || itemArtist.length <= 3) {
                cleanArtist == itemArtist
            } else {
                cleanArtist.contains(itemArtist) || itemArtist.contains(cleanArtist)
            }
            matchSong && matchArtist && CanvasArtwork.isValidVideoUrl(item.url)
        }

        if (target != null && target.url.isNotBlank()) {
            CanvasArtwork(
                name = target.song,
                artist = target.artist,
                videoUrl = target.url,
                animated = target.url
            )
        } else {
            null
        }
    }
}
