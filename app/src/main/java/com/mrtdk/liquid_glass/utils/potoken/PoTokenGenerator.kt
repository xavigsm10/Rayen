package com.mrtdk.liquid_glass.utils.potoken

import android.util.Log
import android.webkit.CookieManager
import com.mrtdk.liquid_glass.utils.cipher.CipherDeobfuscator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class PoTokenGenerator {
    private val TAG = "PoTokenGenerator"

    private val webViewSupported by lazy { runCatching { CookieManager.getInstance() }.isSuccess }
    private var webViewBadImpl = false // whether the system has a bad WebView implementation

    private val webPoTokenGenLock = Mutex()
    private var webPoTokenSessionId: String? = null
    private var webPoTokenStreamingPot: String? = null
    private var webPoTokenGenerator: PoTokenWebView? = null

    fun initialize() {
        if (!webViewSupported || webViewBadImpl) return
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
            try {
                webPoTokenGenLock.withLock {
                    if (webPoTokenGenerator == null) {
                        Log.d(TAG, "Pre-initializing PoTokenWebView in background...")
                        webPoTokenSessionId = "init-" + System.currentTimeMillis()
                        webPoTokenGenerator =
                            PoTokenWebView.getNewPoTokenGenerator(com.mrtdk.liquid_glass.App.context)
                        webPoTokenStreamingPot = webPoTokenGenerator!!.generatePoToken(webPoTokenSessionId!!)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pre-initialize PoTokenWebView", e)
            }
        }
    }

    fun getWebClientPoToken(videoId: String, sessionId: String): PoTokenResult? {
        Log.d(TAG, "getWebClientPoToken called: videoId=$videoId, sessionId=$sessionId")
        Log.d(TAG, "WebView state: supported=$webViewSupported, badImpl=$webViewBadImpl")
        if (!webViewSupported || webViewBadImpl) {
            Log.d(TAG, "WebView not available: supported=$webViewSupported, badImpl=$webViewBadImpl")
            return null
        }

        return try {
            Log.d(TAG, "Calling runBlocking to generate poToken (timeout=${POTOKEN_TIMEOUT_MS}ms)...")
            runBlocking {
                withTimeout(POTOKEN_TIMEOUT_MS) {
                    getWebClientPoToken(videoId, sessionId, forceRecreate = false)
                }
            }
        } catch (e: TimeoutCancellationException) {
            // El proceso sandbox del WebView puede ser podado por el SO y dejar
            // la llamada colgada para siempre. Se acota para caer a clientes sin
            // PoToken en vez de bloquear toda la ruta de reproducción.
            Log.w(TAG, "poToken generation timed out after ${POTOKEN_TIMEOUT_MS}ms; proceeding without PoToken")
            runBlocking {
                webPoTokenGenLock.withLock {
                    try {
                        withContext(Dispatchers.Main) { webPoTokenGenerator?.close() }
                    } catch (closeEx: Exception) {
                        Log.e(TAG, "Exception closing PoTokenWebView during timeout cleanup", closeEx)
                    }
                    webPoTokenGenerator = null
                    webPoTokenStreamingPot = null
                    webPoTokenSessionId = null
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "poToken generation exception: ${e.javaClass.simpleName}: ${e.message}")
            when (e) {
                is BadWebViewException -> {
                    Log.e(TAG, "Could not obtain poToken because WebView is broken")
                    webViewBadImpl = true
                    null
                }
                else -> throw e // includes PoTokenException
            }
        }
    }

    private companion object {
        // Arranque frío sano (WebView + botguard JS + generación) ~2-5s;
        // 8s da margen sin hacer esperar antes del fallback (ANDROID_VR, etc.).
        const val POTOKEN_TIMEOUT_MS = 8_000L
    }

    /**
     * @param forceRecreate whether to force the recreation of [webPoTokenGenerator], to be used in
     * case the current [webPoTokenGenerator] threw an error last time
     * [PoTokenWebView.generatePoToken] was called
     */
    private suspend fun getWebClientPoToken(videoId: String, sessionId: String, forceRecreate: Boolean): PoTokenResult {
        Log.d(TAG, "Web poToken requested: videoId=$videoId, sessionId=$sessionId")

        val (poTokenGenerator, streamingPot, hasBeenRecreated) =
            webPoTokenGenLock.withLock {
                val shouldRecreate =
                    forceRecreate || webPoTokenGenerator == null || webPoTokenGenerator!!.isExpired || webPoTokenSessionId != sessionId

                if (shouldRecreate) {
                    Log.d(TAG, "Creating new PoTokenWebView (forceRecreate=$forceRecreate)")
                    webPoTokenSessionId = sessionId

                    withContext(Dispatchers.Main) {
                        webPoTokenGenerator?.close()
                    }

                    // create a new webPoTokenGenerator
                    webPoTokenGenerator = PoTokenWebView.getNewPoTokenGenerator(com.mrtdk.liquid_glass.App.context)

                    // The streaming poToken needs to be generated exactly once before generating
                    // any other (player) tokens.
                    webPoTokenStreamingPot = webPoTokenGenerator!!.generatePoToken(webPoTokenSessionId!!)
                    Log.d(TAG, "Streaming poToken generated for sessionId=${webPoTokenSessionId?.take(20)}...")
                }

                Triple(webPoTokenGenerator!!, webPoTokenStreamingPot!!, shouldRecreate)
            }

        val playerPot = try {
            poTokenGenerator.generatePoToken(videoId)
        } catch (throwable: Throwable) {
            if (hasBeenRecreated) {
                // the poTokenGenerator has just been recreated (and possibly this is already the
                // second time we try), so there is likely nothing we can do
                throw throwable
            } else {
                // retry, this time recreating the [webPoTokenGenerator] from scratch;
                // this might happen for example if the app goes in the background and the WebView
                // content is lost
                Log.e(TAG, "Failed to obtain poToken, retrying: ${throwable.message}")
                return getWebClientPoToken(videoId = videoId, sessionId = sessionId, forceRecreate = true)
            }
        }

        Log.d(TAG, "poToken generated successfully: player=${playerPot.take(20)}..., streaming=${streamingPot.take(20)}...")

        /*
         * Content binding (igual que Echo-Music, según `get_webpo_content_binding`
         * de yt-dlp y PoTokenProviderImpl de NewPipe):
         *   - contexto GVS (el `pot=` anexado a una URL googlevideo) -> ligado a
         *     la SESIÓN (dataSyncId autenticado, visitor_data si no).
         *   - contexto PLAYER (`serviceIntegrityDimensions` en el cuerpo /player)
         *     -> ligado al id de vídeo, con WEB_REMIX devuelto a la rama de sesión.
         *
         * `streamingDataPoToken` es lo que se anexa como `pot=`, así que debe ser
         * el ligado a SESIÓN. Era `playerPot` (ligado al vídeo), al revés: con ese
         * mismatch YouTube rechaza el /player o sirve solo SABR y nada suena.
         *
         * `playerRequestPoToken` se deja deliberadamente como `streamingPot`:
         * MAIN_CLIENT es WEB_REMIX, que es justo el cliente que yt-dlp
         * special-casea a la rama de sesión también en el contexto PLAYER.
         */
        return PoTokenResult(
            playerRequestPoToken = streamingPot,
            streamingDataPoToken = streamingPot,
        )
    }
}
