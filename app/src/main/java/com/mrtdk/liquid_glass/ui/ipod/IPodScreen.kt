package com.mrtdk.liquid_glass.ui.ipod

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrtdk.liquid_glass.data.LibraryItem
import com.mrtdk.liquid_glass.data.Playlist
import com.mrtdk.liquid_glass.data.lyrics.LyricLine
import com.mrtdk.liquid_glass.ui.screens.PlayerState

/**
 * Data model for an iPod menu item.
 */
data class IPodMenuItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val hasSubmenu: Boolean = true,
    val artworkUrl: String? = null,
    val extraData: Any? = null
)

/**
 * Complete set of iPod Navigation Destinations representing all sections and views of the application.
 */
sealed class IPodScreenDestination {
    abstract val title: String

    object MainMenu : IPodScreenDestination() { override val title = "iPod" }
    object CoverFlowViews : IPodScreenDestination() { override val title = "Cover Flow" }
    object CoverFlowAlbums : IPodScreenDestination() { override val title = "Álbumes en 3D" }
    object MusicMenu : IPodScreenDestination() { override val title = "Música" }
    object InicioMenu : IPodScreenDestination() { override val title = "Inicio" }
    object NovedadesMenu : IPodScreenDestination() { override val title = "Novedades" }
    object RadioMenu : IPodScreenDestination() { override val title = "Radio" }
    object PlaylistsList : IPodScreenDestination() { override val title = "Playlists" }
    data class PlaylistDetail(val playlist: Playlist) : IPodScreenDestination() { override val title = playlist.name }
    object ArtistsList : IPodScreenDestination() { override val title = "Artistas" }
    data class ArtistDetail(val artistName: String) : IPodScreenDestination() { override val title = artistName }
    object AlbumsList : IPodScreenDestination() { override val title = "Álbumes" }
    data class AlbumDetail(val albumName: String) : IPodScreenDestination() { override val title = albumName }
    object SongsList : IPodScreenDestination() { override val title = "Canciones" }
    object HistoryList : IPodScreenDestination() { override val title = "Historial" }
    object DownloadsList : IPodScreenDestination() { override val title = "Descargados" }
    object NowPlaying : IPodScreenDestination() { override val title = "Reproduciendo" }
    object LyricsScreen : IPodScreenDestination() { override val title = "Letras" }
    object QueueScreen : IPodScreenDestination() { override val title = "Cola de reproducción" }
    object SearchScreen : IPodScreenDestination() { override val title = "Buscar" }
    object SettingsMenu : IPodScreenDestination() { override val title = "Ajustes" }
}

/**
 * iPod Screen Composable rendering the retro LCD display with split menus, 3D Cover Flow,
 * top status bar, classic blue highlight bar, Now Playing screen, synchronized lyrics, and Volume HUD.
 */
@Composable
fun IPodScreen(
    modifier: Modifier = Modifier,
    destination: IPodScreenDestination,
    items: List<IPodMenuItem>,
    coverFlowCards: List<CoverFlowCard> = emptyList(),
    selectedIndex: Int,
    isPlaying: Boolean,
    playerState: PlayerState?,
    currentPosition: Long,
    duration: Long,
    volumeLevel: Float, // 0f to 1f
    showVolumeHud: Boolean,
    isScrubbingMode: Boolean,
    lyrics: List<LyricLine>? = null,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onNavigateToLyrics: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {},
    onItemClick: ((Int, IPodMenuItem) -> Unit)? = null,
    onCoverFlowCardClick: ((Int, CoverFlowCard) -> Unit)? = null,
    onIndexChange: ((Int) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF6F7F9))
            .border(2.dp, Color(0xFF30343D), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── TOP LCD STATUS BAR ──────────────────────────────────────
            IPodStatusBar(
                title = destination.title,
                isPlaying = isPlaying
            )

            // ── SCREEN BODY ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (destination) {
                    is IPodScreenDestination.CoverFlowViews,
                    is IPodScreenDestination.CoverFlowAlbums -> {
                        IPodCoverFlow(
                            cards = coverFlowCards,
                            selectedIndex = selectedIndex,
                            onIndexChange = onIndexChange,
                            onCardClick = onCoverFlowCardClick
                        )
                    }

                    is IPodScreenDestination.NowPlaying -> {
                        IPodNowPlayingView(
                            playerState = playerState,
                            currentPosition = currentPosition,
                            duration = duration,
                            isScrubbingMode = isScrubbingMode,
                            onNavigateToLyrics = onNavigateToLyrics,
                            onNavigateToQueue = onNavigateToQueue
                        )
                    }

                    is IPodScreenDestination.LyricsScreen -> {
                        IPodLyricsView(
                            playerState = playerState,
                            currentPosition = currentPosition,
                            lyrics = lyrics,
                            manualScrollIndex = selectedIndex
                        )
                    }

                    is IPodScreenDestination.QueueScreen -> {
                        IPodQueueView(
                            queueItems = items,
                            coverFlowCards = coverFlowCards,
                            selectedIndex = selectedIndex,
                            playerState = playerState,
                            onItemClick = onItemClick,
                            onCardClick = onCoverFlowCardClick,
                            onIndexChange = onIndexChange
                        )
                    }

                    is IPodScreenDestination.SearchScreen -> {
                        IPodSearchView(
                            query = searchQuery,
                            onQueryChange = onSearchQueryChange,
                            results = items,
                            selectedIndex = selectedIndex,
                            onItemClick = onItemClick
                        )
                    }

                    else -> {
                        IPodSplitMenuView(
                            items = items,
                            selectedIndex = selectedIndex,
                            playerState = playerState,
                            onItemClick = onItemClick
                        )
                    }
                }

                // ── VOLUME HUD OVERLAY ──────────────────────────────────
                androidx.compose.animation.AnimatedVisibility(
                    visible = showVolumeHud,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 14.dp)
                ) {
                    IPodVolumeHud(volumeLevel = volumeLevel)
                }
            }
        }

        // Retro LCD Diagonal Glass Reflection Highlight
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x18FFFFFF),
                        Color(0x06FFFFFF),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height * 0.7f)
                )
            )
        }
    }
}

/**
 * Classic iPod Status Bar (Grey gradient, play/pause icon, centered title, battery).
 */
@Composable
fun IPodStatusBar(
    title: String,
    isPlaying: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFE8EAEF),
                        Color(0xFFCED3DC),
                        Color(0xFFB8BFCC)
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = Color(0xFFA0A7B5)
            )
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Left: Play / Pause indicator
        Row(
            modifier = Modifier.align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPlaying) {
                Text(
                    text = "▶",
                    fontSize = 11.sp,
                    color = Color(0xFF22262B),
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = "❚❚",
                    fontSize = 10.sp,
                    color = Color(0xFF4A505A),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Section Title
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E2125),
            fontFamily = FontFamily.SansSerif,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        // Right: Classic iPod Battery Icon
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(width = 18.dp, height = 9.dp)) {
                // Battery outline
                drawRoundRect(
                    color = Color(0xFF333840),
                    size = Size(size.width - 2.5.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
                // Battery tip
                drawRoundRect(
                    color = Color(0xFF333840),
                    topLeft = Offset(size.width - 2.dp.toPx(), size.height * 0.25f),
                    size = Size(2.dp.toPx(), size.height * 0.5f),
                    cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
                )
                // Battery fill level (75% green fill)
                drawRoundRect(
                    color = Color(0xFF388E3C),
                    topLeft = Offset(1.5.dp.toPx(), 1.5.dp.toPx()),
                    size = Size((size.width - 5.5.dp.toPx()) * 0.75f, size.height - 3.dp.toPx()),
                    cornerRadius = CornerRadius(0.8.dp.toPx(), 0.8.dp.toPx())
                )
            }
        }
    }
}

/**
 * Classic 5th/6th Gen iPod Split-Screen Menu:
 * Left pane has the list with blue gradient highlight bar and right arrow `>`.
 * Right pane has dynamic artwork/preview pane.
 */
@Composable
fun IPodSplitMenuView(
    items: List<IPodMenuItem>,
    selectedIndex: Int,
    playerState: PlayerState?,
    onItemClick: ((Int, IPodMenuItem) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    // Keep highlighted row centered and visible as user scrolls the Click Wheel
    LaunchedEffect(selectedIndex) {
        if (items.isNotEmpty()) {
            val safeIndex = selectedIndex.coerceIn(0, items.size - 1)
            listState.animateScrollToItem(safeIndex)
        }
    }

    val currentSelectedItem = items.getOrNull(selectedIndex)

    Row(modifier = Modifier.fillMaxSize()) {
        // ── LEFT PANE: MENU LIST (54% width) ────────────────────────
        Box(
            modifier = Modifier
                .weight(0.54f)
                .fillMaxHeight()
                .border(
                    width = 0.5.dp,
                    color = Color(0xFFC8CCD4)
                )
        ) {
            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Sin elementos",
                        color = Color(0xFF888E99),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(items) { index, item ->
                        val isSelected = index == selectedIndex
                        IPodMenuRow(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onItemClick?.invoke(index, item) }
                        )
                    }
                }
            }
        }

        // ── RIGHT PANE: DYNAMIC PREVIEW PANE (46% width) ────────────
        Box(
            modifier = Modifier
                .weight(0.46f)
                .fillMaxHeight()
                .background(Color(0xFFEBEFF4))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            val artworkUrl = currentSelectedItem?.artworkUrl
                ?: playerState?.artUrl?.toString()

            if (!artworkUrl.isNullOrBlank()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Artwork with drop shadow and reflection
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artworkUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentSelectedItem?.title ?: playerState?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .aspectRatio(1f)
                            .shadow(elevation = 6.dp, shape = RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentSelectedItem?.title ?: playerState?.title.orEmpty(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3038),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )

                    if (!currentSelectedItem?.subtitle.isNullOrBlank() || !playerState?.artist.isNullOrBlank()) {
                        Text(
                            text = currentSelectedItem?.subtitle ?: playerState?.artist.orEmpty(),
                            fontSize = 10.sp,
                            color = Color(0xFF6B7280),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Category Fallback Illustration
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFDFE3EB), Color(0xFFCBD1DD))
                                )
                            )
                            .border(1.dp, Color(0xFFBAC1CF), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                currentSelectedItem?.id?.contains("playlist") == true -> Icons.AutoMirrored.Rounded.QueueMusic
                                currentSelectedItem?.id?.contains("artist") == true -> Icons.Rounded.Person
                                currentSelectedItem?.id?.contains("album") == true -> Icons.Rounded.Album
                                currentSelectedItem?.id?.contains("lyrics") == true -> Icons.Rounded.Lyrics
                                currentSelectedItem?.id?.contains("search") == true -> Icons.Rounded.Search
                                else -> Icons.Rounded.MusicNote
                            },
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = currentSelectedItem?.title.orEmpty(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3A3F49),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Individual row in iPod menu list.
 * Features the signature iPod blue gradient selection bar when highlighted.
 */
@Composable
fun IPodMenuRow(
    item: IPodMenuItem,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val rowHeight = if (item.subtitle.isNullOrBlank()) 28.dp else 34.dp
    val rowModifier = if (isSelected) {
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF4C9DF5), // Top highlight
                        Color(0xFF0F72EC), // Upper blue
                        Color(0xFF0257C9)  // Deep bottom blue
                    )
                )
            )
            .border(
                width = 0.6.dp,
                color = Color(0xFF01419B)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .background(Color(0xFFF9FAFC))
            .border(
                width = 0.3.dp,
                color = Color(0xFFE4E7ED)
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                color = if (isSelected) Color.White else Color(0xFF1E2126),
                fontSize = if (item.subtitle.isNullOrBlank()) 12.sp else 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!item.subtitle.isNullOrBlank()) {
                Text(
                    text = item.subtitle,
                    color = if (isSelected) Color(0xFFD6E4FF) else Color(0xFF6B7280),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (item.hasSubmenu) {
            Text(
                text = ">",
                color = if (isSelected) Color.White else Color(0xFF7E8694),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

/**
 * Authentic iPod Classic "Now Playing" (Reproduciendo) screen:
 * - Album artwork with glossy corner highlight and shadow
 * - Song Title (Bold), Artist Name, Album Name
 * - Direct quick access buttons to Letras and Cola
 * - Retro diamond scrubber playback bar
 * - Elapsed time & remaining time
 */
@Composable
fun IPodNowPlayingView(
    playerState: PlayerState?,
    currentPosition: Long,
    duration: Long,
    isScrubbingMode: Boolean,
    onNavigateToLyrics: () -> Unit = {},
    onNavigateToQueue: () -> Unit = {}
) {
    val progress = if (duration > 0L) (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val elapsedStr = formatTimeMs(currentPosition)
    val remainingStr = if (duration > 0L) "-${formatTimeMs((duration - currentPosition).coerceAtLeast(0L))}" else "-0:00"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6F9))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Track position info & quick access buttons for Letras and Cola
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isScrubbingMode) "Modo Búsqueda" else "Reproduciendo",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isScrubbingMode) Color(0xFF0F72EC) else Color(0xFF6B7280)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Letras button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFF0F3F8), Color(0xFFDCE1EB))
                            )
                        )
                        .border(0.8.dp, Color(0xFFB5BDCC), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToLyrics() }
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Letras",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E242E)
                    )
                }

                // Cola button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFFF0F3F8), Color(0xFFDCE1EB))
                            )
                        )
                        .border(0.8.dp, Color(0xFFB5BDCC), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToQueue() }
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cola",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E242E)
                    )
                }
            }
        }

        // Middle: Artwork and Metadata side-by-side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Artwork (Left ~45%)
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .aspectRatio(1f)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(4.dp))
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFD6DBE4)),
                contentAlignment = Alignment.Center
            ) {
                val artUrl = playerState?.artUrl?.toString()
                if (!artUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(artUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = playerState?.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFF888E9B),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata (Right ~55%)
            Column(
                modifier = Modifier.weight(0.55f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = playerState?.title ?: "Sin reproducir",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E2127),
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = playerState?.artist ?: "Selecciona una canción",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A515E),
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = playerState?.album ?: "iPod Audio",
                    fontSize = 11.sp,
                    color = Color(0xFF7A8290),
                    fontFamily = FontFamily.SansSerif,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isScrubbingMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "● MODO DESPLAZAMIENTO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F72EC)
                    )
                }
            }
        }

        // Bottom: Classic Diamond Playback Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Background Track with bevel
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val trackHeight = 6.dp.toPx()
                    val y = (size.height - trackHeight) / 2f

                    // Beveled track trough
                    drawRoundRect(
                        color = Color(0xFFC0C5CF),
                        topLeft = Offset(0f, y),
                        size = Size(size.width, trackHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )

                    // Blue fill progress
                    val fillWidth = size.width * progress
                    if (fillWidth > 0f) {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(Color(0xFF5BA7FF), Color(0xFF0A6DE6))
                            ),
                            topLeft = Offset(0f, y),
                            size = Size(fillWidth, trackHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    // Classic Diamond Playhead Indicator
                    val diamondX = size.width * progress
                    val diamondSize = 10.dp.toPx()
                    val centerY = size.height / 2f

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(diamondX, centerY - diamondSize / 2f)
                        lineTo(diamondX + diamondSize / 2f, centerY)
                        lineTo(diamondX, centerY + diamondSize / 2f)
                        lineTo(diamondX - diamondSize / 2f, centerY)
                        close()
                    }

                    // Shadow
                    drawPath(
                        path = path,
                        color = if (isScrubbingMode) Color(0xFF0F72EC) else Color(0xFF1E232B)
                    )
                    // Highlight border
                    drawPath(
                        path = path,
                        color = Color.White,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = elapsedStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A515E)
                )
                Text(
                    text = remainingStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A515E)
                )
            }
        }
    }
}

/**
 * Synchronized Lyrics View on the iPod LCD screen.
 * Displays the current singing lyric line in bold blue with auto-scroll.
 */
@Composable
fun IPodLyricsView(
    playerState: PlayerState?,
    currentPosition: Long,
    lyrics: List<LyricLine>?,
    manualScrollIndex: Int
) {
    val listState = rememberLazyListState()

    // Find active synced lyric index
    val activeIndex = remember(currentPosition, lyrics) {
        if (lyrics.isNullOrEmpty()) -1
        else {
            val idx = lyrics.indexOfLast { it.timeMs <= currentPosition }
            if (idx >= 0) idx else 0
        }
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex in 0 until (lyrics?.size ?: 0)) {
            listState.animateScrollToItem(activeIndex)
        }
    }

    LaunchedEffect(manualScrollIndex) {
        if (!lyrics.isNullOrEmpty() && manualScrollIndex in lyrics.indices) {
            listState.animateScrollToItem(manualScrollIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8FA))
            .padding(10.dp)
    ) {
        if (lyrics.isNullOrEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Lyrics,
                        contentDescription = null,
                        tint = Color(0xFF888E9B),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sin letra disponible",
                        color = Color(0xFF6B7280),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = playerState?.title.orEmpty(),
                        color = Color(0xFF9CA3AF),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Retro header indicating active track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${playerState?.title ?: "Letras"} • ${playerState?.artist.orEmpty()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6B7280),
                        fontFamily = FontFamily.SansSerif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(lyrics) { index, line ->
                        val isLineActive = index == activeIndex

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (isLineActive) Color(0x1A0F72EC) else Color.Transparent
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = line.text,
                                fontSize = if (isLineActive) 13.sp else 12.sp,
                                fontWeight = if (isLineActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLineActive) Color(0xFF0F72EC) else Color(0xFF4A505A),
                                fontFamily = FontFamily.SansSerif,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Playback Queue View on the iPod LCD screen.
 * Displays current and upcoming songs with 3D Cover Flow perspective, badges,
 * Click Wheel / touch swipe navigation, and an optional 2D split list toggle.
 */
@Composable
fun IPodQueueView(
    queueItems: List<IPodMenuItem>,
    coverFlowCards: List<CoverFlowCard> = emptyList(),
    selectedIndex: Int,
    playerState: PlayerState?,
    onItemClick: ((Int, IPodMenuItem) -> Unit)? = null,
    onCardClick: ((Int, CoverFlowCard) -> Unit)? = null,
    onIndexChange: ((Int) -> Unit)? = null
) {
    if (queueItems.isEmpty() && coverFlowCards.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = Color(0xFF888E9B),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cola de reproducción vacía",
                    color = Color(0xFF4B5563),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No hay canciones a continuación",
                    color = Color(0xFF9CA3AF),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    } else {
        var is3DMode by remember { mutableStateOf(true) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Mode toggle header bar: [ 3D Cover Flow ] / [ Lista ]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF14171E))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val count = maxOf(queueItems.size, coverFlowCards.size)
                    Text(
                        text = "$count canciones en cola",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF9CA3AF)
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF262C36))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (is3DMode) Color(0xFF0F72EC) else Color.Transparent)
                                .clickable { is3DMode = true }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "3D",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (is3DMode) Color.White else Color(0xFF8E95A5)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!is3DMode) Color(0xFF0F72EC) else Color.Transparent)
                                .clickable { is3DMode = false }
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Lista",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!is3DMode) Color.White else Color(0xFF8E95A5)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (is3DMode && coverFlowCards.isNotEmpty()) {
                    IPodCoverFlow(
                        cards = coverFlowCards,
                        selectedIndex = selectedIndex,
                        onIndexChange = onIndexChange,
                        onCardClick = onCardClick
                    )
                } else {
                    IPodSplitMenuView(
                        items = queueItems,
                        selectedIndex = selectedIndex,
                        playerState = playerState,
                        onItemClick = onItemClick
                    )
                }
            }
        }
    }
}

/**
 * Interactive Search View on the iPod LCD screen.
 */
@Composable
fun IPodSearchView(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<IPodMenuItem>,
    selectedIndex: Int,
    onItemClick: ((Int, IPodMenuItem) -> Unit)? = null
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (results.isNotEmpty()) {
            listState.animateScrollToItem(selectedIndex.coerceIn(0, results.size - 1))
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Input Bar with real interactive BasicTextField
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE4E8EE))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFC0C5CF), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF7A808C),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        color = Color(0xFF1E2126),
                        fontFamily = FontFamily.SansSerif
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(
                                text = "Buscar canciones...",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF),
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // Search Results List
        Box(modifier = Modifier.fillMaxSize()) {
            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (query.isBlank()) "Escribe para buscar" else "Sin resultados",
                        color = Color(0xFF888E99),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(results) { index, item ->
                        val isSelected = index == selectedIndex
                        IPodMenuRow(
                            item = item,
                            isSelected = isSelected,
                            onClick = { onItemClick?.invoke(index, item) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic iPod Volume HUD overlay (appears when rotating the Click Wheel in Now Playing).
 */
@Composable
fun IPodVolumeHud(
    volumeLevel: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(34.dp)
            .shadow(elevation = 10.dp, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xE62A2E36),
                        Color(0xF0181A1F)
                    )
                )
            )
            .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Segmented Volume Bar (16 segments like genuine iPod)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalSegments = 16
                val activeSegments = (volumeLevel * totalSegments).toInt().coerceIn(0, totalSegments)

                for (i in 0 until totalSegments) {
                    val isActive = i < activeSegments
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(1.dp))
                            .then(
                                if (isActive) {
                                    Modifier.background(
                                        Brush.verticalGradient(
                                            listOf(Color(0xFF63B3FF), Color(0xFF0F72EC))
                                        )
                                    )
                                } else {
                                    Modifier.background(Color(0x33FFFFFF))
                                }
                            )
                    )
                }
            }
        }
    }
}

private fun formatTimeMs(timeMs: Long): String {
    val totalSeconds = (timeMs / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
