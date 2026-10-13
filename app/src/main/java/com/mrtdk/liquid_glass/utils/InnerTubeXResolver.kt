package com.mrtdk.liquid_glass.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.util.Log
import com.echo.innertube.YouTube
import com.metrolist.innertubex.InnerTube
import com.metrolist.innertubex.InnerTubeLogLevel
import com.metrolist.innertubex.InnerTubeLogger
import com.metrolist.innertubex.cipher.PlayerConfigRepository
import com.metrolist.innertubex.cipher.RemotePlayerConfigStore
import com.metrolist.innertubex.cipher.YouTubeCipherService
import com.metrolist.innertubex.extraction.AudioQuality
import com.metrolist.innertubex.extraction.ContentHints
import com.metrolist.innertubex.extraction.InnerTubeExtractor
import com.metrolist.innertubex.extraction.PoTokenResult
import com.metrolist.innertubex.extraction.TokenProvider
import com.metrolist.innertubex.extraction.TokenProviderCapabilities
import com.metrolist.innertubex.extraction.YtConfigParserImpl
import com.metrolist.innertubex.extraction.generateClientPlaybackNonce
import com.metrolist.innertubex.extraction.strategy.PoTokenProviderKind
import com.metrolist.innertubex.models.YouTubeLocale
import com.mrtdk.liquid_glass.BuildConfig
import com.mrtdk.liquid_glass.utils.potoken.PoTokenGenerator
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Portado de Echo-Music (utils/InnerTubeXResolver.kt): extracción de streams
 * de YouTube a través del catálogo de clientes con benchmark en vivo y los
 * niveles de cipher de InnerTubeX.
 */
object InnerTubeXResolver {

    private const val TAG = "InnerTubeX"

    /** Un stream que InnerTubeX encontró, y la identidad que su descarga debe vestir. */
    class Extracted(
        val videoId: String,
        val url: String,
        val kbps: Int,
        val mimeType: String,
        val loudnessDb: Double?,
        val clientName: String,
        /** Id de catálogo de InnerTubeX para la variante exacta de cliente; clave de exclusiones. */
        val profileId: String,
        val headers: Map<String, String>,
    )

    private var prefs: SharedPreferences? = null
    private var poTokens: PoTokenGenerator? = null
    private var playerDir: File? = null
    private var warmup: Job? = null

    fun init(context: Context) {
        val app = context.applicationContext
        prefs = app.getSharedPreferences("innertubex_player_config", Context.MODE_PRIVATE)
        poTokens = PoTokenGenerator()
        playerDir = File(app.filesDir, "innertubex_players").apply { mkdirs() }
        scope.launch { warm(WARM_DELAY_MS) }
    }

    /**
     * Paga los costes fríos antes de que una pista los necesite: la player
     * config, el solver EJS y el WebView de BotGuard tras el PoToken de
     * WEB_REMIX. En frío ponen una pista con restricción de edad a 15-20s del
     * primer audio si se pagan en la ruta de reproducción.
     */
    private fun warm(delayMs: Long) {
        warmup?.cancel()
        warmup =
            scope.launch {
                delay(delayMs)
                val start = SystemClock.elapsedRealtime()
                runCatching {
                    innerTube.cookie = YouTube.cookie
                    innerTube.visitorData = YouTube.visitorData
                    innerTube.locale = YouTubeLocale(gl = "US", hl = "en")
                    extractor.prewarm()
                }
                    .onFailure { if (it is CancellationException) throw it }
                    .onFailure { Log.w(TAG, "InnerTubeX warm-up failed: ${it.message}") }
                    .onSuccess {
                        Log.d(TAG, "InnerTubeX warmed in ${SystemClock.elapsedRealtime() - start}ms")
                    }
            }
    }

    /**
     * Players EJS preprocesados, persistidos entre procesos. Resolver un player
     * desde cero es el paso de ~8.7s; uno preprocesado se reutiliza por versión
     * de player, así que solo el primer arranque tras rotar YouTube su player
     * lo paga.
     */
    private fun readPlayer(key: String): String? =
        playerDir?.let { File(it, key) }?.takeIf { it.isFile }?.readText()

    private fun writePlayer(key: String, value: String?) {
        val dir = playerDir ?: return
        val file = File(dir, key)
        if (value == null) {
            file.delete()
            return
        }
        val tmp = File(dir, "$key.tmp")
        tmp.writeText(value)
        tmp.renameTo(file)
        // Los players rotan cada pocos días; solo los nuevos merecen sus megabytes.
        dir
            .listFiles()
            ?.filter { !it.name.endsWith(".tmp") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEPT_PLAYERS)
            ?.forEach { it.delete() }
    }

    /** PoTokens de BotGuard acuñados en un WebView oculto; lo que WEB_REMIX necesita. */
    private val tokenProvider =
        object : TokenProvider {
            override val capabilities =
                TokenProviderCapabilities(
                    providers = setOf(PoTokenProviderKind.WEB_BOTGUARD),
                    usesWebView = true,
                )

            override suspend fun getPoToken(
                videoId: String,
                visitorData: String,
                cookie: String?
            ): PoTokenResult? =
                poTokens?.getWebClientPoToken(videoId, visitorData)?.let { token ->
                    PoTokenResult(
                        playerRequestToken = token.playerRequestPoToken,
                        streamingDataToken = token.streamingDataPoToken,
                        visitorData = visitorData,
                    )
                }

            override suspend fun close() {}
        }

    private val http =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        explicitNulls = false
                        encodeDefaults = true
                    }
                )
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 20_000
            }
            expectSuccess = false
        }

    private val logger = InnerTubeLogger { event ->
        if (event.level == InnerTubeLogLevel.DEBUG && !BuildConfig.DEBUG) return@InnerTubeLogger
        val details =
            if (event.details.isEmpty()) ""
            else
                event.details.entries.joinToString(prefix = " [", postfix = "]") { "${it.key}=${it.value}" }
        val line = "ITX ${event.tag}: ${event.message}$details"
        if (event.level == InnerTubeLogLevel.INFO || event.level == InnerTubeLogLevel.DEBUG)
            Log.d(TAG, line)
        else Log.w(TAG, line)
    }

    private val repository =
        object : PlayerConfigRepository {
            override val enabled: Boolean
                get() = prefs != null

            override val sourceUrl: String = PLAYER_CONFIG_URL
            override val defaultSourceUrl: String = PLAYER_CONFIG_URL
            override var cachedJson: String
                get() = prefs?.getString("json", "").orEmpty()
                set(value) {
                    prefs?.edit()?.putString("json", value)?.apply()
                }

            override var cachedAtMs: Long
                get() = prefs?.getLong("cached_at_ms", 0L) ?: 0L
                set(value) {
                    prefs?.edit()?.putLong("cached_at_ms", value)?.apply()
                }

            override var cachedSourceUrl: String
                get() = prefs?.getString("source_url", "").orEmpty()
                set(value) {
                    prefs?.edit()?.putString("source_url", value)?.apply()
                }

            override var cachedEtag: String
                get() = prefs?.getString("etag", "").orEmpty()
                set(value) {
                    prefs?.edit()?.putString("etag", value)?.apply()
                }
        }

    private val innerTube = InnerTube(http, logger = logger)
    private val remoteStore = RemotePlayerConfigStore(http, repository, logger)
    private val cipherService = YouTubeCipherService(http, remoteStore, logger)
    private val extractor =
        InnerTubeExtractor(
            configParser = YtConfigParserImpl(http, innerTube, remoteStore, logger),
            cipherService = cipherService,
            innerTube = innerTube,
            tokenProvider = tokenProvider,
            logger = logger,
        )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Lo que era cada URL acuñada, para vestir su descarga y atribuir su rechazo. */
    private val minted = ConcurrentHashMap<String, Extracted>()

    /** Clientes que rechazaron una pista a mitad de reproducción, por videoId y hasta cuándo. */
    private val excluded = ConcurrentHashMap<String, ConcurrentHashMap<String, Long>>()

    /**
     * InnerTubeX elige un formato por petición y no tiene techo propio de
     * bitrate: [maxKbps] solo elige entre su peldaño menor y el mejor, y
     * [requireM4a] pide su mejor AAC-en-MP4.
     *
     * @return un stream para [videoId], o null cuando cada cliente que
     *   InnerTubeX probaría está rechazado o [skipClients] lo descarta.
     */
    suspend fun extract(
        videoId: String,
        maxKbps: Int,
        skipClients: Set<String> = emptySet(),
        requireM4a: Boolean = false,
    ): Extracted? {
        innerTube.cookie = YouTube.cookie
        innerTube.visitorData = YouTube.visitorData
        innerTube.locale = YouTubeLocale(gl = "US", hl = "en")
        val stream =
            extractor.extract(
                videoId = videoId,
                hints =
                    ContentHints()
                        .withStreamCapabilities(allowHls = false, allowSabr = false, allowBoundedRange = true),
                excludedClients = excludedFor(videoId) + skipClients,
                audioQuality =
                    when {
                        requireM4a -> AudioQuality.MP4
                        maxKbps <= LOW_KBPS -> AudioQuality.LOW
                        else -> AudioQuality.AUTO
                    },
                clientPlaybackNonce = generateClientPlaybackNonce(),
            ) ?: return null
        check(stream.sabrBootstrap == null) { "SABR is not supported by this playback engine" }
        val mime = stream.mimeType.orEmpty()
        val extracted =
            Extracted(
                videoId = videoId,
                url = stream.audioUrl,
                kbps = (stream.bitrate ?: 0) / 1000,
                mimeType =
                    if (stream.codecs.isNullOrBlank()) mime else "$mime; codecs=\"${stream.codecs}\"",
                loudnessDb = stream.loudnessDb,
                clientName = stream.clientName,
                profileId = stream.profileId,
                headers = stream.headers,
            )
        if (minted.size >= MAX_REMEMBERED) minted.clear()
        minted[extracted.url] = extracted
        return extracted
    }

    /** Headers que la descarga de medios para [url] debe llevar, si InnerTubeX la acuñó. */
    fun headersFor(url: String): Map<String, String>? = minted[url]?.headers

    /**
     * Registra que googlevideo rechazó [url] a mitad de reproducción.
     *
     * @return el videoId para el que se acuñó, o null si InnerTubeX no la acuñó.
     */
    fun onRefused(url: String): String? {
        val refused = minted.remove(url) ?: return null
        exclude(refused.videoId, refused.profileId)
        // Una URL cifrada rechazada es la señal de que la cipher config remota
        // puede estar obsoleta; tiene su propio cooldown.
        scope.launch { runCatching { cipherService.refreshAfterStreamRejection() } }
        return refused.videoId
    }

    fun exclude(videoId: String, profileId: String) {
        Log.w(TAG, "InnerTubeX: $profileId refused $videoId; skipping it for this track")
        excluded.getOrPut(videoId) { ConcurrentHashMap() }[profileId] =
            SystemClock.elapsedRealtime() + EXCLUDE_MS
    }

    fun onSessionChanged() {
        excluded.clear()
        minted.clear()
        // Nueva cookie o visitor = nueva player config y nuevo binding de PoToken.
        if (prefs != null) warm(0)
    }

    private fun excludedFor(videoId: String): Set<String> {
        val entries = excluded[videoId] ?: return emptySet()
        val now = SystemClock.elapsedRealtime()
        entries.entries.removeAll { it.value <= now }
        return entries.keys.toSet()
    }

    private const val PLAYER_CONFIG_URL =
        "https://raw.githubusercontent.com/ZemerTeam/zemer-cipher/master/library/src/main/assets/player_configs.json"
    private const val LOW_KBPS = 64
    private const val EXCLUDE_MS = 10 * 60 * 1000L
    private const val MAX_REMEMBERED = 64

    /** Fuera de la ruta de arranque en frío; el primer tap en una pista rara vez es antes. */
    private const val WARM_DELAY_MS = 2_000L
    private const val KEPT_PLAYERS = 3
}
