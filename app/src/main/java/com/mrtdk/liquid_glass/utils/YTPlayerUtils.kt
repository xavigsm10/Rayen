package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.echo.innertube.NewPipeExtractor
import com.echo.innertube.YouTube
import com.echo.innertube.YouTubeExtractor
import com.echo.innertube.models.WatchEndpoint
import com.echo.innertube.models.YouTubeClient
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_CREATOR
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_43_32
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_61_48
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_1_65_10
import com.echo.innertube.models.YouTubeClient.Companion.ANDROID_VR_NO_AUTH
import com.echo.innertube.models.YouTubeClient.Companion.IOS
import com.echo.innertube.models.YouTubeClient.Companion.IPADOS
import com.echo.innertube.models.YouTubeClient.Companion.MOBILE
import com.echo.innertube.models.YouTubeClient.Companion.TVHTML5
import com.echo.innertube.models.YouTubeClient.Companion.TVHTML5_SIMPLY_EMBEDDED_PLAYER
import com.echo.innertube.models.YouTubeClient.Companion.VISIONOS
import com.echo.innertube.models.YouTubeClient.Companion.WEB
import com.echo.innertube.models.YouTubeClient.Companion.WEB_CREATOR
import com.echo.innertube.models.YouTubeClient.Companion.WEB_REMIX
import com.echo.innertube.models.response.PlayerResponse
import com.mrtdk.liquid_glass.utils.potoken.PoTokenGenerator
import com.mrtdk.liquid_glass.utils.potoken.PoTokenResult
import com.mrtdk.liquid_glass.jiosaavn.SaavnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object YTPlayerUtils {
    private const val TAG = "YTPlayerUtils"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .proxy(YouTube.proxy)
        .proxyAuthenticator { _, response ->
            YouTube.proxyAuth?.let { auth ->
                response.request.newBuilder()
                    .header("Proxy-Authorization", auth)
                    .build()
            } ?: response.request
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .fastFallback(true)
        .build()

    private val poTokenGenerator = PoTokenGenerator()

    /**
     * Primary client for YouTube playback streams.
     * VISIONOS and ANDROID_VR_1_65_10 provide direct unencrypted streaming URLs without 403 bot blocks
     * and serve complete files without preview truncation.
     */
    private val MAIN_CLIENT: YouTubeClient = VISIONOS

    private val STREAM_FALLBACK_CLIENTS: Array<YouTubeClient> = arrayOf(
        VISIONOS,
        ANDROID_VR_1_65_10,
        TVHTML5,
        ANDROID_VR_1_43_32,
        IPADOS,
        IOS,
        ANDROID_CREATOR,
        ANDROID_VR_NO_AUTH,
        MOBILE,
        WEB_REMIX,
        WEB_CREATOR
    )

    data class PlaybackData(
        val format: PlayerResponse.StreamingData.Format?,
        val streamUrl: String,
        val expiresInSeconds: Int = 21600,
        val headers: Map<String, String>? = null
    )

    fun init(context: Context) {
        YouTubeExtractor.cacheDir = context.cacheDir
        InnerTubeXResolver.init(context)
    }

    /**
     * Pre-crea el WebView de PoToken en segundo plano (igual que Echo-Music):
     * evita pagar el arranque en frío en la primera reproducción.
     */
    fun initialize() {
        poTokenGenerator.initialize()
    }

    /**
     * Vía principal estilo Echo-Music: InnerTubeX (catálogo de clientes con
     * benchmark en vivo + cipher + PoToken). Devuelve la URL con los headers
     * con los que hay que vestir la descarga, o null para seguir al fallback.
     */
    suspend fun resolveInnerTubeX(
        videoId: String,
        qualityIdOverride: Int? = null
    ): PlaybackData? = withContext(Dispatchers.IO) {
        val AQ = com.mrtdk.liquid_glass.data.audio.AudioQuality
        val qid = qualityIdOverride ?: try { AQ.readStreamingQuality() } catch (_: Exception) { AQ.DEFAULT_STREAMING }
        // Echo usa maxKbps=160 fijo; Data Saver pide el peldaño LOW (<=64).
        val maxKbps = if (qid == AQ.QUALITY_DATA_SAVER) 64 else 160
        val extracted = try {
            InnerTubeXResolver.extract(videoId = videoId, maxKbps = maxKbps, requireM4a = false)
        } catch (e: Exception) {
            Log.w(TAG, "InnerTubeX extract failed for $videoId: ${e.message}")
            null
        } ?: return@withContext null
        Log.d(TAG, "InnerTubeX hit for $videoId via ${extracted.clientName} (${extracted.kbps}kbps)")
        PlaybackData(
            format = null,
            streamUrl = extracted.url,
            expiresInSeconds = 21600,
            headers = extracted.headers
        )
    }

    /** Headers para vestir la descarga de [url]: InnerTubeX, o el cliente que la acuñó. */
    fun streamHeadersFor(url: String): Map<String, String> =
        InnerTubeXResolver.headersFor(url)
            ?: PlayerClient.forStreamUrl(url).mediaHeaders()

    /**
     * Registra un 403 de googlevideo: excluye el cliente 10 min y refresca el
     * cipher. Igual que Echo-Music (InnerTubeXResolver.onRefused).
     *
     * @return el videoId afectado, o null si la URL no la acuñó InnerTubeX.
     */
    fun reportRefusedStream(url: String): String? = InnerTubeXResolver.onRefused(url)

    /**
     * Resolves the exact audio stream URL, vía principal Echo-Music primero:
     * - Tier 0: InnerTubeX (catálogo con benchmark en vivo + cipher + PoToken).
     * - Tier 1: Direct YouTube InnerTube Playback (legacy).
     * - Tier 2: Bot Detection Mitigation & Session Rotation.
     * - Tier 3: NewPipeExtractor direct stream resolution.
     * - Tier 4: Lossless 320kbps Matcher fallback.
     * - Tier 5: Resilient Piped API direct stream resolution.
     * - Tier 6: Resilient Invidious API stream resolution.
     */
    suspend fun resolveStreamUrl(
        videoId: String,
        preferLow: Boolean = false,
        playlistId: String? = null,
        qualityIdOverride: Int? = null
    ): String? = withContext(Dispatchers.IO) {
        // ── Tier 0: InnerTubeX (Echo-Music) ──────────────────────────────────
        val itx = resolveInnerTubeX(videoId, qualityIdOverride)
        if (!itx?.streamUrl.isNullOrBlank()) {
            BotDetectionMitigator.notifyPlaybackSuccess()
            return@withContext itx!!.streamUrl
        }
        // ── Tier 1: Direct YouTube InnerTube Playback Resolution (<150ms) ─────
        val firstAttempt = resolvePlaybackData(videoId, preferLow, playlistId, qualityIdOverride)
        if (firstAttempt.isSuccess) {
            val url = firstAttempt.getOrNull()?.streamUrl
            if (!url.isNullOrBlank()) {
                BotDetectionMitigator.notifyPlaybackSuccess()
                return@withContext url
            }
        }

        // ── Tier 2: Bot Detection Mitigation & Session Rotation ───────────────
        if (YouTube.cookie == null) {
            Log.w(TAG, "Tier 1 playback failed for $videoId. Rotating guest session and retrying...")
            BotDetectionMitigator.rotateGuestSession()
            val retryAttempt = resolvePlaybackData(videoId, preferLow, playlistId, qualityIdOverride)
            if (retryAttempt.isSuccess) {
                val url = retryAttempt.getOrNull()?.streamUrl
                if (!url.isNullOrBlank()) {
                    BotDetectionMitigator.notifyPlaybackSuccess()
                    return@withContext url
                }
            }
        }

        // ── Tier 3: NewPipeExtractor Direct Stream Extraction (ViMusic Engine) ─
        try {
            val npStreams = NewPipeExtractor.newPipePlayer(videoId)
            val bestAudioStream = npStreams.firstOrNull { it.first in listOf(251, 140, 250, 249) }?.second
                ?: npStreams.firstOrNull()?.second
            if (!bestAudioStream.isNullOrBlank() && validateStatus(bestAudioStream)) {
                Log.d(TAG, "Tier 3 (NewPipeExtractor) stream resolved for $videoId")
                return@withContext bestAudioStream
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tier 3 NewPipeExtractor failed for $videoId: ${e.message}")
        }

        // ── Tier 4: Lossless 320kbps Matcher Fallback ─────────────────────────
        try {
            val losslessUrl = resolveLosslessStreamStrict(videoId, qualityIdOverride)
            if (!losslessUrl.isNullOrBlank()) {
                Log.d(TAG, "Tier 4 (Lossless 320kbps) hit for $videoId")
                return@withContext losslessUrl
            }
        } catch (e: Exception) {
            Log.d(TAG, "Tier 4 lossless matcher skipped for $videoId: ${e.message}")
        }

        // ── Tier 5: Resilient Piped API Direct Stream Extraction ──────────────
        Log.w(TAG, "InnerTube and NewPipe failed. Trying Piped API stream resolution for $videoId...")
        val pipedUrl = resolvePipedStream(videoId)
        if (!pipedUrl.isNullOrBlank()) {
            return@withContext pipedUrl
        }

        // ── Tier 6: Resilient Invidious API Direct Stream Extraction ──────────
        Log.w(TAG, "Piped failed. Trying Invidious API stream resolution for $videoId...")
        val invidiousUrl = resolveInvidiousStream(videoId)
        if (!invidiousUrl.isNullOrBlank()) {
            return@withContext invidiousUrl
        }

        firstAttempt.getOrNull()?.streamUrl
    }

    /**
     * Strict verification matcher matching ViviMusic's lossless engine.
     * Only returns the stream if:
     * 1. The title normalized matches the target song title
     * 2. The artist normalized matches the target artist
     * 3. The duration is within +/- 4 seconds
     * Returns null immediately if not strictly matching, falling back to YouTube InnerTube.
     */
    private suspend fun resolveLosslessStreamStrict(videoId: String, qualityIdOverride: Int? = null): String? = coroutineScope {
        val songItem = try {
            val nextResult = YouTube.next(WatchEndpoint(videoId = videoId)).getOrNull()
            nextResult?.items?.getOrNull(nextResult.currentIndex ?: 0)
                ?: nextResult?.items?.firstOrNull()
        } catch (_: Exception) { null } ?: return@coroutineScope null

        val title = songItem.title?.trim().orEmpty()
        val artists = songItem.artists?.map { it.name.trim() } ?: emptyList()
        val expectedDuration = songItem.duration

        if (title.isBlank() || expectedDuration == null || expectedDuration <= 0) {
            return@coroutineScope null
        }

        fun clean(text: String): String {
            return text.lowercase(java.util.Locale.ROOT)
                .replace(Regex("""\((?:official|music|video|audio|lyrics|remix|feat\.?|ft\.?).*?\)"""), "")
                .replace(Regex("""\[.*?\]"""), "")
                .replace(Regex("""[^a-z0-9\s]"""), " ")
                .replace(Regex("""\s+"""), " ")
                .trim()
        }

        val cleanWantedTitle = clean(title)
        val cleanWantedArtists = artists.map { clean(it) }.filter { it.isNotBlank() }

        val query = if (artists.isNotEmpty()) "$title ${artists.first()}" else title
        val candidates = SaavnService.searchSongs(query).getOrNull() ?: return@coroutineScope null

        val strictMatch = candidates.firstOrNull { candidate ->
            val candidateDuration = candidate.duration ?: return@firstOrNull false
            if (kotlin.math.abs(candidateDuration - expectedDuration) > 4) {
                return@firstOrNull false
            }

            val cleanCandTitle = clean(candidate.name)
            val titleMatch = cleanCandTitle == cleanWantedTitle ||
                    (cleanWantedTitle.length >= 4 && cleanCandTitle.contains(cleanWantedTitle)) ||
                    (cleanCandTitle.length >= 4 && cleanWantedTitle.contains(cleanCandTitle))

            if (!titleMatch) return@firstOrNull false

            val candArtists = candidate.artists.primary.map { clean(it.name) }
            val artistMatch = cleanWantedArtists.isEmpty() ||
                    candArtists.any { ca -> cleanWantedArtists.any { wa -> ca.contains(wa) || wa.contains(ca) } }

            artistMatch
        }

        if (strictMatch != null) {
            val AQ = com.mrtdk.liquid_glass.data.audio.AudioQuality
            val qid = qualityIdOverride ?: try { AQ.readStreamingQuality() } catch (_: Exception) { AQ.DEFAULT_STREAMING }
            val target = when (qid) {
                AQ.QUALITY_DATA_SAVER -> "96kbps"
                AQ.QUALITY_MP3_320 -> "160kbps"
                else -> "320kbps"
            }
            // Data Saver pide exacta 96k; el resto intenta su target y cae a 320k dentro de selectBestUrl.
            val streamUrl = SaavnService.selectBestUrl(strictMatch.downloadUrl, target)
            if (!streamUrl.isNullOrBlank()) {
                val artistNames = strictMatch.artists.primary.joinToString { it.name }
                Log.i(TAG, "Lossless 320kbps verified exact match for $videoId: '${strictMatch.name}' by '$artistNames'")
                return@coroutineScope streamUrl
            }
        }

        null
    }

    suspend fun resolvePlaybackData(
        videoId: String,
        preferLow: Boolean = false,
        playlistId: String? = null,
        qualityIdOverride: Int? = null
    ): Result<PlaybackData> = runCatching {
        Log.d(TAG, "Resolving YouTube playback data for videoId=$videoId, playlistId=$playlistId")

        val isLoggedIn = YouTube.cookie != null
        val signatureTimestamp = try {
            NewPipeExtractor.getSignatureTimestamp(videoId).getOrNull()
        } catch (_: Exception) { null }

        var poToken: PoTokenResult? = null
        val sessionId = if (isLoggedIn) YouTube.dataSyncId else YouTube.visitorData
        if (MAIN_CLIENT.useWebPoTokens && sessionId != null) {
            try {
                poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
            } catch (e: Exception) {
                Log.e(TAG, "PoToken generation failed for MAIN_CLIENT: ${e.message}")
            }
        }

        var mainPlayerResponse: PlayerResponse? = try {
            YouTube.player(videoId, playlistId, MAIN_CLIENT, signatureTimestamp, poToken?.playerRequestPoToken).getOrNull()
        } catch (e: Exception) {
            Log.w(TAG, "Main client ${MAIN_CLIENT.clientName} failed: ${e.message}")
            null
        }

        var usedAgeRestrictedClient: YouTubeClient? = null
        val mainStatus = mainPlayerResponse?.playabilityStatus?.status
        val isAgeRestricted = mainStatus in listOf(
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED"
        )
        val wasOriginallyAgeRestricted = isAgeRestricted

        if (isAgeRestricted && isLoggedIn) {
            Log.d(TAG, "Age-restricted detected, using WEB_CREATOR")
            val creatorResponse = YouTube.player(videoId, playlistId, WEB_CREATOR, null, null).getOrNull()
            if (creatorResponse?.playabilityStatus?.status == "OK") {
                mainPlayerResponse = creatorResponse
                usedAgeRestrictedClient = WEB_CREATOR
            }
        }

        var format: PlayerResponse.StreamingData.Format? = null
        var streamUrl: String? = null
        var streamExpiresInSeconds: Int? = null
        var bestCandidateUrl: String? = null
        var bestCandidateFormat: PlayerResponse.StreamingData.Format? = null

        val startIndex = if (isAgeRestricted) 0 else -1

        for (clientIndex in startIndex until STREAM_FALLBACK_CLIENTS.size) {
            val client: YouTubeClient
            val streamPlayerResponse: PlayerResponse?

            if (clientIndex == -1) {
                client = MAIN_CLIENT
                streamPlayerResponse = mainPlayerResponse
            } else {
                client = STREAM_FALLBACK_CLIENTS[clientIndex]
                if (client.loginRequired && !isLoggedIn && YouTube.cookie == null) {
                    continue
                }

                if (client.useWebPoTokens && poToken == null && sessionId != null) {
                    try {
                        poToken = poTokenGenerator.getWebClientPoToken(videoId, sessionId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Lazy PoToken generation failed for ${client.clientName}: ${e.message}")
                    }
                }

                val clientPoToken = if (client.useWebPoTokens) poToken?.playerRequestPoToken else null
                val clientSigTimestamp = if (wasOriginallyAgeRestricted || !client.useSignatureTimestamp) null else signatureTimestamp
                streamPlayerResponse = YouTube.player(videoId, playlistId, client, clientSigTimestamp, clientPoToken).getOrNull()
            }

            if (streamPlayerResponse?.playabilityStatus?.status == "OK") {
                val foundFormat = findFormat(streamPlayerResponse, preferLow, qualityIdOverride) ?: continue
                var currentUrl = findUrlOrNull(foundFormat, videoId, streamPlayerResponse, skipNewPipe = wasOriginallyAgeRestricted) ?: continue

                val currentClient = if (clientIndex == -1) (usedAgeRestrictedClient ?: MAIN_CLIENT) else STREAM_FALLBACK_CLIENTS[clientIndex]

                // Apply n-transform ONLY for web clients that require it
                if (currentClient.useWebPoTokens) {
                    try {
                        val transformed = YouTubeExtractor.deobfuscateUrlNParam(currentUrl)
                        if (transformed != currentUrl) {
                            currentUrl = transformed
                            Log.d(TAG, "YouTubeExtractor n-transform applied successfully for ${currentClient.clientName}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "YouTubeExtractor n-transform failed: ${e.message}")
                    }

                    // Apply PoToken parameter if web client (Base64 direct, do NOT Uri.encode)
                    if (poToken?.streamingDataPoToken != null) {
                        val separator = if ("?" in currentUrl) "&" else "?"
                        currentUrl = "${currentUrl}${separator}pot=${poToken.streamingDataPoToken}"
                    }
                }

                bestCandidateUrl = currentUrl
                bestCandidateFormat = foundFormat

                // For main client or last fallback client: skip validation for instant start without consuming tokens
                if (clientIndex == -1 || clientIndex == STREAM_FALLBACK_CLIENTS.size - 1) {
                    Log.d(TAG, "Selected stream client without probe: ${currentClient.clientName}")
                    format = foundFormat
                    streamUrl = currentUrl
                    streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600
                    break
                }

                // Pre-validation for fallback web clients to ensure working link
                if (validateStatus(currentUrl, foundFormat.contentLength, currentClient.clientName)) {
                    Log.d(TAG, "Stream validated OK with client: ${currentClient.clientName}")
                    format = foundFormat
                    streamUrl = currentUrl
                    streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600
                    break
                } else {
                    Log.w(TAG, "Stream validation probe failed (403/invalid) for client: ${currentClient.clientName}")
                    if (currentClient.useWebPoTokens) {
                        try {
                            val nTransformed = YouTubeExtractor.deobfuscateUrlNParam(currentUrl)
                            if (nTransformed != currentUrl && validateStatus(nTransformed, foundFormat.contentLength, currentClient.clientName)) {
                                Log.d(TAG, "Alternate n-transform validated OK!")
                                format = foundFormat
                                streamUrl = nTransformed
                                streamExpiresInSeconds = streamPlayerResponse.streamingData?.expiresInSeconds ?: 21600
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
            } else {
                val status = streamPlayerResponse?.playabilityStatus?.status
                val reason = streamPlayerResponse?.playabilityStatus?.reason
                Log.d(TAG, "Client ${client.clientName} failed with status=$status, reason=$reason")
            }
        }

        // Fallback to NewPipe direct extractor if all InnerTube clients were rejected
        if (streamUrl == null) {
            Log.w(TAG, "All InnerTube clients failed. Trying NewPipe direct stream extraction for $videoId...")
            try {
                val npStreams = NewPipeExtractor.newPipePlayer(videoId)
                val bestAudio = npStreams.firstOrNull { it.first == 251 }?.second
                    ?: npStreams.firstOrNull { it.first == 140 }?.second
                    ?: npStreams.firstOrNull()?.second
                if (!bestAudio.isNullOrBlank() && validateStatus(bestAudio)) {
                    Log.d(TAG, "NewPipe direct stream extraction successful!")
                    streamUrl = bestAudio
                }
            } catch (e: Exception) {
                Log.e(TAG, "NewPipe direct fallback error: ${e.message}")
            }
        }

        // Final fallback to best candidate URL
        if (streamUrl == null && bestCandidateUrl != null) {
            streamUrl = bestCandidateUrl
            format = bestCandidateFormat
            Log.d(TAG, "Using best candidate stream URL")
        }

        if (streamUrl == null) {
            throw IllegalStateException("Failed to obtain playback stream for videoId=$videoId")
        }

        PlaybackData(
            format = format,
            streamUrl = streamUrl,
            expiresInSeconds = streamExpiresInSeconds ?: 21600
        )
    }

    private fun findFormat(
        playerResponse: PlayerResponse,
        preferLow: Boolean,
        qualityIdOverride: Int? = null
    ): PlayerResponse.StreamingData.Format? {
        val adaptiveFormats = playerResponse.streamingData?.adaptiveFormats ?: return null
        val formats = adaptiveFormats.filter { it.isAudio && it.isOriginal }
            .ifEmpty { adaptiveFormats.filter { it.isAudio } }
            .ifEmpty { return null }

        // Paridad LastWave-Native: tiers 28/27/7/6/5/4/-1 (AudioQuality).
        // Sin backend lossless, se resuelve best-effort sobre YouTube con cap por tier.
        val qualityId = qualityIdOverride ?: try {
            com.mrtdk.liquid_glass.data.audio.AudioQuality.readStreamingQuality()
        } catch (_: Exception) {
            com.mrtdk.liquid_glass.data.audio.AudioQuality.DEFAULT_STREAMING
        }
        val isLow = preferLow || com.mrtdk.liquid_glass.data.audio.AudioQuality.isDataSaver(qualityId)
        if (isLow) {
            return formats.minByOrNull {
                it.bitrate - (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
            }
        }
        val capBps = com.mrtdk.liquid_glass.data.audio.AudioQuality.maxBitrateKbps(qualityId) * 1000L
        val inCap = if (capBps >= Int.MAX_VALUE) formats else formats.filter { it.bitrate <= capBps }
        val pool = inCap.ifEmpty {
            // Nada bajo el cap (YouTube real > cap): lo más cercano por encima.
            listOf(formats.minByOrNull { it.bitrate }!!)
        }
        // Igual que LastWave en playback: mejor bitrate, bonus a Opus/WebM.
        return pool.maxByOrNull {
            it.bitrate + (if (it.mimeType.startsWith("audio/webm")) 10240 else 0)
        }
    }

    private fun findUrlOrNull(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        playerResponse: PlayerResponse,
        skipNewPipe: Boolean = false
    ): String? {
        // 1. Direct format URL
        if (!format.url.isNullOrBlank()) {
            return format.url
        }

        // 2. Custom JS deobfuscation with Mozilla Rhino for signatureCipher
        val cipherStr = format.signatureCipher
        if (!cipherStr.isNullOrBlank()) {
            try {
                val decrypted = YouTubeExtractor.decryptUrl(cipherStr)
                if (!decrypted.isNullOrBlank()) {
                    return decrypted
                }
            } catch (e: Exception) {
                Log.e(TAG, "Rhino JS decrypt failed: ${e.message}")
            }
        }

        // 3. Fallback to NewPipeExtractor if not age-restricted
        if (!skipNewPipe) {
            try {
                val npUrl = NewPipeExtractor.getStreamUrl(format, videoId)
                if (!npUrl.isNullOrBlank()) {
                    return npUrl
                }
            } catch (e: Exception) {
                Log.e(TAG, "NewPipe extractor fallback failed: ${e.message}")
            }
        }

        return null
    }

    private suspend fun resolvePipedStream(videoId: String): String? = withContext(Dispatchers.IO) {
        val instances = listOf(
            "https://pipedapi.kavin.rocks",
            "https://api.piped.privacydev.net",
            "https://piped-api.garudalinux.org",
            "https://yt.drgnz.club",
            "https://pa.il.ax",
            "https://pipedapi.tokhmi.xyz"
        )

        for (instance in instances) {
            try {
                val url = "$instance/streams/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = org.json.JSONObject(body)
                        val audioStreams = json.optJSONArray("audioStreams") ?: return@use

                        var bestUrl: String? = null
                        var highestBitrate = 0
                        for (i in 0 until audioStreams.length()) {
                            val stream = audioStreams.getJSONObject(i)
                            val streamUrl = stream.optString("url")
                            val bitrate = stream.optInt("bitrate", 0)
                            if (streamUrl.isNotBlank() && bitrate > highestBitrate) {
                                highestBitrate = bitrate
                                bestUrl = streamUrl
                            }
                        }
                        if (!bestUrl.isNullOrBlank() && validateStatus(bestUrl)) {
                            Log.d(TAG, "Successfully resolved stream via Piped ($instance) for $videoId")
                            return@withContext bestUrl
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Piped instance $instance failed for $videoId: ${e.message}")
            }
        }
        null
    }

    private suspend fun resolveInvidiousStream(videoId: String): String? = withContext(Dispatchers.IO) {
        val instances = listOf(
            "https://yewtu.be",
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.jing.rocks",
            "https://vid.puffyan.us"
        )

        for (instance in instances) {
            try {
                val url = "$instance/api/v1/videos/$videoId"
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                    .get()
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return@use
                        val json = org.json.JSONObject(body)
                        val adaptiveFormats = json.optJSONArray("adaptiveFormats") ?: return@use

                        var bestUrl: String? = null
                        var highestBitrate = 0
                        for (i in 0 until adaptiveFormats.length()) {
                            val format = adaptiveFormats.getJSONObject(i)
                            val type = format.optString("type", "")
                            if (type.startsWith("audio/")) {
                                val streamUrl = format.optString("url")
                                val bitrate = format.optInt("bitrate", 0)
                                if (streamUrl.isNotBlank() && bitrate > highestBitrate) {
                                    highestBitrate = bitrate
                                    bestUrl = streamUrl
                                }
                            }
                        }
                        if (!bestUrl.isNullOrBlank() && validateStatus(bestUrl)) {
                            Log.d(TAG, "Successfully resolved stream via Invidious ($instance) for $videoId")
                            return@withContext bestUrl
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Invidious instance $instance failed for $videoId: ${e.message}")
            }
        }
        null
    }

    /**
     * Valida una URL de stream (igual que Echo-Music): cuando se conoce el
     * `contentLength` del formato se sondea el ÚLTIMO byte del archivo — una
     * URL que sirve su byte final no es un preview truncado. La sonda de solo
     * primer chunk acepta streams capados a ~60s que mueren a mitad.
     * Se envían los headers del cliente que acuñó la URL + Cookie, sin los
     * cuales googlevideo rechaza sondas válidas.
     */
    private fun validateStatus(
        urlStr: String,
        contentLength: Long? = null,
        label: String = ""
    ): Boolean {
        return try {
            // Último byte si se conoce el tamaño, si no el primer chunk que pedirá ExoPlayer.
            val range = if (contentLength != null && contentLength > 0) {
                "bytes=${contentLength - 1}-${contentLength - 1}"
            } else {
                "bytes=0-${512 * 1024 - 1}"
            }
            val requestBuilder = Request.Builder()
                .head()
                .url(urlStr)
                .header("Range", range)
            val extraHeaders = try {
                InnerTubeXResolver.headersFor(urlStr)
                    ?: PlayerClient.forStreamUrl(urlStr).mediaHeaders()
            } catch (_: Exception) { emptyMap<String, String>() }
            for ((k, v) in extraHeaders) {
                requestBuilder.header(k, v)
            }
            YouTube.cookie?.let { cookie -> requestBuilder.addHeader("Cookie", cookie) }

            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val accepted = response.isSuccessful || response.code == 405
                if (!accepted) {
                    Log.w(TAG, "Stream URL REJECTED: code=${response.code} range=$range $label")
                }
                accepted
            }
        } catch (e: java.io.IOException) {
            // Network timeout / reset while HEAD-probing. Stream itself may still be fine; accept optimistically
            true
        } catch (_: Exception) {
            false
        }
    }
}
