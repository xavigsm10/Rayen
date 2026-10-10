package com.mrtdk.liquid_glass.ui.ipod

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.sqrt

enum class IPodWheelButton {
    NONE,
    MENU,
    PLAY_PAUSE,
    PREV,
    NEXT,
    SELECT
}

data class IPodWheelColors(
    val wheelGradient: List<Color>,
    val wheelBorder: Color,
    val centerButtonGradient: List<Color>,
    val centerButtonBorder: Color,
    val iconColor: Color,
    val pressedColor: Color,
    val centerPressedColor: Color
)

object IPodColorPalettes {
    val Silver = IPodWheelColors(
        wheelGradient = listOf(Color(0xFFE9ECF0), Color(0xFFDCE0E6), Color(0xFFD4D8DF)),
        wheelBorder = Color(0xFFB0B5BD),
        centerButtonGradient = listOf(Color(0xFFF6F8FA), Color(0xFFE6EAEF)),
        centerButtonBorder = Color(0xFFC0C5CC),
        iconColor = Color(0xFF4A4E57),
        pressedColor = Color(0x33000000),
        centerPressedColor = Color(0x22000000)
    )

    val White = IPodWheelColors(
        wheelGradient = listOf(Color(0xFFF1F3F5), Color(0xFFE4E7EA)),
        wheelBorder = Color(0xFFCCD0D6),
        centerButtonGradient = listOf(Color(0xFFFFFFFF), Color(0xFFF0F2F5)),
        centerButtonBorder = Color(0xFFD0D4DA),
        iconColor = Color(0xFF5D636E),
        pressedColor = Color(0x22000000),
        centerPressedColor = Color(0x18000000)
    )

    val Black = IPodWheelColors(
        wheelGradient = listOf(Color(0xFF2C2D31), Color(0xFF232427)),
        wheelBorder = Color(0xFF161719),
        centerButtonGradient = listOf(Color(0xFF1E1F22), Color(0xFF151618)),
        centerButtonBorder = Color(0xFF101113),
        iconColor = Color(0xFFB4B8C2),
        pressedColor = Color(0x44FFFFFF),
        centerPressedColor = Color(0x33FFFFFF)
    )

    val U2 = IPodWheelColors(
        wheelGradient = listOf(Color(0xFFDC1C27), Color(0xFFBA131D)),
        wheelBorder = Color(0xFF8E0A12),
        centerButtonGradient = listOf(Color(0xFF18181A), Color(0xFF0F0F10)),
        centerButtonBorder = Color(0xFF050506),
        iconColor = Color(0xFF1A1A1A),
        pressedColor = Color(0x33000000),
        centerPressedColor = Color(0x33FFFFFF)
    )

    fun forStyle(style: String): IPodWheelColors {
        return when (style.lowercase()) {
            "white" -> White
            "black" -> Black
            "u2" -> U2
            else -> Silver
        }
    }
}

/**
 * Authentic iPod Click Wheel with rotary drag detection, quadrant buttons, center button,
 * satisfying mechanical clicks, and haptic pulses.
 */
@Composable
fun IPodClickWheel(
    modifier: Modifier = Modifier,
    wheelSize: Dp = 250.dp,
    colorStyle: String = "silver",
    soundEnabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    onScroll: (steps: Int) -> Unit,
    onMenu: () -> Unit,
    onSelect: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current

    LaunchedEffect(Unit) {
        IPodAudio.init(context)
    }

    val palette = remember(colorStyle) { IPodColorPalettes.forStyle(colorStyle) }
    var pressedButton by remember { mutableStateOf(IPodWheelButton.NONE) }

    // Rotary gesture tracking state
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var accumulatedDelta by remember { mutableFloatStateOf(0f) }
    var isDraggingWheel by remember { mutableStateOf(false) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val stepThresholdDegrees = 16f // Each 16 degrees triggers 1 click tick

    Box(
        modifier = modifier
            .size(wheelSize)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        // Outer Wheel with circular drag & tap gesture detector
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 12.dp, shape = CircleShape, ambientColor = Color(0x40000000), spotColor = Color(0x60000000))
                .clip(CircleShape)
                .background(Brush.radialGradient(palette.wheelGradient))
                .border(1.5.dp, palette.wheelBorder, CircleShape)
                .pointerInput(soundEnabled, hapticsEnabled) {
                    val centerPx = size.width / 2f
                    val centerRadiusPx = centerPx * 0.38f // Inner select button radius

                    detectDragGestures(
                        onDragStart = { offset ->
                            val dx = offset.x - centerPx
                            val dy = offset.y - centerPx
                            val r = hypot(dx, dy)
                            if (r > centerRadiusPx && r <= centerPx) {
                                isDraggingWheel = true
                                lastAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                accumulatedDelta = 0f
                                totalDragDistance = 0f
                            } else {
                                isDraggingWheel = false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (!isDraggingWheel) return@detectDragGestures

                            val currentPos = change.position
                            val dx = currentPos.x - centerPx
                            val dy = currentPos.y - centerPx
                            val r = hypot(dx, dy)
                            if (r > centerRadiusPx * 0.7f && r <= centerPx * 1.35f) {
                                val currentAngle = (Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 360f) % 360f
                                var delta = currentAngle - lastAngle
                                if (delta > 180f) delta -= 360f
                                else if (delta < -180f) delta += 360f

                                totalDragDistance += kotlin.math.abs(delta)
                                accumulatedDelta += delta
                                lastAngle = currentAngle

                                if (kotlin.math.abs(accumulatedDelta) >= stepThresholdDegrees) {
                                    val steps = (accumulatedDelta / stepThresholdDegrees).toInt()
                                    if (steps != 0) {
                                        accumulatedDelta -= steps * stepThresholdDegrees
                                        IPodAudio.tick(view, playAudio = soundEnabled, triggerHaptic = hapticsEnabled)
                                        onScroll(steps)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            isDraggingWheel = false
                            accumulatedDelta = 0f
                        },
                        onDragCancel = {
                            isDraggingWheel = false
                            accumulatedDelta = 0f
                        }
                    )
                }
                .pointerInput(soundEnabled, hapticsEnabled) {
                    val centerPx = size.width / 2f
                    val centerRadiusPx = centerPx * 0.38f

                    detectTapGestures(
                        onPress = { offset ->
                            val dx = offset.x - centerPx
                            val dy = offset.y - centerPx
                            val r = hypot(dx, dy)

                            val btn = if (r <= centerRadiusPx) {
                                IPodWheelButton.SELECT
                            } else if (r <= centerPx) {
                                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                when {
                                    angle in -135f..-45f -> IPodWheelButton.MENU
                                    angle in 45f..135f -> IPodWheelButton.PLAY_PAUSE
                                    angle in -45f..45f -> IPodWheelButton.NEXT
                                    else -> IPodWheelButton.PREV
                                }
                            } else {
                                IPodWheelButton.NONE
                            }

                            pressedButton = btn
                            tryAwaitRelease()
                            pressedButton = IPodWheelButton.NONE
                        },
                        onTap = { offset ->
                            val dx = offset.x - centerPx
                            val dy = offset.y - centerPx
                            val r = hypot(dx, dy)

                            if (r <= centerRadiusPx) {
                                IPodAudio.buttonClick(view, playAudio = soundEnabled)
                                onSelect()
                            } else if (r <= centerPx) {
                                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                IPodAudio.buttonClick(view, playAudio = soundEnabled)
                                when {
                                    angle in -135f..-45f -> onMenu()
                                    angle in 45f..135f -> onPlayPause()
                                    angle in -45f..45f -> onNext()
                                    else -> onPrev()
                                }
                            }
                        }
                    )
                }
        ) {
            // Quadrant Pressed Visual Highlights
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val innerR = size.width * 0.19f
                val outerR = size.width / 2f

                // Top (MENU) highlight
                if (pressedButton == IPodWheelButton.MENU) {
                    drawArc(
                        color = palette.pressedColor,
                        startAngle = 225f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
                // Bottom (PLAY/PAUSE) highlight
                if (pressedButton == IPodWheelButton.PLAY_PAUSE) {
                    drawArc(
                        color = palette.pressedColor,
                        startAngle = 45f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
                // Right (NEXT) highlight
                if (pressedButton == IPodWheelButton.NEXT) {
                    drawArc(
                        color = palette.pressedColor,
                        startAngle = 315f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
                // Left (PREV) highlight
                if (pressedButton == IPodWheelButton.PREV) {
                    drawArc(
                        color = palette.pressedColor,
                        startAngle = 135f,
                        sweepAngle = 90f,
                        useCenter = true
                    )
                }
            }

            // Top: MENU
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
            ) {
                Text(
                    text = "MENU",
                    color = palette.iconColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = 1.sp
                )
            }

            // Bottom: PLAY / PAUSE (▶❚❚)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 18.dp)
            ) {
                Text(
                    text = "▶❚❚",
                    color = palette.iconColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Left: PREV (|◀◀)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 18.dp)
            ) {
                Text(
                    text = "|◀◀",
                    color = palette.iconColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }

            // Right: NEXT (▶▶|)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 18.dp)
            ) {
                Text(
                    text = "▶▶|",
                    color = palette.iconColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // Center SELECT Button
        val centerSize = wheelSize * 0.38f
        val isSelectPressed = pressedButton == IPodWheelButton.SELECT
        val centerScale by animateFloatAsState(
            targetValue = if (isSelectPressed) 0.96f else 1.0f,
            animationSpec = tween(50)
        )

        Box(
            modifier = Modifier
                .size(centerSize)
                .shadow(
                    elevation = if (isSelectPressed) 1.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = Color(0x33000000),
                    spotColor = Color(0x44000000)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (isSelectPressed) {
                            palette.centerButtonGradient.map { it.copy(alpha = 0.85f) }
                        } else {
                            palette.centerButtonGradient
                        }
                    )
                )
                .border(1.2.dp, palette.centerButtonBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Subtle concavity highlight on the center button
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0x15000000)),
                        center = Offset(size.width * 0.45f, size.height * 0.45f),
                        radius = size.width * 0.5f
                    )
                )
                if (isSelectPressed) {
                    drawCircle(color = palette.centerPressedColor)
                }
            }
        }
    }
}
