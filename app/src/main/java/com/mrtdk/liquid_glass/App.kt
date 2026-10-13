package com.mrtdk.liquid_glass

import android.app.Application
import android.content.Context
import com.echo.innertube.YouTube
import com.echo.innertube.YouTubeExtractor
import com.mrtdk.liquid_glass.data.LibraryManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class App : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        
        LibraryManager.init(this)
        com.mrtdk.liquid_glass.utils.PerformanceProfileManager.init(this)
        YouTubeExtractor.cacheDir = cacheDir

        // Pre-warm de reproducción (igual que Echo-Music): locale de sesión,
        // WebView de PoToken e InnerTubeX (cipher + player config) antes de
        // que la primera pista los necesite.
        try {
            YouTube.locale = com.echo.innertube.models.YouTubeLocale.fromSystem()
        } catch (_: Exception) {}
        try {
            com.mrtdk.liquid_glass.utils.YTPlayerUtils.initialize()
        } catch (_: Exception) {}
        try {
            com.mrtdk.liquid_glass.utils.InnerTubeXResolver.init(this)
        } catch (_: Exception) {}
        
        appScope.launch {
            try {
                YouTubeExtractor.ensureInitialized()
            } catch (_: Exception) {}
            
            try {
                if (YouTube.visitorData == null) {
                    YouTube.visitorData = YouTube.visitorData().getOrNull()
                }
            } catch (_: Exception) {}

            try {
                com.mrtdk.liquid_glass.canvas.EchoMusicCanvasProvider.prefetch()
                com.mrtdk.liquid_glass.canvas.AppleMusicTokenProvider.getToken()
            } catch (_: Exception) {}
        }
    }
}
