package com.mrtdk.liquid_glass.ui.ipod

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlin.math.abs
import kotlin.math.sin

/**
 * Data item for 3D Cover Flow.
 */
data class CoverFlowCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String? = null,
    val icon: ImageVector? = null,
    val gradientColors: List<Color> = listOf(Color(0xFF2C3E50), Color(0xFF000000)),
    val targetDestination: IPodScreenDestination? = null,
    val badge: String? = null,
    val isPlaying: Boolean = false,
    val extraData: Any? = null
)

/**
 * Authentic Apple 3D Cover Flow carousel.
 * Smoothly animates covers receding in 3D perspective with realistic continuous angles,
 * scaling, depth sorting (zIndex), mirrored reflections underneath, and touch gestures.
 */
@Composable
fun IPodCoverFlow(
    modifier: Modifier = Modifier,
    cards: List<CoverFlowCard>,
    selectedIndex: Int,
    onIndexChange: ((Int) -> Unit)? = null,
    onCardClick: ((Int, CoverFlowCard) -> Unit)? = null
) {
    if (cards.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF0A0C10)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sin elementos en Cover Flow",
                color = Color.White,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
        return
    }

    // High-performance spring animation: smooth, zero jitter, zero overshoot bounce
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat().coerceIn(0f, (cards.size - 1).toFloat()),
        animationSpec = spring(
            dampingRatio = 0.88f,
            stiffness = 520f
        ),
        label = "coverFlowOffset"
    )

    val currentCard = cards.getOrNull(selectedIndex) ?: cards.first()
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF14171E),
                        Color(0xFF0A0B0E),
                        Color(0xFF050507)
                    )
                )
            )
            .pointerInput(cards.size, selectedIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { accumulatedDrag = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        val threshold = 36.dp.toPx()
                        if (abs(accumulatedDrag) >= threshold) {
                            val step = if (accumulatedDrag > 0) -1 else 1
                            val targetIndex = (selectedIndex + step).coerceIn(0, cards.size - 1)
                            if (targetIndex != selectedIndex) {
                                onIndexChange?.invoke(targetIndex)
                            }
                            accumulatedDrag = 0f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val stageWidth = maxWidth
        val cardSize = minOf(stageWidth * 0.44f, 130.dp)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── 3D COVER FLOW STAGE ─────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cardSize * 1.48f),
                contentAlignment = Alignment.Center
            ) {
                // Subtle polished reflective ground horizon line
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(1.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationY = cardSize.value * 0.49f * density
                            alpha = 0.28f
                        }
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xFF6B7280),
                                    Color.White,
                                    Color(0xFF6B7280),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Render covers within visible range of animated index
                cards.forEachIndexed { index, card ->
                    val dist = index - animatedIndex
                    val absD = abs(dist)

                    // Render cascade up to +/- 4.8 covers for smooth appearance without popping
                    if (absD <= 4.8f) {
                        val isCenter = absD < 0.25f

                        // Continuous rotation formula: 0 at center, smoothly curving to +/- 58 degrees
                        val clampedDist = dist.coerceIn(-1f, 1f)
                        val smoothFactor = sin(clampedDist * (Math.PI / 2.0).toFloat())
                        val rotationY = -smoothFactor * 58f

                        // Continuous translation curve: Hermite curve from center to folded stacks
                        val centerSpacing = cardSize.value * 0.62f
                        val stackStep = cardSize.value * 0.26f
                        val sign = if (dist > 0f) 1f else if (dist < 0f) -1f else 0f
                        val transX = if (absD <= 1f) {
                            val t = absD
                            val smoothT = t * t * (3f - 2f * t)
                            sign * (smoothT * centerSpacing)
                        } else {
                            sign * (centerSpacing + (absD - 1f) * stackStep)
                        }

                        // Continuous scale curve
                        val scale = (1f - (absD.coerceIn(0f, 1f) * 0.16f) - ((absD - 1f).coerceAtLeast(0f) * 0.04f))
                            .coerceIn(0.70f, 1f)

                        // Z-Index sorting: center card always on top
                        val cardZIndex = 200f - absD * 20f

                        // Smooth fade-in / fade-out at edges
                        val alpha = when {
                            absD <= 3.0f -> (1f - absD * 0.12f).coerceIn(0.6f, 1f)
                            absD < 4.8f -> {
                                val fadeProgress = (4.8f - absD) / (4.8f - 3.0f)
                                (0.6f * fadeProgress).coerceIn(0f, 0.6f)
                            }
                            else -> 0f
                        }

                        // Fold shadow dimming (realistic lighting on turned cards)
                        val shadowDim = (absD.coerceIn(0f, 1f) * 0.38f)

                        Box(
                            modifier = Modifier
                                .size(cardSize)
                                .zIndex(cardZIndex)
                                .clickable {
                                    if (isCenter) {
                                        onCardClick?.invoke(index, card)
                                    } else {
                                        onIndexChange?.invoke(index)
                                    }
                                }
                                .graphicsLayer {
                                    cameraDistance = 28f * density
                                    this.rotationY = rotationY
                                    this.scaleX = scale
                                    this.scaleY = scale
                                    this.translationX = transX * density
                                    this.alpha = alpha
                                }
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                // Upper: Cover Art / Graphic
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .shadow(
                                            elevation = if (isCenter) 16.dp else 4.dp,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Brush.linearGradient(card.gradientColors))
                                        .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!card.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(card.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = card.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (card.icon != null) {
                                        Icon(
                                            imageVector = card.icon,
                                            contentDescription = card.title,
                                            tint = Color.White,
                                            modifier = Modifier.size(cardSize * 0.44f)
                                        )
                                    } else {
                                        Text(
                                            text = card.title.take(2).uppercase(),
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Dynamic lighting shadow overlay for turned cards
                                    if (shadowDim > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = shadowDim))
                                        )
                                    }

                                    // Badge on the card corner if playing
                                    if (card.isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(5.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0xE600C853))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }

                                // Lower: Mirrored reflection fading out smoothly into dark floor
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(cardSize * 0.38f)
                                        .graphicsLayer {
                                            scaleY = -1f
                                            this.alpha = 0.28f
                                        }
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Brush.linearGradient(card.gradientColors)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!card.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = card.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (card.icon != null) {
                                        Icon(
                                            imageVector = card.icon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(cardSize * 0.44f)
                                        )
                                    }

                                    // Gradient mask fading the reflection into black
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color(0xB3050507),
                                                    Color(0xFF050507)
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // ── ACTIVE COVER TITLE, SUBTITLE & BADGE ────────────────
            if (!currentCard.badge.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (currentCard.isPlaying) {
                                Brush.horizontalGradient(listOf(Color(0xFF00C853), Color(0xFF009688)))
                            } else {
                                Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF1D4ED8)))
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (currentCard.isPlaying) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                        Text(
                            text = currentCard.badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.4.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
            }

            Text(
                text = currentCard.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = currentCard.subtitle,
                fontSize = 11.sp,
                color = Color(0xFFA5ACB8),
                fontFamily = FontFamily.SansSerif,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Index Position Counter (e.g. "4 de 18")
            Text(
                text = "${selectedIndex + 1} de ${cards.size}",
                fontSize = 9.sp,
                color = Color(0xFF6B7280),
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
