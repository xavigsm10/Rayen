package com.mrtdk.liquid_glass.ui.ipod

import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.liquid_glass.data.ItemType
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.LibraryManager
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.lyrics.LyricLine
import com.mrtdk.liquid_glass.data.lyrics.LrcLib
import com.mrtdk.liquid_glass.playback.MusicPlayer
import com.mrtdk.liquid_glass.playback.PlaybackQueue
import com.mrtdk.liquid_glass.ui.screens.PlayerState
import com.mrtdk.liquid_glass.ui.screens.QueueItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Chassis styling definitions for the physical iPod body.
 */
data class IPodChassisTheme(
    val bodyBrush: Brush,
    val outerBorder: Color,
    val screenBezelColor: Color,
    val accentTextColor: Color,
    val wheelColorStyle: String
)

object IPodChassisThemes {
    val Silver = IPodChassisTheme(
        bodyBrush = Brush.linearGradient(
            listOf(
                Color(0xFFE5E8ED),
                Color(0xFFD6DBE2),
                Color(0xFFCCD1DA),
                Color(0xFFDCE0E7)
            )
        ),
        outerBorder = Color(0xFF9EA4AF),
        screenBezelColor = Color(0xFF2E323A),
        accentTextColor = Color(0xFF424752),
        wheelColorStyle = "silver"
    )

    val White = IPodChassisTheme(
        bodyBrush = Brush.linearGradient(
            listOf(
                Color(0xFFFAFAFA),
                Color(0xFFF2F4F7),
                Color(0xFFE8EBEF),
                Color(0xFFFFFFFF)
            )
        ),
        outerBorder = Color(0xFFC4C8CE),
        screenBezelColor = Color(0xFF1E2024),
        accentTextColor = Color(0xFF5A606C),
        wheelColorStyle = "white"
    )

    val Black = IPodChassisTheme(
        bodyBrush = Brush.linearGradient(
            listOf(
                Color(0xFF1C1D21),
                Color(0xFF25262B),
                Color(0xFF18191C),
                Color(0xFF222328)
            )
        ),
        outerBorder = Color(0xFF0F1012),
        screenBezelColor = Color(0xFF0C0D0E),
        accentTextColor = Color(0xFFB0B4C0),
        wheelColorStyle = "black"
    )

    val U2 = IPodChassisTheme(
        bodyBrush = Brush.linearGradient(
            listOf(
                Color(0xFF121214),
                Color(0xFF18181A),
                Color(0xFF0D0D0E),
                Color(0xFF1A1A1D)
            )
        ),
        outerBorder = Color(0xFF050506),
        screenBezelColor = Color(0xFF000000),
        accentTextColor = Color(0xFFD41A24),
        wheelColorStyle = "u2"
    )

    fun forStyle(style: String): IPodChassisTheme {
        return when (style.lowercase()) {
            "white" -> White
            "black" -> Black
            "u2" -> U2
            else -> Silver
        }
    }
}

/**
 * Master iPod Container: Transforms the entire phone screen into an authentic fullscreen iPod,
 * with edge-to-edge hardware texture, embedded LCD screen, 3D Cover Flow for all views,
 * synchronized lyrics, and the tactile Click Wheel.
 */
@Composable
fun IPodContainer(
    musicPlayer: MusicPlayer?,
    playerState: PlayerState?,
    onSongSelected: (PlayerState) -> Unit,
    onSongSelectedFromQueue: (PlayerState) -> Unit = onSongSelected,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onExitIpodMode: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    // Appearance State from LibraryManager
    val ipodColorStyle by LibraryManager.ipodColorStyle.collectAsState()
    val ipodWheelSound by LibraryManager.ipodWheelSound.collectAsState()
    val chassisTheme = remember(ipodColorStyle) { IPodChassisThemes.forStyle(ipodColorStyle) }

    // Playback state
    val isPlaying by (musicPlayer?.isPlaying?.collectAsState() ?: remember { mutableStateOf(false) })
    val currentPosition by (musicPlayer?.currentPosition?.collectAsState() ?: remember { mutableStateOf(0L) })
    val duration by (musicPlayer?.duration?.collectAsState() ?: remember { mutableStateOf(0L) })

    // Library Data
    val savedItems by LibraryManager.savedItems.collectAsState()
    val playlists by LibraryManager.playlists.collectAsState()
    val downloadedSongs by LibraryManager.downloadedSongs.collectAsState()
    val recentlyPlayed by LibraryManager.recentlyPlayed.collectAsState()

    // Live PlaybackQueue observer
    var queueUpdateTick by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        val prevCallback = PlaybackQueue.onQueueChanged
        PlaybackQueue.onQueueChanged = {
            prevCallback?.invoke()
            queueUpdateTick++
        }
        onDispose {
            PlaybackQueue.onQueueChanged = prevCallback
        }
    }

    // Dynamic queue menu items (Active song + PlaybackQueue.queue + PlaybackQueue.upNextSongs)
    val queueMenuItems = remember(queueUpdateTick, playerState, PlaybackQueue.queue, PlaybackQueue.upNextSongs) {
        val list = mutableListOf<IPodMenuItem>()

        if (playerState != null) {
            list.add(
                IPodMenuItem(
                    id = "queue_now_playing",
                    title = "▶ ${playerState.title}",
                    subtitle = "Reproduciendo ahora • ${playerState.artist}",
                    hasSubmenu = false,
                    artworkUrl = playerState.artUrl?.toString(),
                    extraData = playerState
                )
            )
        }

        PlaybackQueue.queue.forEachIndexed { index, qItem ->
            val num = index + 1
            list.add(
                IPodMenuItem(
                    id = "queue_q_${qItem.videoId ?: index}_$index",
                    title = "$num. ${qItem.title}",
                    subtitle = qItem.artist,
                    hasSubmenu = false,
                    artworkUrl = qItem.artUrl?.toString(),
                    extraData = qItem
                )
            )
        }

        val baseIndex = PlaybackQueue.queue.size
        PlaybackQueue.upNextSongs.forEachIndexed { index, songItem ->
            val num = baseIndex + index + 1
            list.add(
                IPodMenuItem(
                    id = "queue_upnext_${songItem.id}_$index",
                    title = "$num. ${songItem.title}",
                    subtitle = songItem.artists.joinToString { it.name },
                    hasSubmenu = false,
                    artworkUrl = songItem.thumbnail,
                    extraData = songItem
                )
            )
        }

        list
    }

    // Dynamic queue 3D Cover Flow cards representing all tracks in the playback queue
    val queueCoverFlowCards = remember(queueUpdateTick, playerState, PlaybackQueue.queue, PlaybackQueue.upNextSongs) {
        val list = mutableListOf<CoverFlowCard>()

        if (playerState != null) {
            list.add(
                CoverFlowCard(
                    id = "qcf_now_playing",
                    title = playerState.title,
                    subtitle = playerState.artist,
                    imageUrl = playerState.artUrl?.toString(),
                    icon = Icons.Rounded.PlayArrow,
                    gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)),
                    badge = "EN REPRODUCCIÓN",
                    isPlaying = true,
                    extraData = playerState,
                    targetDestination = IPodScreenDestination.NowPlaying
                )
            )
        }

        PlaybackQueue.queue.forEachIndexed { index, qItem ->
            val num = index + 1
            list.add(
                CoverFlowCard(
                    id = "qcf_queue_${qItem.videoId ?: index}_$index",
                    title = qItem.title,
                    subtitle = qItem.artist,
                    imageUrl = qItem.artUrl?.toString(),
                    icon = Icons.Rounded.MusicNote,
                    gradientColors = listOf(Color(0xFF283593), Color(0xFF1A237E)),
                    badge = "#$num EN COLA",
                    isPlaying = false,
                    extraData = qItem,
                    targetDestination = IPodScreenDestination.NowPlaying
                )
            )
        }

        val baseIndex = PlaybackQueue.queue.size
        PlaybackQueue.upNextSongs.forEachIndexed { index, songItem ->
            val num = baseIndex + index + 1
            list.add(
                CoverFlowCard(
                    id = "qcf_upnext_${songItem.id}_$index",
                    title = songItem.title,
                    subtitle = songItem.artists.joinToString { it.name },
                    imageUrl = songItem.thumbnail,
                    icon = Icons.Rounded.MusicNote,
                    gradientColors = listOf(Color(0xFF37474F), Color(0xFF212121)),
                    badge = "#$num A CONTINUACIÓN",
                    isPlaying = false,
                    extraData = songItem,
                    targetDestination = IPodScreenDestination.NowPlaying
                )
            )
        }

        list
    }

    // Lyrics State
    var songLyrics by remember { mutableStateOf<List<LyricLine>?>(null) }
    LaunchedEffect(playerState?.title, playerState?.artist) {
        val title = playerState?.title
        val artist = playerState?.artist
        if (!title.isNullOrBlank() && !artist.isNullOrBlank()) {
            val dur = if (duration > 0) duration else 180000L
            songLyrics = runCatching { LrcLib.lyrics(title, artist, dur) }.getOrNull()
        } else {
            songLyrics = null
        }
    }

    // Search query state
    var searchQuery by remember { mutableStateOf("") }

    // Navigation Stack
    val backStack = remember { mutableStateListOf<IPodScreenDestination>(IPodScreenDestination.MainMenu) }
    val currentDestination = backStack.lastOrNull() ?: IPodScreenDestination.MainMenu

    // Per-destination selection index mapping
    val selectionIndices = remember { mutableMapOf<String, Int>() }
    var currentSelectedIndex by remember { mutableIntStateOf(0) }

    // Update currentSelectedIndex whenever currentDestination changes
    LaunchedEffect(currentDestination) {
        val key = currentDestination.title
        currentSelectedIndex = selectionIndices[key] ?: 0
    }

    // Now Playing modes
    var isScrubbingMode by remember { mutableStateOf(false) }

    // Volume HUD state
    var showVolumeHud by remember { mutableStateOf(false) }
    var volumeHudJob by remember { mutableStateOf<Job?>(null) }
    var currentVolumeLevel by remember {
        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15
        val currVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 8
        mutableFloatStateOf(currVol.toFloat() / maxVol.toFloat())
    }

    // ── 3D COVER FLOW CARDS FOR ALL APP VIEWS ─────────────────────────────────
    val viewCoverFlowCards = remember(playerState?.artUrl, playerState?.title) {
        listOf(
            CoverFlowCard(
                id = "cf_inicio",
                title = "Inicio",
                subtitle = "Recomendaciones y mezclas",
                icon = Icons.Rounded.Home,
                gradientColors = listOf(Color(0xFFE91E63), Color(0xFFFF5722)),
                targetDestination = IPodScreenDestination.InicioMenu
            ),
            CoverFlowCard(
                id = "cf_music",
                title = "Música",
                subtitle = "Biblioteca y colecciones",
                icon = Icons.Rounded.MusicNote,
                gradientColors = listOf(Color(0xFF3F51B5), Color(0xFF2196F3)),
                targetDestination = IPodScreenDestination.MusicMenu
            ),
            CoverFlowCard(
                id = "cf_albums",
                title = "Álbumes",
                subtitle = "Tus discos y carátulas",
                icon = Icons.Rounded.Album,
                gradientColors = listOf(Color(0xFF9C27B0), Color(0xFF673AB7)),
                targetDestination = IPodScreenDestination.AlbumsList
            ),
            CoverFlowCard(
                id = "cf_artists",
                title = "Artistas",
                subtitle = "Catálogo de artistas",
                icon = Icons.Rounded.Person,
                gradientColors = listOf(Color(0xFF009688), Color(0xFF4CAF50)),
                targetDestination = IPodScreenDestination.ArtistsList
            ),
            CoverFlowCard(
                id = "cf_songs",
                title = "Canciones",
                subtitle = "Todas las canciones",
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                gradientColors = listOf(Color(0xFFFF9800), Color(0xFFFFC107)),
                targetDestination = IPodScreenDestination.SongsList
            ),
            CoverFlowCard(
                id = "cf_playlists",
                title = "Playlists",
                subtitle = "Listas de reproducción",
                icon = Icons.Rounded.Folder,
                gradientColors = listOf(Color(0xFF00BCD4), Color(0xFF03A9F4)),
                targetDestination = IPodScreenDestination.PlaylistsList
            ),
            CoverFlowCard(
                id = "cf_novedades",
                title = "Novedades",
                subtitle = "Nuevos lanzamientos",
                icon = Icons.Rounded.NewReleases,
                gradientColors = listOf(Color(0xFFE040FB), Color(0xFF7C4DFF)),
                targetDestination = IPodScreenDestination.NovedadesMenu
            ),
            CoverFlowCard(
                id = "cf_radio",
                title = "Radio",
                subtitle = "Estaciones continuas",
                icon = Icons.Rounded.Radio,
                gradientColors = listOf(Color(0xFFFF5252), Color(0xFFFF4081)),
                targetDestination = IPodScreenDestination.RadioMenu
            ),
            CoverFlowCard(
                id = "cf_now_playing",
                title = "Reproduciendo",
                subtitle = playerState?.title ?: "Pista actual",
                imageUrl = playerState?.artUrl?.toString(),
                icon = if (playerState?.artUrl == null) Icons.Rounded.PlayArrow else null,
                gradientColors = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)),
                targetDestination = IPodScreenDestination.NowPlaying
            ),
            CoverFlowCard(
                id = "cf_lyrics",
                title = "Letras",
                subtitle = "Letras sincronizadas",
                icon = Icons.Rounded.Lyrics,
                gradientColors = listOf(Color(0xFF6A1B9A), Color(0xFF4A148C)),
                targetDestination = IPodScreenDestination.LyricsScreen
            ),
            CoverFlowCard(
                id = "cf_queue",
                title = "Cola",
                subtitle = "Cola de reproducción",
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                gradientColors = listOf(Color(0xFF0277BD), Color(0xFF01579B)),
                targetDestination = IPodScreenDestination.QueueScreen
            ),
            CoverFlowCard(
                id = "cf_search",
                title = "Buscar",
                subtitle = "Buscar en biblioteca",
                icon = Icons.Rounded.Search,
                gradientColors = listOf(Color(0xFF37474F), Color(0xFF263238)),
                targetDestination = IPodScreenDestination.SearchScreen
            ),
            CoverFlowCard(
                id = "cf_settings",
                title = "Ajustes",
                subtitle = "Estilo y preferencias",
                icon = Icons.Rounded.Settings,
                gradientColors = listOf(Color(0xFF455A64), Color(0xFF1C2833)),
                targetDestination = IPodScreenDestination.SettingsMenu
            )
        )
    }

    // ── 3D COVER FLOW CARDS FOR LIBRARY ALBUMS ─────────────────────────────────
    // ── 3D COVER FLOW CARDS FOR LIBRARY ALBUMS ─────────────────────────────────
    val albumCoverFlowCards = remember(savedItems, downloadedSongs, recentlyPlayed) {
        val albums = (savedItems + downloadedSongs + recentlyPlayed)
            .filter { !it.album.isNullOrBlank() }
            .distinctBy { it.album }

        albums.map { item ->
            CoverFlowCard(
                id = "album_cf_${item.album}",
                title = item.album.orEmpty(),
                subtitle = item.subtitle,
                imageUrl = item.thumbnail,
                icon = Icons.Rounded.Album,
                gradientColors = listOf(Color(0xFF3A4250), Color(0xFF1B1E24)),
                targetDestination = IPodScreenDestination.AlbumDetail(item.album.orEmpty())
            )
        }
    }

    // Helper: Push next destination
    val navigateTo: (IPodScreenDestination) -> Unit = { dest ->
        selectionIndices[currentDestination.title] = currentSelectedIndex
        backStack.add(dest)
        currentSelectedIndex = 0
    }

    // Helper: Navigate back
    val navigateBack: () -> Unit = {
        if (backStack.size > 1) {
            val popped = backStack.removeAt(backStack.lastIndex)
            val newDest = backStack.last()
            currentSelectedIndex = selectionIndices[newDest.title] ?: 0
        } else {
            onExitIpodMode()
        }
    }

    // Hardware back button returns through iPod menu hierarchy
    BackHandler {
        if (backStack.size > 1) {
            navigateBack()
        } else {
            onExitIpodMode()
        }
    }

    // Helper: Play LibraryItem with upcoming queue
    val playTrackWithQueue: (LibraryItem, List<LibraryItem>) -> Unit = { selectedTrack, trackList ->
        val selectedIndex = trackList.indexOfFirst { it.id == selectedTrack.id }
        val remainingTracks = if (selectedIndex >= 0) trackList.drop(selectedIndex + 1) else emptyList()
        val remainingQueue = remainingTracks.map { track ->
            QueueItem(
                title = track.title,
                artist = track.subtitle,
                artUrl = track.thumbnail,
                videoId = track.id,
                album = track.album
            )
        }
        val newState = PlayerState(
            title = selectedTrack.title,
            artist = selectedTrack.subtitle,
            artUrl = selectedTrack.thumbnail,
            videoId = selectedTrack.id,
            album = selectedTrack.album,
            queue = remainingQueue
        )
        PlaybackQueue.queue = remainingQueue
        onSongSelected(newState)
        navigateTo(IPodScreenDestination.NowPlaying)
    }

    // Helper: Play single LibraryItem
    val playItem: (LibraryItem) -> Unit = { item ->
        val newState = PlayerState(
            title = item.title,
            artist = item.subtitle,
            artUrl = item.thumbnail,
            videoId = item.id,
            album = item.album
        )
        onSongSelected(newState)
        navigateTo(IPodScreenDestination.NowPlaying)
    }

    // Compute menu items for current destination
    val menuItems = remember(currentDestination, savedItems, playlists, downloadedSongs, recentlyPlayed, ipodColorStyle, ipodWheelSound, playerState, searchQuery, queueMenuItems) {
        when (currentDestination) {
            is IPodScreenDestination.MainMenu -> {
                val list = mutableListOf(
                    IPodMenuItem(id = "coverflow_views", title = "Cover Flow", subtitle = "Navegar vistas en 3D", hasSubmenu = true),
                    IPodMenuItem(id = "music", title = "Música", subtitle = "Biblioteca completa", hasSubmenu = true),
                    IPodMenuItem(id = "inicio", title = "Inicio", subtitle = "Mixes y recomendaciones", hasSubmenu = true),
                    IPodMenuItem(id = "novedades", title = "Novedades", subtitle = "Últimos lanzamientos", hasSubmenu = true),
                    IPodMenuItem(id = "radio", title = "Radio", subtitle = "Estaciones", hasSubmenu = true)
                )
                if (playerState != null) {
                    list.add(IPodMenuItem(id = "now_playing", title = "Reproduciendo", subtitle = playerState.title, hasSubmenu = true, artworkUrl = playerState.artUrl?.toString()))
                    list.add(IPodMenuItem(id = "lyrics", title = "Letras", subtitle = "Letras sincronizadas", hasSubmenu = true))
                    val qSize = queueMenuItems.size
                    list.add(IPodMenuItem(id = "queue", title = "Cola de reproducción", subtitle = if (qSize > 0) "$qSize canciones" else "Cola vacía", hasSubmenu = true))
                }
                list.add(IPodMenuItem(id = "search", title = "Buscar", subtitle = "Buscar pistas", hasSubmenu = true))
                list.add(IPodMenuItem(id = "shuffle", title = "Aleatorio", subtitle = "Mezclar canciones", hasSubmenu = false))
                list.add(IPodMenuItem(id = "settings", title = "Ajustes", subtitle = "Preferencias", hasSubmenu = true))
                list
            }

            is IPodScreenDestination.MusicMenu -> {
                val qSize = queueMenuItems.size
                listOf(
                    IPodMenuItem(id = "coverflow_albums", title = "Álbumes en 3D", subtitle = "Cover Flow", hasSubmenu = true),
                    IPodMenuItem(id = "queue", title = "Cola de reproducción", subtitle = if (qSize > 0) "$qSize canciones" else "Cola vacía", hasSubmenu = true),
                    IPodMenuItem(id = "playlists", title = "Playlists", hasSubmenu = true),
                    IPodMenuItem(id = "artists", title = "Artistas", hasSubmenu = true),
                    IPodMenuItem(id = "albums", title = "Álbumes", hasSubmenu = true),
                    IPodMenuItem(id = "songs", title = "Canciones", hasSubmenu = true),
                    IPodMenuItem(id = "history", title = "Historial", hasSubmenu = true),
                    IPodMenuItem(id = "downloads", title = "Descargados", hasSubmenu = true)
                )
            }

            is IPodScreenDestination.InicioMenu -> {
                val songs = (savedItems.filter { it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                    .distinctBy { it.id }
                    .take(20)
                if (songs.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_inicio", title = "Sin canciones recomendadas", hasSubmenu = false))
                } else {
                    songs.map { song ->
                        IPodMenuItem(
                            id = "inicio_${song.id}",
                            title = song.title,
                            subtitle = song.subtitle,
                            hasSubmenu = false,
                            artworkUrl = song.thumbnail,
                            extraData = song
                        )
                    }
                }
            }

            is IPodScreenDestination.NovedadesMenu -> {
                val newItems = (savedItems.filter { it.type == ItemType.ALBUM || it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                    .distinctBy { it.id }
                    .take(15)
                if (newItems.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_nov", title = "Sin novedades recientes", hasSubmenu = false))
                } else {
                    newItems.map { itm ->
                        IPodMenuItem(
                            id = "nov_${itm.id}",
                            title = itm.title,
                            subtitle = itm.subtitle,
                            hasSubmenu = itm.type == ItemType.ALBUM,
                            artworkUrl = itm.thumbnail,
                            extraData = if (itm.type == ItemType.ALBUM) itm.title else itm
                        )
                    }
                }
            }

            is IPodScreenDestination.RadioMenu -> {
                val artists = (savedItems + downloadedSongs + recentlyPlayed)
                    .map { it.subtitle.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .take(15)
                if (artists.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_radio", title = "Sin estaciones", hasSubmenu = false))
                } else {
                    artists.map { artistName ->
                        IPodMenuItem(
                            id = "radio_$artistName",
                            title = "Radio de $artistName",
                            subtitle = "Mix continuo",
                            hasSubmenu = false,
                            extraData = artistName
                        )
                    }
                }
            }

            is IPodScreenDestination.PlaylistsList -> {
                val list = mutableListOf<IPodMenuItem>()
                playlists.forEach { pl ->
                    list.add(
                        IPodMenuItem(
                            id = "playlist_${pl.id}",
                            title = pl.name,
                            subtitle = "${pl.items.size} canciones",
                            hasSubmenu = true,
                            artworkUrl = pl.coverUrl ?: pl.items.firstOrNull()?.thumbnail,
                            extraData = pl
                        )
                    )
                }
                if (list.isEmpty()) {
                    list.add(IPodMenuItem(id = "empty_pl", title = "Sin playlists", hasSubmenu = false))
                }
                list
            }

            is IPodScreenDestination.PlaylistDetail -> {
                val pl = currentDestination.playlist
                if (pl.items.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_tracks", title = "Playlist vacía", hasSubmenu = false))
                } else {
                    pl.items.mapIndexed { index, item ->
                        IPodMenuItem(
                            id = "track_${item.id}_$index",
                            title = item.title,
                            subtitle = item.subtitle,
                            hasSubmenu = false,
                            artworkUrl = item.thumbnail,
                            extraData = item
                        )
                    }
                }
            }

            is IPodScreenDestination.ArtistsList -> {
                val allArtists = (savedItems + downloadedSongs + recentlyPlayed)
                    .filter { it.subtitle.isNotBlank() }
                    .map { it.subtitle.trim() }
                    .distinct()
                    .sorted()

                if (allArtists.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_artists", title = "Sin artistas", hasSubmenu = false))
                } else {
                    allArtists.map { artistName ->
                        val thumb = (savedItems + downloadedSongs + recentlyPlayed).firstOrNull { it.subtitle == artistName }?.thumbnail
                        IPodMenuItem(
                            id = "artist_$artistName",
                            title = artistName,
                            hasSubmenu = true,
                            artworkUrl = thumb,
                            extraData = artistName
                        )
                    }
                }
            }

            is IPodScreenDestination.ArtistDetail -> {
                val artistName = currentDestination.artistName
                val songs = (savedItems + downloadedSongs + recentlyPlayed)
                    .filter { it.subtitle.equals(artistName, ignoreCase = true) }
                    .distinctBy { it.id }

                if (songs.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_artist_songs", title = "Sin canciones", hasSubmenu = false))
                } else {
                    songs.map { song ->
                        IPodMenuItem(
                            id = "song_${song.id}",
                            title = song.title,
                            subtitle = song.album ?: artistName,
                            hasSubmenu = false,
                            artworkUrl = song.thumbnail,
                            extraData = song
                        )
                    }
                }
            }

            is IPodScreenDestination.AlbumsList -> {
                val allAlbums = (savedItems + downloadedSongs + recentlyPlayed)
                    .filter { !it.album.isNullOrBlank() }
                    .map { it.album!!.trim() }
                    .distinct()
                    .sorted()

                if (allAlbums.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_albums", title = "Sin álbumes", hasSubmenu = false))
                } else {
                    allAlbums.map { albumName ->
                        val item = (savedItems + downloadedSongs + recentlyPlayed).firstOrNull { it.album == albumName }
                        IPodMenuItem(
                            id = "album_$albumName",
                            title = albumName,
                            subtitle = item?.subtitle,
                            hasSubmenu = true,
                            artworkUrl = item?.thumbnail,
                            extraData = albumName
                        )
                    }
                }
            }

            is IPodScreenDestination.AlbumDetail -> {
                val albumName = currentDestination.albumName
                val tracks = (savedItems + downloadedSongs + recentlyPlayed)
                    .filter { it.album.equals(albumName, ignoreCase = true) }
                    .distinctBy { it.id }

                if (tracks.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_album_tracks", title = "Sin pistas", hasSubmenu = false))
                } else {
                    tracks.map { track ->
                        IPodMenuItem(
                            id = "album_track_${track.id}",
                            title = track.title,
                            subtitle = track.subtitle,
                            hasSubmenu = false,
                            artworkUrl = track.thumbnail,
                            extraData = track
                        )
                    }
                }
            }

            is IPodScreenDestination.SongsList -> {
                val songs = (savedItems.filter { it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                    .distinctBy { it.id }

                if (songs.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_songs", title = "Sin canciones", hasSubmenu = false))
                } else {
                    songs.map { song ->
                        IPodMenuItem(
                            id = "song_${song.id}",
                            title = song.title,
                            subtitle = song.subtitle,
                            hasSubmenu = false,
                            artworkUrl = song.thumbnail,
                            extraData = song
                        )
                    }
                }
            }

            is IPodScreenDestination.HistoryList -> {
                if (recentlyPlayed.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_history", title = "Sin historial", hasSubmenu = false))
                } else {
                    recentlyPlayed.map { item ->
                        IPodMenuItem(
                            id = "hist_${item.id}",
                            title = item.title,
                            subtitle = item.subtitle,
                            hasSubmenu = false,
                            artworkUrl = item.thumbnail,
                            extraData = item
                        )
                    }
                }
            }

            is IPodScreenDestination.DownloadsList -> {
                if (downloadedSongs.isEmpty()) {
                    listOf(IPodMenuItem(id = "empty_downloads", title = "Sin descargas", hasSubmenu = false))
                } else {
                    downloadedSongs.map { item ->
                        IPodMenuItem(
                            id = "down_${item.id}",
                            title = item.title,
                            subtitle = item.subtitle,
                            hasSubmenu = false,
                            artworkUrl = item.thumbnail,
                            extraData = item
                        )
                    }
                }
            }

            is IPodScreenDestination.SearchScreen -> {
                val pool = (savedItems + downloadedSongs + recentlyPlayed).distinctBy { it.id }
                val filtered = if (searchQuery.isBlank()) pool.take(20) else {
                    pool.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                            it.subtitle.contains(searchQuery, ignoreCase = true) ||
                            (it.album?.contains(searchQuery, ignoreCase = true) == true)
                    }
                }
                filtered.map { item ->
                    IPodMenuItem(
                        id = "search_${item.id}",
                        title = item.title,
                        subtitle = item.subtitle,
                        hasSubmenu = item.type == ItemType.ALBUM || item.type == ItemType.ARTIST,
                        artworkUrl = item.thumbnail,
                        extraData = item
                    )
                }
            }

            is IPodScreenDestination.SettingsMenu -> {
                val colorLabel = when (ipodColorStyle.lowercase()) {
                    "white" -> "Blanco Clásico"
                    "black" -> "Negro Grafito"
                    "u2" -> "U2 Special Edition"
                    else -> "Plata Metálico"
                }
                val soundLabel = if (ipodWheelSound) "Activado" else "Desactivado"

                listOf(
                    IPodMenuItem(id = "cfg_color", title = "Color de iPod", subtitle = colorLabel, hasSubmenu = false),
                    IPodMenuItem(id = "cfg_sound", title = "Sonido de Click", subtitle = soundLabel, hasSubmenu = false),
                    IPodMenuItem(id = "cfg_exit", title = "Salir del Modo iPod", hasSubmenu = false)
                )
            }

            is IPodScreenDestination.QueueScreen -> queueMenuItems
            is IPodScreenDestination.NowPlaying,
            is IPodScreenDestination.CoverFlowViews,
            is IPodScreenDestination.CoverFlowAlbums,
            is IPodScreenDestination.LyricsScreen -> emptyList()
        }
    }

    // Wheel Scroll handler
    val handleScroll: (Int) -> Unit = { steps ->
        when (currentDestination) {
            is IPodScreenDestination.NowPlaying -> {
                if (isScrubbingMode) {
                    val target = (currentPosition + steps * 5000L).coerceIn(0L, duration)
                    musicPlayer?.seekTo(target)
                } else {
                    audioManager?.let { am ->
                        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val currVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val newVol = (currVol + steps).coerceIn(0, maxVol)
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                        currentVolumeLevel = newVol.toFloat() / maxVol.toFloat()

                        showVolumeHud = true
                        volumeHudJob?.cancel()
                        volumeHudJob = coroutineScope.launch {
                            delay(1500L)
                            showVolumeHud = false
                        }
                    }
                }
            }

            is IPodScreenDestination.CoverFlowViews -> {
                val newIndex = (currentSelectedIndex + steps).coerceIn(0, viewCoverFlowCards.size - 1)
                currentSelectedIndex = newIndex
                selectionIndices[currentDestination.title] = newIndex
            }

            is IPodScreenDestination.CoverFlowAlbums -> {
                if (albumCoverFlowCards.isNotEmpty()) {
                    val newIndex = (currentSelectedIndex + steps).coerceIn(0, albumCoverFlowCards.size - 1)
                    currentSelectedIndex = newIndex
                    selectionIndices[currentDestination.title] = newIndex
                }
            }

            is IPodScreenDestination.LyricsScreen -> {
                val lineCount = songLyrics?.size ?: 1
                val newIndex = (currentSelectedIndex + steps).coerceIn(0, (lineCount - 1).coerceAtLeast(0))
                currentSelectedIndex = newIndex
                selectionIndices[currentDestination.title] = newIndex
            }

            is IPodScreenDestination.QueueScreen -> {
                val totalCount = maxOf(queueMenuItems.size, queueCoverFlowCards.size)
                if (totalCount > 0) {
                    val newIndex = (currentSelectedIndex + steps).coerceIn(0, totalCount - 1)
                    currentSelectedIndex = newIndex
                    selectionIndices[currentDestination.title] = newIndex
                }
            }

            else -> {
                if (menuItems.isNotEmpty()) {
                    val newIndex = (currentSelectedIndex + steps).coerceIn(0, menuItems.size - 1)
                    currentSelectedIndex = newIndex
                    selectionIndices[currentDestination.title] = newIndex
                }
            }
        }
    }

    // Comprehensive action handler for menu items (used by both touchscreen taps and Click Wheel Select)
    val handleItemAction: (IPodMenuItem) -> Unit = { item ->
        when (item.id) {
            "coverflow_views" -> navigateTo(IPodScreenDestination.CoverFlowViews)
            "coverflow_albums" -> navigateTo(IPodScreenDestination.CoverFlowAlbums)
            "music" -> navigateTo(IPodScreenDestination.MusicMenu)
            "inicio" -> navigateTo(IPodScreenDestination.InicioMenu)
            "novedades" -> navigateTo(IPodScreenDestination.NovedadesMenu)
            "radio" -> navigateTo(IPodScreenDestination.RadioMenu)
            "now_playing" -> navigateTo(IPodScreenDestination.NowPlaying)
            "lyrics" -> navigateTo(IPodScreenDestination.LyricsScreen)
            "queue" -> navigateTo(IPodScreenDestination.QueueScreen)
            "search" -> navigateTo(IPodScreenDestination.SearchScreen)
            "shuffle" -> {
                val allSongs = (savedItems.filter { it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                    .distinctBy { it.id }
                    .shuffled()
                if (allSongs.isNotEmpty()) {
                    playTrackWithQueue(allSongs.first(), allSongs)
                }
            }
            "settings" -> navigateTo(IPodScreenDestination.SettingsMenu)

            "playlists" -> navigateTo(IPodScreenDestination.PlaylistsList)
            "artists" -> navigateTo(IPodScreenDestination.ArtistsList)
            "albums" -> navigateTo(IPodScreenDestination.AlbumsList)
            "songs" -> navigateTo(IPodScreenDestination.SongsList)
            "history" -> navigateTo(IPodScreenDestination.HistoryList)
            "downloads" -> navigateTo(IPodScreenDestination.DownloadsList)

            "cfg_color" -> {
                val nextStyle = when (ipodColorStyle.lowercase()) {
                    "silver" -> "white"
                    "white" -> "black"
                    "black" -> "u2"
                    else -> "silver"
                }
                LibraryManager.saveIpodColorStyle(nextStyle)
            }
            "cfg_sound" -> {
                LibraryManager.saveIpodWheelSound(!ipodWheelSound)
            }
            "cfg_exit" -> {
                onExitIpodMode()
            }

            else -> {
                // Check contextual destination first to avoid loops
                when (currentDestination) {
                    is IPodScreenDestination.AlbumDetail -> {
                        val albumTracks = (savedItems + downloadedSongs + recentlyPlayed)
                            .filter { it.album.equals(currentDestination.albumName, ignoreCase = true) }
                            .distinctBy { it.id }
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, albumTracks)
                        }
                    }

                    is IPodScreenDestination.PlaylistDetail -> {
                        val pl = currentDestination.playlist
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, pl.items)
                        }
                    }

                    is IPodScreenDestination.ArtistDetail -> {
                        val artistSongs = (savedItems + downloadedSongs + recentlyPlayed)
                            .filter { it.subtitle.equals(currentDestination.artistName, ignoreCase = true) }
                            .distinctBy { it.id }
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, artistSongs)
                        }
                    }

                    is IPodScreenDestination.SongsList -> {
                        val allSongs = (savedItems.filter { it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                            .distinctBy { it.id }
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, allSongs)
                        }
                    }

                    is IPodScreenDestination.HistoryList -> {
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, recentlyPlayed)
                        }
                    }

                    is IPodScreenDestination.DownloadsList -> {
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, downloadedSongs)
                        }
                    }

                    is IPodScreenDestination.InicioMenu -> {
                        val songs = (savedItems.filter { it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                            .distinctBy { it.id }
                            .take(20)
                        val track = item.extraData as? LibraryItem
                        if (track != null) {
                            playTrackWithQueue(track, songs)
                        }
                    }

                    is IPodScreenDestination.NovedadesMenu -> {
                        when (val data = item.extraData) {
                            is String -> navigateTo(IPodScreenDestination.AlbumDetail(data))
                            is LibraryItem -> {
                                if (data.type == ItemType.ALBUM) {
                                    navigateTo(IPodScreenDestination.AlbumDetail(data.album ?: data.title))
                                } else {
                                    val newItems = (savedItems.filter { it.type == ItemType.ALBUM || it.type == ItemType.SONG } + downloadedSongs + recentlyPlayed)
                                        .filter { it.type == ItemType.SONG }
                                        .distinctBy { it.id }
                                    playTrackWithQueue(data, newItems)
                                }
                            }
                        }
                    }

                    is IPodScreenDestination.RadioMenu -> {
                        val artistName = (item.extraData as? String) ?: item.title
                        val artistSongs = (savedItems + downloadedSongs + recentlyPlayed)
                            .filter { it.subtitle.contains(artistName, ignoreCase = true) }
                            .distinctBy { it.id }
                        if (artistSongs.isNotEmpty()) {
                            playTrackWithQueue(artistSongs.first(), artistSongs)
                        } else {
                            val anySong = (savedItems + downloadedSongs + recentlyPlayed).firstOrNull()
                            if (anySong != null) playItem(anySong)
                        }
                    }

                    is IPodScreenDestination.QueueScreen -> {
                        when (val data = item.extraData) {
                            is PlayerState -> {
                                navigateTo(IPodScreenDestination.NowPlaying)
                            }
                            is QueueItem -> {
                                val idx = PlaybackQueue.queue.indexOfFirst { it.videoId == data.videoId }
                                val remainingQueue = if (idx >= 0) PlaybackQueue.queue.drop(idx + 1) else PlaybackQueue.queue
                                val newState = PlayerState(
                                    title = data.title,
                                    artist = data.artist,
                                    artUrl = data.artUrl,
                                    videoId = data.videoId,
                                    contentUri = null,
                                    queue = remainingQueue,
                                    isExclusiveQueue = PlaybackQueue.isExclusiveQueue,
                                    album = data.album,
                                    albumId = data.albumId,
                                    playlistId = data.playlistId,
                                    playlistName = data.playlistName
                                )
                                PlaybackQueue.queue = remainingQueue
                                onSongSelectedFromQueue(newState)
                                navigateTo(IPodScreenDestination.NowPlaying)
                            }
                            is com.echo.innertube.models.SongItem -> {
                                val idx = PlaybackQueue.upNextSongs.indexOfFirst { it.id == data.id }
                                val remainingUpNext = if (idx >= 0) PlaybackQueue.upNextSongs.drop(idx + 1) else PlaybackQueue.upNextSongs
                                val newState = PlayerState(
                                    title = data.title,
                                    artist = data.artists.joinToString { it.name },
                                    artUrl = data.thumbnail,
                                    videoId = data.id,
                                    contentUri = null,
                                    isExclusiveQueue = PlaybackQueue.isExclusiveQueue,
                                    album = data.album?.name,
                                    albumId = data.album?.id
                                )
                                PlaybackQueue.upNextSongs = remainingUpNext
                                onSongSelectedFromQueue(newState)
                                navigateTo(IPodScreenDestination.NowPlaying)
                            }
                            else -> {
                                navigateTo(IPodScreenDestination.NowPlaying)
                            }
                        }
                    }

                    else -> {
                        // General fallbacks for other menus like Search, Main, etc.
                        when (val data = item.extraData) {
                            is Playlist -> navigateTo(IPodScreenDestination.PlaylistDetail(data))
                            is LibraryItem -> {
                                if (data.type == ItemType.ALBUM) {
                                    navigateTo(IPodScreenDestination.AlbumDetail(data.album ?: data.title))
                                } else if (data.type == ItemType.ARTIST) {
                                    navigateTo(IPodScreenDestination.ArtistDetail(data.subtitle.ifBlank { data.title }))
                                } else {
                                    playItem(data)
                                }
                            }
                            is String -> {
                                if (currentDestination is IPodScreenDestination.ArtistsList) {
                                    navigateTo(IPodScreenDestination.ArtistDetail(data))
                                } else if (currentDestination is IPodScreenDestination.AlbumsList) {
                                    navigateTo(IPodScreenDestination.AlbumDetail(data))
                                } else {
                                    val song = (savedItems + downloadedSongs + recentlyPlayed).firstOrNull {
                                        it.subtitle.contains(data, ignoreCase = true) || it.title.contains(data, ignoreCase = true)
                                    }
                                    if (song != null) playItem(song)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Center SELECT button handler
    val handleSelect: () -> Unit = {
        when (currentDestination) {
            is IPodScreenDestination.NowPlaying -> {
                // Center button in Now Playing cycles: Scrub Mode -> Lyrics -> Queue -> Back to Cover
                if (isScrubbingMode) {
                    isScrubbingMode = false
                    navigateTo(IPodScreenDestination.LyricsScreen)
                } else {
                    isScrubbingMode = true
                }
            }

            is IPodScreenDestination.LyricsScreen -> {
                navigateTo(IPodScreenDestination.QueueScreen)
            }

            is IPodScreenDestination.QueueScreen -> {
                val selectedItem = queueMenuItems.getOrNull(currentSelectedIndex)
                if (selectedItem != null) {
                    handleItemAction(selectedItem)
                } else {
                    val selectedCard = queueCoverFlowCards.getOrNull(currentSelectedIndex)
                    if (selectedCard?.extraData != null) {
                        handleItemAction(IPodMenuItem(id = selectedCard.id, title = selectedCard.title, extraData = selectedCard.extraData))
                    }
                }
            }

            is IPodScreenDestination.CoverFlowViews -> {
                val targetCard = viewCoverFlowCards.getOrNull(currentSelectedIndex)
                if (targetCard?.targetDestination != null) {
                    navigateTo(targetCard.targetDestination)
                }
            }

            is IPodScreenDestination.CoverFlowAlbums -> {
                val targetCard = albumCoverFlowCards.getOrNull(currentSelectedIndex)
                if (targetCard?.targetDestination != null) {
                    navigateTo(targetCard.targetDestination)
                }
            }

            else -> {
                val selectedItem = menuItems.getOrNull(currentSelectedIndex)
                if (selectedItem != null) {
                    handleItemAction(selectedItem)
                }
            }
        }
    }

    // ── 100% FULLSCREEN IPOD HARDWARE CASING & CHASSIS ─────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(chassisTheme.bodyBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP HARDWARE ACCENTS (Hold Switch, iPod Branding, Salir Button) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Headphone Jack & Hold Switch detail
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 14.dp, height = 6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF262930))
                            .border(0.6.dp, Color(0xFF6E7482), RoundedCornerShape(3.dp))
                    )
                    Box(
                        modifier = Modifier
                            .size(width = 9.dp, height = 5.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF8E95A5))
                    )
                }

                // iPod Logo
                Text(
                    text = "iPod",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = chassisTheme.accentTextColor
                )

                // Quick Exit Button to return to modern UI
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x28000000))
                        .clickable { onExitIpodMode() }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Salir",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = chassisTheme.accentTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── IPOD LCD SCREEN CONTAINER (Upper ~46% height) ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.46f)
                    .shadow(elevation = 14.dp, shape = RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(chassisTheme.screenBezelColor)
                    .border(3.5.dp, chassisTheme.screenBezelColor, RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                AnimatedContent(
                    targetState = currentDestination,
                    transitionSpec = {
                        slideInHorizontally(initialOffsetX = { it }) togetherWith
                            slideOutHorizontally(targetOffsetX = { -it })
                    },
                    label = "iPodScreenTransition"
                ) { targetDest ->
                    val activeCards = when (targetDest) {
                        is IPodScreenDestination.CoverFlowAlbums -> albumCoverFlowCards
                        is IPodScreenDestination.QueueScreen -> queueCoverFlowCards
                        else -> viewCoverFlowCards
                    }

                    IPodScreen(
                        destination = targetDest,
                        items = menuItems,
                        coverFlowCards = activeCards,
                        selectedIndex = currentSelectedIndex,
                        isPlaying = isPlaying,
                        playerState = playerState,
                        currentPosition = currentPosition,
                        duration = duration,
                        volumeLevel = currentVolumeLevel,
                        showVolumeHud = showVolumeHud,
                        isScrubbingMode = isScrubbingMode,
                        lyrics = songLyrics,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigateToLyrics = { navigateTo(IPodScreenDestination.LyricsScreen) },
                        onNavigateToQueue = { navigateTo(IPodScreenDestination.QueueScreen) },
                        onItemClick = { index, item ->
                            currentSelectedIndex = index
                            selectionIndices[currentDestination.title] = index
                            handleItemAction(item)
                        },
                        onCoverFlowCardClick = { index, card ->
                            currentSelectedIndex = index
                            selectionIndices[currentDestination.title] = index
                            if (currentDestination is IPodScreenDestination.QueueScreen) {
                                val qItem = queueMenuItems.getOrNull(index)
                                if (qItem != null) {
                                    handleItemAction(qItem)
                                } else if (card.extraData != null) {
                                    handleItemAction(IPodMenuItem(id = card.id, title = card.title, extraData = card.extraData))
                                }
                            } else if (card.targetDestination != null) {
                                navigateTo(card.targetDestination)
                            }
                        },
                        onIndexChange = { newIdx ->
                            currentSelectedIndex = newIdx
                            selectionIndices[currentDestination.title] = newIdx
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── IPOD CLICK WHEEL CONTAINER (Lower ~54% height) ─────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.54f),
                contentAlignment = Alignment.Center
            ) {
                IPodClickWheel(
                    wheelSize = 270.dp,
                    colorStyle = chassisTheme.wheelColorStyle,
                    soundEnabled = ipodWheelSound,
                    hapticsEnabled = true,
                    onScroll = handleScroll,
                    onMenu = navigateBack,
                    onSelect = handleSelect,
                    onPlayPause = {
                        musicPlayer?.togglePlayPause()
                    },
                    onNext = onSkipNext,
                    onPrev = onSkipPrevious
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
