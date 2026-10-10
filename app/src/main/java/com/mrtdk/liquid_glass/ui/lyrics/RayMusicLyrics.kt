package com.mrtdk.liquid_glass.ui.lyrics

import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.data.lyrics.CharGrowth
import com.mrtdk.liquid_glass.data.lyrics.GrowingWord
import com.mrtdk.liquid_glass.data.lyrics.LyricAlignment
import com.mrtdk.liquid_glass.data.lyrics.LyricLine
import com.mrtdk.liquid_glass.data.lyrics.LyricsTranslation
import com.mrtdk.liquid_glass.data.lyrics.isGeniusSectionHeader
import com.mrtdk.liquid_glass.data.lyrics.translationLanguageName
import com.mrtdk.liquid_glass.ui.components.AppleMusicSingSlider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

// ── Typography & Sizing ────────────────────────────────────────────────────────
// Lead synced: 34sp / 41sp
// Lead unsynced: 30sp / 38sp
// Backing: 23sp / 29sp
// Sub-lyrics (translation/romanization): 20sp / 25sp
// Sub-backing: 16sp / 21sp
private val GLOW_RADIUS = 6.dp
private val GLOW_ROOM = 10.dp
private val BACKING_FONT_SIZE = 23.sp
private val BACKING_LINE_HEIGHT = 29.sp
private const val BACKING_ALPHA = 0.72f
private const val BACKING_OPEN_MS = 400
private const val BACKING_LEAD_MS = 250L
private const val BACKING_CLOSE_MS = 450
private val BACKING_RISE = 6.dp
private const val BACKING_REST_SCALE = 0.94f
private const val BACKING_FADE_LEAD = 1.6f

private val SUB_LYRIC_FONT_SIZE = 20.sp
private val SUB_LYRIC_LINE_HEIGHT = 25.sp
private val SUB_BACKING_FONT_SIZE = 16.sp
private val SUB_BACKING_LINE_HEIGHT = 21.sp
private const val SUB_LYRIC_ALPHA = 0.85f
private val SUB_LYRIC_TUCK = 6.dp
private const val SUB_LYRIC_OPEN_MS = 460
private const val SUB_LYRIC_CLOSE_MS = 260

private val WIPE_FEATHER = 30.dp
private val WORD_RISE = 2.dp
private const val GROW_HEADROOM = 3f
private val DUET_LANE = 44.dp
private val GAP_ROW_HEIGHT = 40.dp
private val GAP_ROW_SPACING = 16.dp
private val LINE_FALLOFF_ALPHA = floatArrayOf(1f, 0.8f, 0.7f, 0.58f, 0.46f)
private val LINE_FALLOFF_BLUR = arrayOf(0.dp, 1.dp, 1.dp, 1.7.dp, 2.4.dp)
private val SKELETON_BLOCKS = listOf(
    floatArrayOf(0.97f, 0.54f),
    floatArrayOf(0.92f, 0.99f, 0.41f),
    floatArrayOf(0.68f),
    floatArrayOf(0.95f, 0.73f),
    floatArrayOf(0.89f, 0.96f, 0.37f),
)
private val SKELETON_BAR = 26.dp
private val SKELETON_LEADING = 15.dp
private val SKELETON_BLOCK_GAP = 35.dp
private const val SKELETON_PERIOD_MS = 1_400
private const val BROWSING_ALPHA = 0.8f
private const val INACTIVE_SCALE = 0.98f
private const val PRESSED_SCALE = 0.96f
private const val GAP_DOTS = 3
private val GAP_DOT_SIZE = 13.dp
private val GAP_DOT_GAP = 5.dp
private const val GAP_DOT_REST = 0.25f
private const val GAP_REST_SCALE = 0.76f
private const val SCROLL_LEAD_MIN_MS = 350L
private const val SCROLL_LEAD_MAX_MS = 500L
private val LYRIC_EASING = CubicBezierEasing(0.41f, 0f, 0.12f, 0.99f)
private const val LYRIC_SETTLE_MS = 400
private const val STAGGER_STEPS = 3
private const val STAGGER_FRACTION = 0.06f
private const val UNSUNG_ALPHA = 0.45f
private const val GLOW_ALPHA = 0.62f
private val CONTROLS_SCROLL_SLOP = 20.dp
private val LYRICS_GUTTER = 24.dp

private const val PHASE_BEFORE = 0
private const val PHASE_MOVING = 1
private const val PHASE_AFTER = 2
private const val GAP_RUNNING = -1f

private const val TRANSLATION_MOTION_MS = 540
private const val PARTICLES_PER_VOICE = 18

sealed interface LyricsTranslationUiState {
    data object Idle : LyricsTranslationUiState
    data object Loading : LyricsTranslationUiState
    data class Ready(val lines: List<LyricLine>) : LyricsTranslationUiState
    data object SameLanguage : LyricsTranslationUiState
}

enum class LyricsDisplayMode { Original, Romanized, Translated }

private data class TranslationParticle(
    val anchor: Offset,
    val drift: Offset,
    val radius: Float,
    val delay: Float,
)

private class ScrollRun(val id: Int, val delta: Float, val durationMs: Int) {
    val spanMs: Float get() = durationMs * (1f + STAGGER_FRACTION * STAGGER_STEPS)
}

internal fun activeLyricRows(lines: List<LyricLine>, positionMs: Long): List<Int> {
    val latest = lines.indexOfLast { it.timeMs <= positionMs }
    if (latest < 0) return emptyList()
    return (0..latest).filter { index ->
        val line = lines[index]
        index == latest || (!line.isGap &&
            (line.hasKnownEnd || line.background?.hasKnownEnd == true) &&
            line.timeMs <= positionMs && positionMs < line.endMs)
    }
}

private fun scrollLead(lines: List<LyricLine>, positionMs: Long): Long {
    val current = lines.indexOfLast { it.timeMs <= positionMs }
    if (current < 0) return SCROLL_LEAD_MIN_MS
    val next = lines.getOrNull(current + 1) ?: return SCROLL_LEAD_MIN_MS
    val gap = next.timeMs - lines[current].endMs
    return gap.coerceIn(SCROLL_LEAD_MIN_MS, SCROLL_LEAD_MAX_MS)
}

@Composable
fun rememberIsForeground(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            foreground = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return foreground
}

@Composable
fun rememberLyricClock(
    positionMs: Long,
    isPlaying: Boolean,
    trackKey: Any = Unit,
): MutableLongState {
    val clock = remember(trackKey) { mutableLongStateOf(positionMs) }
    val engine = remember(trackKey) { LyricClock(clock.longValue) }
    val foreground = rememberIsForeground()

    if (!isPlaying) {
        LaunchedEffect(engine, positionMs) {
            engine.hold(positionMs, System.nanoTime() / 1_000_000.0)
            clock.longValue = positionMs
        }
    }

    LaunchedEffect(engine, positionMs, isPlaying, foreground) {
        if (!isPlaying || !foreground) return@LaunchedEffect
        engine.restart()
        while (true) {
            withFrameNanos { frameNanos ->
                val system = System.nanoTime()
                val now = if (abs(system - frameNanos) < 250_000_000L) frameNanos else system
                clock.longValue = engine.frame(
                    nowMs = now / 1_000_000.0,
                    reportedMs = positionMs,
                    sampledAtMs = Double.NaN,
                    discontinuity = 0L,
                )
            }
        }
    }
    return clock
}

class LyricsTranslationUi(
    val displayedLyrics: List<LyricLine>,
    val subLines: List<LyricLine>?,
    val translationState: LyricsTranslationUiState,
    val romanizationState: LyricsTranslationUiState,
    val showingTranslation: Boolean,
    val showingRomanization: Boolean,
    val transition: Int,
    val status: String,
    val toggleTranslation: () -> Unit,
    val toggleRomanization: () -> Unit,
)

@Composable
fun rememberLyricsTranslation(
    trackId: String,
    lyrics: List<LyricLine>?,
): LyricsTranslationUi {
    val context = LocalContext.current
    val currentLocale = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0] ?: Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale ?: Locale.getDefault()
        }
    }
    val translationLanguage = remember(currentLocale) {
        currentLocale.language.ifBlank { "es" }
    }
    val translationLanguageName = remember(currentLocale, translationLanguage) {
        translationLanguageName(translationLanguage, currentLocale)
    }

    val alreadyInLanguageMessage = stringResource(R.string.lyrics_already_in_language, translationLanguageName)
    val translationUnavailableMessage = stringResource(R.string.lyrics_translation_unavailable)
    val alreadyRomanizedMessage = stringResource(R.string.lyrics_already_romanized)
    val romanizationUnavailableMessage = stringResource(R.string.lyrics_romanization_unavailable)

    var translationState by remember(trackId, translationLanguage, lyrics) {
        mutableStateOf<LyricsTranslationUiState>(LyricsTranslationUiState.Idle)
    }
    var romanizationState by remember(trackId, translationLanguage, lyrics) {
        mutableStateOf<LyricsTranslationUiState>(LyricsTranslationUiState.Idle)
    }
    var lyricsDisplayMode by remember(trackId, translationLanguage, lyrics) {
        mutableStateOf(LyricsDisplayMode.Original)
    }

    val showingTranslation = lyricsDisplayMode == LyricsDisplayMode.Translated
    val showingRomanization = lyricsDisplayMode == LyricsDisplayMode.Romanized
    var translationTransition by remember(trackId) { mutableIntStateOf(0) }

    var translationJob by remember(trackId, translationLanguage, lyrics) {
        mutableStateOf<Job?>(null)
    }
    var romanizationJob by remember(trackId, translationLanguage, lyrics) {
        mutableStateOf<Job?>(null)
    }

    DisposableEffect(trackId, translationLanguage, lyrics) {
        onDispose {
            translationJob?.cancel()
            romanizationJob?.cancel()
        }
    }

    val displayedLyrics = when (lyricsDisplayMode) {
        LyricsDisplayMode.Translated ->
            (translationState as? LyricsTranslationUiState.Ready)?.lines ?: lyrics.orEmpty()
        LyricsDisplayMode.Romanized ->
            (romanizationState as? LyricsTranslationUiState.Ready)?.lines ?: lyrics.orEmpty()
        LyricsDisplayMode.Original -> lyrics.orEmpty()
    }

    val lyricsSubLines = when (lyricsDisplayMode) {
        LyricsDisplayMode.Translated -> (translationState as? LyricsTranslationUiState.Ready)?.lines
        LyricsDisplayMode.Romanized -> (romanizationState as? LyricsTranslationUiState.Ready)?.lines
        LyricsDisplayMode.Original -> null
    }

    val translationScope = rememberCoroutineScope()

    val toggleTranslation: () -> Unit = toggleTranslation@{
        when (val state = translationState) {
            is LyricsTranslationUiState.Ready -> {
                lyricsDisplayMode = if (showingTranslation) {
                    LyricsDisplayMode.Original
                } else {
                    LyricsDisplayMode.Translated
                }
                translationTransition++
            }
            LyricsTranslationUiState.Loading -> Unit
            LyricsTranslationUiState.SameLanguage -> {
                Toast.makeText(context, alreadyInLanguageMessage, Toast.LENGTH_SHORT).show()
            }
            LyricsTranslationUiState.Idle -> {
                val source = lyrics.orEmpty()
                if (source.isEmpty()) return@toggleTranslation
                translationState = LyricsTranslationUiState.Loading
                romanizationJob?.cancel()
                if (romanizationState is LyricsTranslationUiState.Loading) {
                    romanizationState = LyricsTranslationUiState.Idle
                }
                translationJob?.cancel()
                translationJob = translationScope.launch {
                    when (
                        val result = LyricsTranslation.translate(
                            trackId = trackId,
                            lines = source,
                            targetLanguageTag = translationLanguage,
                            context = context,
                        )
                    ) {
                        is LyricsTranslation.Result.Translated -> {
                            translationState = LyricsTranslationUiState.Ready(result.lines)
                            lyricsDisplayMode = LyricsDisplayMode.Translated
                            translationTransition++
                        }
                        is LyricsTranslation.Result.SameLanguage -> {
                            translationState = LyricsTranslationUiState.SameLanguage
                            Toast.makeText(context, alreadyInLanguageMessage, Toast.LENGTH_SHORT).show()
                        }
                        LyricsTranslation.Result.Unavailable -> {
                            translationState = LyricsTranslationUiState.Idle
                            Toast.makeText(context, translationUnavailableMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val toggleRomanization: () -> Unit = toggleRomanization@{
        when (val state = romanizationState) {
            is LyricsTranslationUiState.Ready -> {
                lyricsDisplayMode = if (showingRomanization) {
                    LyricsDisplayMode.Original
                } else {
                    LyricsDisplayMode.Romanized
                }
                translationTransition++
            }
            LyricsTranslationUiState.Loading -> Unit
            LyricsTranslationUiState.SameLanguage -> {
                Toast.makeText(context, alreadyRomanizedMessage, Toast.LENGTH_SHORT).show()
            }
            LyricsTranslationUiState.Idle -> {
                val source = lyrics.orEmpty()
                if (source.isEmpty()) return@toggleRomanization
                romanizationState = LyricsTranslationUiState.Loading
                translationJob?.cancel()
                if (translationState is LyricsTranslationUiState.Loading) {
                    translationState = LyricsTranslationUiState.Idle
                }
                romanizationJob?.cancel()
                romanizationJob = translationScope.launch {
                    when (
                        val result = LyricsTranslation.romanize(
                            trackId = trackId,
                            lines = source,
                            targetLanguageTag = translationLanguage,
                            context = context,
                        )
                    ) {
                        is LyricsTranslation.RomanizationResult.Romanized -> {
                            romanizationState = LyricsTranslationUiState.Ready(result.lines)
                            lyricsDisplayMode = LyricsDisplayMode.Romanized
                            translationTransition++
                        }
                        LyricsTranslation.RomanizationResult.AlreadyRomanized -> {
                            romanizationState = LyricsTranslationUiState.SameLanguage
                            Toast.makeText(context, alreadyRomanizedMessage, Toast.LENGTH_SHORT).show()
                        }
                        LyricsTranslation.RomanizationResult.Unavailable -> {
                            romanizationState = LyricsTranslationUiState.Idle
                            Toast.makeText(context, romanizationUnavailableMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val status = when {
        translationState is LyricsTranslationUiState.Loading ->
            stringResource(R.string.translating_lyrics_to, translationLanguageName)
        romanizationState is LyricsTranslationUiState.Loading ->
            stringResource(R.string.romanizing_lyrics)
        showingTranslation ->
            stringResource(R.string.lyrics_translated_to, translationLanguageName)
        showingRomanization ->
            stringResource(R.string.lyrics_romanized)
        translationState is LyricsTranslationUiState.SameLanguage ->
            stringResource(R.string.lyrics_already_in_language, translationLanguageName)
        romanizationState is LyricsTranslationUiState.SameLanguage ->
            stringResource(R.string.lyrics_already_romanized)
        else -> ""
    }

    return LyricsTranslationUi(
        displayedLyrics = displayedLyrics,
        subLines = lyricsSubLines,
        translationState = translationState,
        romanizationState = romanizationState,
        showingTranslation = showingTranslation,
        showingRomanization = showingRomanization,
        transition = translationTransition,
        status = status,
        toggleTranslation = toggleTranslation,
        toggleRomanization = toggleRomanization,
    )
}

@Composable
fun TranslationToggleButton(
    state: LyricsTranslationUiState,
    showingTranslation: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val active = showingTranslation || state is LyricsTranslationUiState.Loading
    val tint = when {
        !enabled || state is LyricsTranslationUiState.SameLanguage -> Color.White.copy(alpha = 0.42f)
        active -> Color.White
        else -> Color.White.copy(alpha = 0.78f)
    }
    val discAlpha by animateFloatAsState(
        targetValue = if (active) 0.34f else 0.18f,
        label = "translateDisc",
    )
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = discAlpha))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state is LyricsTranslationUiState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = tint,
                strokeWidth = 1.7.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Translate,
                contentDescription = stringResource(
                    if (showingTranslation) R.string.show_original_lyrics
                    else R.string.translate_lyrics,
                ),
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
fun RomanizationToggleButton(
    state: LyricsTranslationUiState,
    showingRomanization: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val active = showingRomanization || state is LyricsTranslationUiState.Loading
    val tint = when {
        !enabled || state is LyricsTranslationUiState.SameLanguage -> Color.White.copy(alpha = 0.42f)
        active -> Color.White
        else -> Color.White.copy(alpha = 0.78f)
    }
    val discAlpha by animateFloatAsState(
        targetValue = if (active) 0.34f else 0.18f,
        label = "romanizeDisc",
    )
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = discAlpha))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (state is LyricsTranslationUiState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = tint,
                strokeWidth = 1.7.dp,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = stringResource(
                    if (showingRomanization) R.string.show_original_lyrics
                    else R.string.romanize_lyrics,
                ),
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun <T, V : androidx.compose.animation.core.AnimationVector> Animatable<T, V>.asState(): State<T> =
    object : State<T> {
        override val value: T get() = this@asState.value
    }

@Composable
fun LyricsTranslationMotion(
    trigger: Int,
    reduceMotion: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable (State<Float>?) -> Unit,
) {
    val progress = remember { Animatable(1f) }
    val foreground = rememberIsForeground()
    var consumedTrigger by remember { mutableIntStateOf(trigger) }
    LaunchedEffect(trigger, reduceMotion, foreground) {
        val changed = trigger != consumedTrigger
        consumedTrigger = trigger
        if (!changed || trigger <= 0 || reduceMotion || !foreground) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = TRANSLATION_MOTION_MS, easing = LinearEasing),
            )
        }
    }

    Box(modifier = modifier) {
        content(progress.asState().takeIf { !reduceMotion && foreground && trigger > 0 })
    }
}

private fun Modifier.lyricParticles(
    layout: TextLayoutResult?,
    progress: State<Float>?,
    room: Dp,
): Modifier {
    if (layout == null || progress == null) return this
    return drawWithCache {
        val text = layout.layoutInput.text.text
        val candidates = text.indices.filter { text[it].isLetterOrDigit() }
        val random = Random(text.hashCode())
        val inset = room.toPx()
        val particles = candidates.shuffled(random).take(PARTICLES_PER_VOICE).map { index ->
            val glyph = layout.getBoundingBox(index)
            TranslationParticle(
                anchor = glyph.center + Offset(inset, inset),
                drift = Offset(
                    (random.nextFloat() - 0.5f) * 12.dp.toPx(),
                    -(5f + random.nextFloat() * 11f).dp.toPx()
                ),
                radius = (0.65f + random.nextFloat() * 0.65f).dp.toPx(),
                delay = 0.16f * index / text.length.coerceAtLeast(1),
            )
        }
        onDrawWithContent {
            drawContent()
            val value = progress.value
            if (value > 0f && value < 1f) {
                particles.forEach { particle ->
                    val t = ((value - particle.delay) / 0.84f).coerceIn(0f, 1f)
                    val envelope = sin(PI * t).toFloat()
                    val ease = 1f - (1f - t) * (1f - t)
                    val center = particle.anchor + Offset(
                        particle.drift.x * ease,
                        particle.drift.y * ease + 3.dp.toPx() * t * t,
                    )
                    drawCircle(Color.White, particle.radius * 2.7f, center, alpha = envelope * 0.07f)
                    drawCircle(Color.White, particle.radius, center, alpha = envelope * 0.58f)
                }
            }
        }
    }
}

private class SubLyricsReveal(
    val progress: State<Float>,
    lines: State<List<LyricLine>?>,
) {
    val lines: List<LyricLine>? by lines
}

@Composable
private fun rememberSubLyricsReveal(target: List<LyricLine>?, trackKey: String): SubLyricsReveal {
    val shown = remember(trackKey) { mutableStateOf(target) }
    val progress = remember(trackKey) { Animatable(if (target != null) 1f else 0f) }
    LaunchedEffect(target, trackKey) {
        if (shown.value != null && shown.value !== target && progress.value > 0f) {
            progress.animateTo(0f, tween(SUB_LYRIC_CLOSE_MS, easing = FastOutSlowInEasing))
        }
        if (target != null) {
            shown.value = target
            progress.animateTo(1f, tween(SUB_LYRIC_OPEN_MS, easing = LYRIC_EASING))
        } else {
            shown.value = null
        }
    }
    return remember(trackKey) { SubLyricsReveal(progress.asState(), shown) }
}

private fun Modifier.revealBelow(progress: State<Float>): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val open = progress.value
        val height = (placeable.height * open).roundToInt()
        layout(placeable.width, height) {
            placeable.placeWithLayer(0, 0) {
                translationY = -placeable.height * (1f - open) * 0.6f
                alpha = open * open
            }
        }
    }

private fun Modifier.revealBacking(progress: State<Float>, alignEnd: Boolean): Modifier = this
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val open = progress.value
        val rise = BACKING_RISE.toPx()
        layout(placeable.width, (placeable.height * open).roundToInt()) {
            placeable.placeWithLayer(0, 0) {
                alpha = (open * BACKING_FADE_LEAD).coerceAtMost(1f)
                translationY = rise * (1f - open)
                val grow = BACKING_REST_SCALE + (1f - BACKING_REST_SCALE) * open
                scaleX = grow
                scaleY = grow
                transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0f)
            }
        }
    }

private fun String.differsFrom(original: String): Boolean =
    trim().lowercase(Locale.ROOT) != original.trim().lowercase(Locale.ROOT)

/**
 * Main RayMusic Lyrics Panel Composable.
 */
@Composable
fun RayMusicLyrics(
    lines: List<LyricLine>,
    positionMs: Long,
    isPlaying: Boolean,
    looking: Boolean = false,
    onSeekToLine: (Long) -> Unit = {},
    controlsOpen: Boolean = true,
    onRevealControls: () -> Unit = {},
    onHideControls: () -> Unit = {},
    songTitle: String = "",
    artistName: String = "",
    artUrl: Any? = null,
    selectionModeTrigger: Int = 0,
    trackId: String = "",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedIndices by remember { mutableStateOf(setOf<Int>()) }
    var showShareDialog by remember { mutableStateOf(false) }

    val resolvedTrackId = remember(trackId, songTitle, artistName) {
        trackId.ifBlank { "${songTitle}_${artistName}".trim().ifBlank { "current_track" } }
    }

    val translationUi = rememberLyricsTranslation(
        trackId = resolvedTrackId,
        lyrics = lines,
    )

    val clock = rememberLyricClock(positionMs, isPlaying, resolvedTrackId)
    val subReveal = rememberSubLyricsReveal(translationUi.subLines, resolvedTrackId)

    val isSynced = remember(lines) { lines.any { it.timeMs > 0L } }
    val duet = remember(lines) { lines.any { it.alignment == LyricAlignment.End } }

    val activeRows by remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced) emptyList() else activeLyricRows(lines, clock.longValue)
        }
    }
    val scrollLine = activeRows.firstOrNull() ?: -1
    val leadLine by remember(lines, isSynced) {
        derivedStateOf {
            if (!isSynced) {
                -1
            } else {
                val now = clock.longValue
                activeLyricRows(lines, now + scrollLead(lines, now)).firstOrNull() ?: -1
            }
        }
    }
    val focusLine = if (leadLine >= 0) leadLine else scrollLine

    LaunchedEffect(selectionModeTrigger) {
        if (selectionModeTrigger > 0 && lines.isNotEmpty()) {
            isSelectionMode = true
            val currentIdx = if (lines.indices.contains(focusLine)) focusLine else 0
            selectedIndices = setOf(currentIdx)
        }
    }
    val listState = rememberLazyListState()

    val viewportHeight by remember(listState) {
        derivedStateOf { listState.layoutInfo.viewportSize.height }
    }
    val keepScroll = remember(listState) { keepScrollInList(listState) }
    var browsing by remember { mutableStateOf(false) }

    val glowing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !com.mrtdk.liquid_glass.BuildConfig.IS_LITE

    val hideControls by rememberUpdatedState(onHideControls)
    val revealControls by rememberUpdatedState(onRevealControls)

    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) {
                browsing = true
            }
        }
    }

    val controlsSlopPx = with(LocalDensity.current) { CONTROLS_SCROLL_SLOP.toPx() }
    val controlsOnScroll = remember(listState, controlsSlopPx) {
        object : NestedScrollConnection {
            private var travel = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    if (travel != 0f && (travel > 0f) != (available.y > 0f)) travel = 0f
                    travel += available.y
                    if (travel <= -controlsSlopPx) {
                        travel = 0f
                        hideControls()
                    } else if (travel >= controlsSlopPx) {
                        travel = 0f
                        revealControls()
                    }
                }
                return Offset.Zero
            }
        }
    }

    val currentLine by rememberUpdatedState(focusLine)
    val activeOnScreen by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.any { it.index == currentLine }
        }
    }
    LaunchedEffect(browsing, activeOnScreen, listState.isScrollInProgress) {
        if (browsing && activeOnScreen && !listState.isScrollInProgress) {
            delay(600)
            browsing = false
        }
    }

    LaunchedEffect(browsing, listState.isScrollInProgress) {
        if (browsing && !listState.isScrollInProgress) {
            delay(5_000)
            browsing = false
        }
    }

    var run by remember(lines) { mutableStateOf(ScrollRun(0, 0f, LYRIC_SETTLE_MS)) }
    val since = remember(lines) { mutableFloatStateOf(0f) }
    LaunchedEffect(run.id) {
        if (run.id == 0) return@LaunchedEffect
        val spanMs = maxOf(1, run.spanMs.toInt())
        animate(
            initialValue = 0f,
            targetValue = run.spanMs,
            animationSpec = tween(spanMs, easing = LinearEasing),
        ) { value, _ -> since.floatValue = value }
    }

    var placed by remember(resolvedTrackId) { mutableStateOf(false) }
    LaunchedEffect(focusLine, browsing, controlsOpen) {
        if (isSynced && !browsing && focusLine >= 0 && focusLine in lines.indices) {
            snapshotFlow { listState.layoutInfo.viewportSize.height }.first { it > 0 }
            val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == focusLine }
            when {
                !placed -> {
                    listState.scrollToItem(focusLine, scrollOffset = 0)
                    placed = true
                }
                visible != null -> {
                    val span = maxOf(50, scrollLead(lines, clock.longValue).toInt())
                    run = ScrollRun(run.id + 1, visible.offset.toFloat(), span)
                    listState.animateScrollBy(
                        value = visible.offset.toFloat(),
                        animationSpec = tween(durationMillis = span, easing = LYRIC_EASING),
                    )
                }
                else -> listState.animateScrollToItem(focusLine, scrollOffset = 0)
            }
        }
    }

    if (lines.isEmpty()) {
        if (looking) {
            LyricsSkeleton(modifier)
        } else {
            Box(modifier, contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_lyrics_for_track),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.6f),
                )
            }
        }
        return
    }

    LyricsTranslationMotion(
        trigger = translationUi.transition,
        modifier = modifier.fillMaxSize(),
    ) { translationProgress ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(controlsOnScroll)
                    .nestedScroll(keepScroll)
                    .revealLyricsControlsOnTap(!controlsOpen && !isSelectionMode) { onRevealControls() }
                    .fadingEdges(),
                contentPadding = PaddingValues(
                    top = 40.dp - GLOW_ROOM,
                    bottom = with(LocalDensity.current) { viewportHeight.toDp() } * 0.8f,
                    start = LYRICS_GUTTER - GLOW_ROOM,
                    end = LYRICS_GUTTER - GLOW_ROOM,
                ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(
                    items = lines,
                    key = { index, line -> "${line.timeMs}_${index}" }
                ) { index, line ->
                    if (!isSynced && isGeniusSectionHeader(line.text)) {
                        val sectionTitle = line.text.removePrefix("[").removeSuffix("]").trim()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = if (index == 0) 6.dp else 24.dp, bottom = 8.dp)
                                .padding(horizontal = GLOW_ROOM),
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .padding(horizontal = 11.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    text = sectionTitle.uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.3.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                    ),
                                    color = Color.White.copy(alpha = 0.9f),
                                )
                            }
                        }
                        return@itemsIndexed
                    }

                    if (!isSynced && line.isGap) {
                        Spacer(Modifier.height(14.dp))
                        return@itemsIndexed
                    }

                    val offset = if (scrollLine < 0) 0 else index - scrollLine
                    val distance = abs(offset)
                    val isActive = isSynced && index in activeRows
                    val step = distance.coerceAtMost(LINE_FALLOFF_ALPHA.lastIndex)

                    val blur by animateDpAsState(
                        targetValue = when {
                            !isSynced || !glowing || browsing || isActive -> 0.dp
                            else -> LINE_FALLOFF_BLUR[step]
                        },
                        animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                        label = "lyricBlur",
                    )
                    val lineAlpha by animateFloatAsState(
                        targetValue = when {
                            !isSynced -> 0.95f
                            isActive -> 1f
                            browsing -> BROWSING_ALPHA
                            else -> LINE_FALLOFF_ALPHA[step]
                        },
                        animationSpec = tween(LYRIC_SETTLE_MS, easing = LYRIC_EASING),
                        label = "lyricAlpha",
                    )

                    if (line.isGap) {
                        val until = lines.getOrNull(index + 1)?.timeMs ?: line.endMs
                        val gapFill = remember(line, until, clock) {
                            derivedStateOf(structuralEqualityPolicy()) {
                                val now = clock.longValue
                                when {
                                    now <= line.timeMs -> 0f
                                    now >= until -> 1f
                                    else -> GAP_RUNNING
                                }
                            }
                        }
                        val swell by animateFloatAsState(
                            targetValue = if (isActive) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = if (isActive) 400 else 350,
                                easing = LYRIC_EASING,
                            ),
                            label = "gapSwell",
                        )
                        val instrumental = stringResource(R.string.instrumental)
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .height((GAP_ROW_HEIGHT + GAP_ROW_SPACING) * swell)
                                .clipToBounds(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .lyricBlur(blur)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = isSynced && !isSelectionMode) { onSeekToLine(line.timeMs) }
                                    .padding(GLOW_ROOM)
                                    .size(
                                        width = GAP_DOT_SIZE * 3 + GAP_DOT_GAP * 2,
                                        height = GAP_DOT_SIZE,
                                    )
                                    .graphicsLayer {
                                        val grow = GAP_REST_SCALE + (1f - GAP_REST_SCALE) * swell
                                        scaleX = grow
                                        scaleY = grow
                                        transformOrigin = TransformOrigin(0f, 0.5f)
                                        alpha = lineAlpha * swell
                                    }
                                    .drawBehind {
                                        val span = (until - line.timeMs).coerceAtLeast(1L)
                                        val through = gapFill.value.takeUnless { it == GAP_RUNNING }
                                            ?: ((clock.longValue - line.timeMs).toFloat() / span).coerceIn(0f, 1f)
                                        val radius = GAP_DOT_SIZE.toPx() / 2f
                                        val stride = (GAP_DOT_SIZE + GAP_DOT_GAP).toPx()
                                        repeat(GAP_DOTS) { dot ->
                                            val lit = (through * GAP_DOTS - dot).coerceIn(0f, 1f)
                                            drawCircle(
                                                color = Color.White.copy(
                                                    alpha = GAP_DOT_REST + (1f - GAP_DOT_REST) * lit,
                                                ),
                                                radius = radius,
                                                center = Offset(radius + dot * stride, size.height / 2f),
                                            )
                                        }
                                    }
                                    .semantics { contentDescription = instrumental },
                            )
                        }
                        return@itemsIndexed
                    }

                    val alignEnd = duet && line.alignment == LyricAlignment.End
                    val isSelected = isSelectionMode && index in selectedIndices

                    val baseStyle = if (isSynced) {
                        MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 34.sp,
                            lineHeight = 41.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                        )
                    } else {
                        MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 30.sp,
                            lineHeight = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                        )
                    }

                    val sub = subReveal.lines?.getOrNull(index)
                    val subStyle = baseStyle.copy(
                        fontSize = SUB_LYRIC_FONT_SIZE,
                        lineHeight = SUB_LYRIC_LINE_HEIGHT,
                        fontWeight = FontWeight.Bold,
                    )

                    val backingOpen = line.background?.let { backing ->
                        val due by remember(backing, clock) {
                            derivedStateOf {
                                val now = clock.longValue
                                now >= backing.timeMs - BACKING_LEAD_MS && now < backing.endMs
                            }
                        }
                        val wanted = !isSynced || isSelectionMode || if (backing.isWordSynced) {
                            due
                        } else {
                            isActive || index == focusLine
                        }
                        animateFloatAsState(
                            targetValue = if (wanted) 1f else 0f,
                            animationSpec = if (wanted) tween(BACKING_OPEN_MS, easing = LYRIC_EASING)
                            else tween(
                                BACKING_CLOSE_MS,
                                delayMillis = if (index < focusLine) run.durationMs else 0,
                                easing = FastOutSlowInEasing,
                            ),
                            label = "backingOpen",
                        )
                    }

                    val sung = offset < 0
                    val behind = if (run.delta >= 0f) index - focusLine else focusLine - index
                    val staggerDelay = behind.coerceIn(0, STAGGER_STEPS) * STAGGER_FRACTION * run.durationMs
                    val interaction = remember { MutableInteractionSource() }
                    val pressed by interaction.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = when {
                            pressed -> PRESSED_SCALE
                            isActive -> 1f
                            else -> INACTIVE_SCALE
                        },
                        animationSpec = tween(
                            durationMillis = if (pressed) 120 else LYRIC_SETTLE_MS,
                            easing = LYRIC_EASING,
                        ),
                        label = "lyricScale",
                    )
                    val glow by animateFloatAsState(
                        targetValue = if (isActive && glowing) GLOW_ALPHA else 0f,
                        animationSpec = tween(durationMillis = 420),
                        label = "lyricGlow",
                    )

                    val shape = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = if (duet && alignEnd) DUET_LANE else 0.dp,
                            end = if (duet && !alignEnd) DUET_LANE else 0.dp,
                        )
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            transformOrigin = TransformOrigin(if (alignEnd) 1f else 0f, 0.5f)
                            alpha = lineAlpha
                            translationY = if (staggerDelay <= 0f) {
                                0f
                            } else {
                                val duration = maxOf(1, run.durationMs).toFloat()
                                val elapsed = since.floatValue
                                run.delta * (
                                    LYRIC_EASING.transform((elapsed / duration).coerceIn(0f, 1f)) -
                                    LYRIC_EASING.transform(((elapsed - staggerDelay) / duration).coerceIn(0f, 1f))
                                )
                            }
                        }
                        .lyricBlur(blur)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.16f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.dp else 0.dp,
                            color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.Transparent,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .pointerInput(isSynced, isSelectionMode, selectedIndices) {
                            detectTapGestures(
                                onTap = {
                                    if (isSelectionMode) {
                                        selectedIndices = if (index in selectedIndices) {
                                            selectedIndices - index
                                        } else {
                                            if (selectedIndices.size < 6) {
                                                selectedIndices + index
                                            } else {
                                                Toast.makeText(context, "Máximo 6 versos para compartir", Toast.LENGTH_SHORT).show()
                                                selectedIndices
                                            }
                                        }
                                        if (selectedIndices.isEmpty()) {
                                            isSelectionMode = false
                                        }
                                    } else if (isSynced) {
                                        onSeekToLine(line.timeMs)
                                    }
                                },
                                onLongPress = {
                                    if (!isSelectionMode && !line.isGap) {
                                        isSelectionMode = true
                                        selectedIndices = setOf(index)
                                    }
                                }
                            )
                        }

                    Column(modifier = shape) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = if (isSelected) 8.dp else 0.dp,
                                vertical = if (isSelected) 6.dp else 0.dp
                            )
                        ) {
                            if (duet) {
                                Text(
                                    text = if (alignEnd) "VOZ 2" else "VOZ 1",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp
                                    ),
                                    color = if (isActive) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.35f),
                                    textAlign = if (alignEnd) TextAlign.End else TextAlign.Start,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = GLOW_ROOM, vertical = 2.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isSelected && !alignEnd) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = GLOW_ROOM, end = 6.dp)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    PanelVoice(
                                        line = line,
                                        clock = clock,
                                        style = baseStyle,
                                        isActive = isActive,
                                        sung = sung,
                                        synced = isSynced,
                                        browsing = browsing,
                                        glowAlpha = glow,
                                        room = GLOW_ROOM,
                                        alignEnd = alignEnd,
                                        translationProgress = translationProgress.takeIf {
                                            if (isSynced) abs(index - focusLine) <= 1 else index < 4
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }

                                if (isSelected && alignEnd) {
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 6.dp, end = GLOW_ROOM)
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }

                            sub?.takeIf { it.text.differsFrom(line.text) }?.let { subLine ->
                                PanelVoice(
                                    line = subLine,
                                    clock = clock,
                                    style = subStyle,
                                    isActive = isActive,
                                    sung = sung,
                                    synced = isSynced,
                                    browsing = browsing,
                                    glowAlpha = 0f,
                                    room = 0.dp,
                                    alignEnd = alignEnd,
                                    rise = false,
                                    translationProgress = translationProgress.takeIf {
                                        if (isSynced) abs(index - focusLine) <= 1 else index < 4
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .revealBelow(subReveal.progress)
                                        .padding(start = GLOW_ROOM, end = GLOW_ROOM, bottom = GLOW_ROOM)
                                        .offset(y = -SUB_LYRIC_TUCK)
                                        .graphicsLayer { alpha = SUB_LYRIC_ALPHA },
                                )
                            }

                            line.background?.let { backing ->
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .then(backingOpen?.let { Modifier.revealBacking(it, alignEnd) } ?: Modifier),
                                ) {
                                    PanelVoice(
                                        line = backing.withoutBracketPunctuation(),
                                        clock = clock,
                                        style = baseStyle.copy(
                                            fontSize = BACKING_FONT_SIZE,
                                            lineHeight = BACKING_LINE_HEIGHT,
                                            fontWeight = FontWeight.SemiBold,
                                            fontStyle = FontStyle.Italic,
                                        ),
                                        isActive = isActive,
                                        sung = sung,
                                        synced = isSynced,
                                        browsing = browsing,
                                        glowAlpha = 0f,
                                        room = 0.dp,
                                        alignEnd = alignEnd,
                                        rise = false,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = GLOW_ROOM, end = GLOW_ROOM, bottom = GLOW_ROOM)
                                            .graphicsLayer { alpha = BACKING_ALPHA },
                                    )

                                    sub?.background
                                        ?.takeIf { it.text.differsFrom(backing.text) }
                                        ?.let { subBacking ->
                                            PanelVoice(
                                                line = subBacking.withoutBracketPunctuation(),
                                                clock = clock,
                                                style = subStyle.copy(
                                                    fontSize = SUB_BACKING_FONT_SIZE,
                                                    lineHeight = SUB_BACKING_LINE_HEIGHT,
                                                    fontWeight = FontWeight.Bold,
                                                ),
                                                isActive = isActive,
                                                sung = sung,
                                                synced = isSynced,
                                                browsing = browsing,
                                                glowAlpha = 0f,
                                                room = 0.dp,
                                                alignEnd = alignEnd,
                                                rise = false,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .revealBelow(subReveal.progress)
                                                    .padding(start = GLOW_ROOM, end = GLOW_ROOM, bottom = GLOW_ROOM)
                                                    .offset(y = -SUB_LYRIC_TUCK)
                                                    .graphicsLayer { alpha = BACKING_ALPHA * SUB_LYRIC_ALPHA },
                                            )
                                        }
                                }
                            }
                        }
                    }
                }
            }

            // Apple Music Sing Vertical Slider Capsule (Right edge)
            AppleMusicSingSlider(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(bottom = 60.dp)
            )

            // Translation & Romanization Floating Toggle Buttons (Bottom Start)
            AnimatedVisibility(
                visible = !isSelectionMode && controlsOpen,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 24.dp, bottom = 24.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RomanizationToggleButton(
                        state = translationUi.romanizationState,
                        showingRomanization = translationUi.showingRomanization,
                        enabled = lines.isNotEmpty(),
                        onClick = translationUi.toggleRomanization,
                    )
                    TranslationToggleButton(
                        state = translationUi.translationState,
                        showingTranslation = translationUi.showingTranslation,
                        enabled = lines.isNotEmpty(),
                        onClick = translationUi.toggleTranslation,
                    )
                }
            }

            // Floating Action Bar for Lyrics Selection Mode
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInVertically(initialOffsetY = { it * 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E1E24).copy(alpha = 0.94f))
                        .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "${selectedIndices.size}/6 seleccionados",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                isSelectionMode = false
                                selectedIndices = emptySet()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Cancelar", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedIndices.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.3f))
                            .clickable(enabled = selectedIndices.isNotEmpty()) {
                                showShareDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Compartir", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Aesthetic Lyric Card Share Dialog
            if (showShareDialog) {
                val selectedLyricLines = selectedIndices.sorted().mapNotNull { lines.getOrNull(it) }
                LyricCardShareDialog(
                    selectedLines = selectedLyricLines,
                    songTitle = songTitle.ifEmpty { "RayMusic" },
                    artistName = artistName.ifEmpty { "Desconocido" },
                    artUrl = artUrl,
                    onDismissRequest = {
                        showShareDialog = false
                        isSelectionMode = false
                        selectedIndices = emptySet()
                    }
                )
            }
        }
    }
}

/**
 * Convenience alias for RayMusicLyrics.
 */
@Composable
fun RayMusic(
    lines: List<LyricLine>,
    positionMs: Long,
    isPlaying: Boolean,
    looking: Boolean = false,
    onSeekToLine: (Long) -> Unit = {},
    controlsOpen: Boolean = true,
    onRevealControls: () -> Unit = {},
    onHideControls: () -> Unit = {},
    songTitle: String = "",
    artistName: String = "",
    artUrl: Any? = null,
    selectionModeTrigger: Int = 0,
    trackId: String = "",
    modifier: Modifier = Modifier,
) {
    RayMusicLyrics(
        lines = lines,
        positionMs = positionMs,
        isPlaying = isPlaying,
        looking = looking,
        onSeekToLine = onSeekToLine,
        controlsOpen = controlsOpen,
        onRevealControls = onRevealControls,
        onHideControls = onHideControls,
        songTitle = songTitle,
        artistName = artistName,
        artUrl = artUrl,
        selectionModeTrigger = selectionModeTrigger,
        trackId = trackId,
        modifier = modifier,
    )
}

@Composable
private fun PanelVoice(
    line: LyricLine,
    clock: MutableLongState,
    style: TextStyle,
    isActive: Boolean,
    sung: Boolean,
    synced: Boolean,
    browsing: Boolean,
    glowAlpha: Float,
    room: Dp,
    alignEnd: Boolean,
    translationProgress: State<Float>? = null,
    rise: Boolean = true,
    modifier: Modifier = Modifier,
) {
    if (line.isWordSynced && !browsing) {
        val tail by animateFloatAsState(
            targetValue = if (sung) 1f else UNSUNG_ALPHA,
            label = "lyricTail",
        )
        SweptLyricLine(
            line = line,
            clock = clock,
            style = style,
            dimAlpha = tail,
            modifier = modifier,
            glowAlpha = glowAlpha,
            glowRoom = room,
            feather = isActive,
            rise = rise,
            alignEnd = alignEnd,
            translationProgress = translationProgress,
        )
    } else if (line.isWordSynced) {
        val tail by animateFloatAsState(
            targetValue = if (sung) 1f else UNSUNG_ALPHA,
            label = "lyricTail",
        )
        SweptLyricLine(
            line = line,
            clock = clock,
            style = style,
            dimAlpha = tail,
            modifier = modifier,
            glowAlpha = 0f,
            glowRoom = room,
            rise = rise,
            alignEnd = alignEnd,
            translationProgress = translationProgress,
        )
    } else {
        val lit by animateFloatAsState(
            targetValue = if (!synced || sung || isActive) 1f else UNSUNG_ALPHA,
            label = "lyricLit",
        )
        var layout by remember(line.text) { mutableStateOf<TextLayoutResult?>(null) }
        Text(
            text = line.text,
            style = style,
            color = Color.White.copy(alpha = lit),
            onTextLayout = { layout = it },
            modifier = modifier.lyricParticles(layout, translationProgress, room).padding(room),
        )
    }
}

@Composable
private fun SweptLyricLine(
    line: LyricLine,
    clock: MutableLongState,
    style: TextStyle,
    dimAlpha: Float,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    glowAlpha: Float = 0f,
    glowRadius: Dp = GLOW_RADIUS,
    glowRoom: Dp = 0.dp,
    feather: Boolean = false,
    rise: Boolean = true,
    alignEnd: Boolean = false,
    translationProgress: State<Float>? = null,
) {
    var layout by remember(line) { mutableStateOf<TextLayoutResult?>(null) }
    val growth = remember { CharGrowth() }

    val phase = remember(line, clock) {
        derivedStateOf(structuralEqualityPolicy()) {
            val now = clock.longValue
            when {
                now < line.animatesFromMs -> PHASE_BEFORE
                now > line.animatesUntilMs -> PHASE_AFTER
                else -> PHASE_MOVING
            }
        }
    }
    val drawnAt: () -> Long = {
        when (phase.value) {
            PHASE_MOVING -> clock.longValue
            PHASE_BEFORE -> line.animatesFromMs - 1
            else -> line.animatesUntilMs + 1
        }
    }

    val room = if (glowRoom > 0.dp) Modifier.padding(glowRoom) else Modifier

    val riseAgainst: (Modifier) -> Modifier = { inner ->
        if (!rise) {
            inner
        } else {
            Modifier
                .drawWithContent {
                    val measured = layout
                    if (measured == null || line.words.isEmpty()) {
                        drawContent()
                    } else {
                        riseWith(
                            layout = measured,
                            line = line,
                            positionMs = drawnAt(),
                            inset = glowRoom.toPx(),
                            peak = WORD_RISE.toPx(),
                            growth = growth,
                        )
                    }
                }
                .then(inner)
        }
    }

    val sweep = Modifier.drawWithContent {
        val position = drawnAt()
        when {
            position >= line.endMs -> drawContent()
            position <= line.timeMs -> Unit
            else -> layout?.let {
                sweepTo(it, line.revealedChars(position), line.sweepSpans, feather)
            }
        }
    }

    Box(
        modifier.lyricParticles(layout, translationProgress, glowRoom),
        contentAlignment = if (alignEnd) Alignment.TopEnd else Alignment.TopStart,
    ) {
        Text(
            text = line.text,
            style = style,
            color = Color.White.copy(alpha = dimAlpha),
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = { layout = it },
            modifier = riseAgainst(room),
        )
        if (glowAlpha > 0.01f) {
            Text(
                text = line.text,
                style = style,
                color = Color.White,
                maxLines = maxLines,
                overflow = overflow,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = glowAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .blur(glowRadius, BlurredEdgeTreatment.Unbounded)
                    .then(room)
                    .drawWithContent {
                        val measured = layout ?: return@drawWithContent
                        glowGrown(
                            layout = measured,
                            line = line,
                            positionMs = drawnAt(),
                            inset = glowRoom.toPx(),
                            peak = WORD_RISE.toPx(),
                            growth = growth,
                        )
                    },
            )
        }
        Text(
            text = line.text,
            style = style,
            color = Color.White,
            maxLines = maxLines,
            overflow = overflow,
            modifier = riseAgainst(
                Modifier
                    .graphicsLayer {
                        compositingStrategy = if (feather) {
                            CompositingStrategy.Offscreen
                        } else {
                            CompositingStrategy.Auto
                        }
                    }
                    .then(room)
                    .then(sweep),
            ),
        )
    }
}

private fun ContentDrawScope.glowGrown(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isGrowing(positionMs)) return
    val em = layout.layoutInput.style.fontSize.toPx()
    val length = layout.layoutInput.text.length
    for (word in line.growingWords) {
        if (positionMs < word.startMs || positionMs > word.restsAtMs) continue
        val span = line.wordSpans[word.index]
        val fall = line.wordFall(word.index, positionMs)
        val charStart = maxOf(0, span.first)
        val charEnd = minOf(span.last, length - 1)
        for (char in charStart..charEnd) {
            word.sampleInto(char - span.first, positionMs, growth)
            if (growth.bloom <= 0.01f) continue
            val visualLine = layout.getLineForOffset(char)
            val from = layout.xOn(char, visualLine, inset)
            val to = layout.xOn(char + 1, visualLine, inset)
            if (to <= from) continue
            val dx = growth.shift * em
            val dy = -growth.rise * peak * fall
            val rowTop = layout.getLineTop(visualLine) + inset
            val bottom = layout.getLineBottom(visualLine) + inset
            val overhang = (to - from) * (growth.scale - 1f) / 2f
            clipRect(
                left = from - overhang + dx,
                top = rowTop - peak * GROW_HEADROOM,
                right = to + overhang + dx,
                bottom = bottom,
            ) {
                translate(left = dx, top = dy) {
                    scale(
                        growth.scale,
                        growth.scale,
                        Offset((from + to) / 2f, (rowTop + bottom) / 2f),
                    ) {
                        this@glowGrown.drawContent()
                    }
                }
                drawRect(
                    color = Color.White.copy(alpha = growth.bloom),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
    }
}

private fun ContentDrawScope.riseWith(
    layout: TextLayoutResult,
    line: LyricLine,
    positionMs: Long,
    inset: Float,
    peak: Float,
    growth: CharGrowth,
) {
    if (!line.isLifted(positionMs)) {
        drawContent()
        return
    }
    val em = layout.layoutInput.style.fontSize.toPx()
    for (visualLine in 0 until layout.lineCount) {
        val lineStart = layout.getLineStart(visualLine)
        val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
        val top = layout.getLineTop(visualLine) + inset
        val bottom = layout.getLineBottom(visualLine) + inset
        var at = lineStart
        var edge = layout.getLineLeft(visualLine) + inset
        for (index in line.words.indices) {
            val span = line.wordSpans[index]
            val start = maxOf(span.first, lineStart)
            val end = minOf(span.last + 1, lineEnd)
            if (start >= end) continue
            val held = line.growingAt(index)?.takeIf { positionMs in it.startMs..it.restsAtMs }
            val lift = line.wordLift(index, positionMs)
            if (held == null && lift <= 0.01f) continue
            val from = layout.xOn(start, visualLine, inset)
            val to = layout.xOn(end, visualLine, inset)
            if (to <= from) continue
            if (start > at) sliceRisen(edge, top, from, bottom, 0f)
            if (held != null) {
                growEach(
                    layout, held, line, positionMs, visualLine,
                    start, end, top, bottom, inset, peak, em, growth,
                )
            } else {
                sliceRisen(from, top - peak, to, bottom, -lift * peak)
            }
            at = end
            edge = to
        }
        if (at < lineEnd) {
            sliceRisen(edge, top, layout.getLineRight(visualLine) + inset, bottom, 0f)
        }
    }
}

private fun ContentDrawScope.growEach(
    layout: TextLayoutResult,
    word: GrowingWord,
    line: LyricLine,
    positionMs: Long,
    visualLine: Int,
    start: Int,
    end: Int,
    top: Float,
    bottom: Float,
    inset: Float,
    peak: Float,
    em: Float,
    growth: CharGrowth,
) {
    val fall = line.wordFall(word.index, positionMs)
    val first = line.wordSpans[word.index].first
    val ceiling = top - peak * GROW_HEADROOM
    val middle = (top + bottom) / 2f
    val length = layout.layoutInput.text.length
    val charStart = maxOf(0, start)
    val charEnd = minOf(end, length)
    for (char in charStart until charEnd) {
        word.sampleInto(char - first, positionMs, growth)
        val from = layout.xOn(char, visualLine, inset)
        val to = layout.xOn(char + 1, visualLine, inset)
        if (to <= from) continue
        val dx = growth.shift * em
        val dy = -growth.rise * peak * fall
        val overhang = (to - from) * (growth.scale - 1f) / 2f
        clipRect(
            left = from - overhang + dx,
            top = ceiling,
            right = to + overhang + dx,
            bottom = bottom,
        ) {
            translate(left = dx) {
                scale(growth.scale, growth.scale, Offset((from + to) / 2f, middle)) {
                    this@growEach.drawContent()
                }
            }
        }
    }
}

private fun TextLayoutResult.xOn(offset: Int, visualLine: Int, inset: Float): Float {
    val left = getLineLeft(visualLine) + inset
    val right = getLineRight(visualLine) + inset
    return when {
        offset <= getLineStart(visualLine) -> left
        offset >= getLineEnd(visualLine, visibleEnd = true) -> right
        else -> (getHorizontalPosition(offset, usePrimaryDirection = true) + inset)
            .coerceIn(left, right)
    }
}

private fun ContentDrawScope.sliceRisen(
    from: Float,
    top: Float,
    to: Float,
    bottom: Float,
    dy: Float,
) {
    if (to <= from) return
    clipRect(left = from, top = top, right = to, bottom = bottom) {
        translate(top = dy) { this@sliceRisen.drawContent() }
    }
}

private fun horizontalAt(
    layout: TextLayoutResult,
    chars: Float,
    visualLine: Int,
    spans: List<IntRange>,
): Float {
    val lineStart = layout.getLineStart(visualLine)
    val lineEnd = layout.getLineEnd(visualLine, visibleEnd = true)
    val word = spans.firstOrNull { chars >= it.first && chars < it.last + 1 }
    if (word != null && word.first >= lineStart && word.last + 1 <= lineEnd) {
        val from = layout.xOn(word.first, visualLine, 0f)
        val to = layout.xOn(word.last + 1, visualLine, 0f)
        return from + (to - from) * (chars - word.first) / (word.last + 1 - word.first)
    }
    val index = chars.toInt().coerceIn(lineStart, lineEnd)
    val here = layout.xOn(index, visualLine, 0f)
    val next = layout.xOn((index + 1).coerceAtMost(lineEnd), visualLine, 0f)
    return here + (next - here) * (chars - index)
}

private fun ContentDrawScope.sweepTo(
    layout: TextLayoutResult,
    revealedChars: Float,
    spans: List<IntRange>,
    feather: Boolean,
) {
    if (revealedChars <= 0f) return
    if (revealedChars >= layout.layoutInput.text.length) {
        drawContent()
        return
    }
    for (visualLine in 0 until layout.lineCount) {
        val start = layout.getLineStart(visualLine)
        if (revealedChars <= start) return
        val end = layout.getLineEnd(visualLine, visibleEnd = true)
        val cut = revealedChars < end
        val right = if (cut) {
            horizontalAt(layout, revealedChars, visualLine, spans)
        } else {
            layout.getLineRight(visualLine)
        }
        val top = layout.getLineTop(visualLine)
        val bottom = layout.getLineBottom(visualLine)
        clipRect(
            left = layout.getLineLeft(visualLine),
            top = top,
            right = right,
            bottom = bottom,
        ) {
            this@sweepTo.drawContent()
        }
        if (!feather || !cut) continue
        clipRect(top = top, bottom = bottom) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.White,
                    1f to Color.Transparent,
                    startX = (right - WIPE_FEATHER.toPx()).coerceAtLeast(layout.getLineLeft(visualLine)),
                    endX = right,
                ),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

private fun LyricLine.withoutBracketPunctuation(): LyricLine = copy(
    text = text.stripParens(),
    words = words.mapNotNull { word ->
        word.withoutChars { it == '(' || it == ')' }
    },
)

private fun String.stripParens(): String = replace("(", "").replace(")", "").trim()

private fun keepScrollInList(listState: LazyListState) = object : NestedScrollConnection {
    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = available

    override suspend fun onPreFling(available: Velocity): Velocity =
        if (available.y > 0f && !listState.canScrollBackward) available else Velocity.Zero

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}

private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    .drawWithContent {
        drawContent()
        val fade = 16.dp.toPx()
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = fade,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Black, Color.Transparent),
                startY = size.height - fade,
                endY = size.height,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private const val TAP_SLOP_FACTOR = 2.5f

@Composable
internal fun Modifier.revealLyricsControlsOnTap(
    enabled: Boolean,
    onReveal: () -> Unit,
): Modifier {
    val currentOnReveal = rememberUpdatedState(onReveal)
    return pointerInput(enabled) {
        if (!enabled) return@pointerInput
        val tapSlop = viewConfiguration.touchSlop * TAP_SLOP_FACTOR
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (down.position.y < size.height / 2f) return@awaitEachGesture
            var dragged = false
            do {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if ((change.position - down.position).getDistance() > tapSlop ||
                    event.changes.size > 1
                ) {
                    dragged = true
                } else if (change.positionChange() != Offset.Zero) {
                    change.consume()
                }
                if (!change.pressed) {
                    if (!dragged) {
                        change.consume()
                        currentOnReveal.value()
                    }
                    break
                }
            } while (true)
        }
    }
}

@Composable
private fun LyricsSkeleton(modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "lyricsSkeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(SKELETON_PERIOD_MS, easing = LinearEasing),
        ),
        label = "sweep",
    )
    BoxWithConstraints(modifier.padding(top = 40.dp).padding(horizontal = LYRICS_GUTTER)) {
        val column = maxWidth
        Column(verticalArrangement = Arrangement.spacedBy(SKELETON_BLOCK_GAP)) {
            SKELETON_BLOCKS.forEach { rows ->
                Column(verticalArrangement = Arrangement.spacedBy(SKELETON_LEADING)) {
                    rows.forEach { fraction ->
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(SKELETON_BAR)
                                .clip(RoundedCornerShape(4.dp))
                                .drawWithCache {
                                    val full = column.toPx()
                                    val band = full * 0.45f
                                    val startX = -band + sweep.value * (full + band * 2)
                                    val brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.10f),
                                            Color.White.copy(alpha = 0.26f),
                                            Color.White.copy(alpha = 0.10f),
                                        ),
                                        startX = startX,
                                        endX = startX + band,
                                    )
                                    onDrawWithContent { drawRect(brush) }
                                },
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.lyricBlur(radius: Dp): Modifier =
    if (radius > 0.dp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !com.mrtdk.liquid_glass.BuildConfig.IS_LITE) {
        this.blur(radius, BlurredEdgeTreatment.Unbounded)
    } else {
        this
    }
