package com.mrtdk.liquid_glass.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.mrtdk.liquid_glass.ui.NavTabItem

/**
 * Menú de navegación por defecto de Echo-Music adaptado con las opciones y estilo de RayMusic:
 * - Material 3 Expressive Floating Toolbar con píldora deslizante activa animada.
 * - Expansión suave de la etiqueta de texto en la pestaña activa con física de resortes bouncy.
 * - Responde con compresión al presionar y soporte completo tanto en modo sólido como con Liquid Glass.
 */
@Composable
fun M3ExpressiveFloatingNavBar(
    items: List<NavTabItem>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    isDarkMode: Boolean,
    accentColor: Color,
    isSolid: Boolean,
    solidBgColor: Color,
    containerColor: Color,
    backdrop: Backdrop,
    isLightweight: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemWidths = remember { mutableStateMapOf<Int, Dp>() }
    val itemPositions = remember { mutableStateMapOf<Int, Dp>() }

    val targetWidth = itemWidths[selectedIndex] ?: 0.dp
    val targetPosition = itemPositions[selectedIndex] ?: 0.dp

    val slidingPillWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3PillWidth"
    )

    val slidingPillOffset by animateDpAsState(
        targetValue = targetPosition,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3PillOffset"
    )

    val pillShape = RoundedCornerShape(percent = 50)
    val pillBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)

    val toolbarModifier = if (isSolid) {
        Modifier
            .shadow(elevation = 6.dp, shape = pillShape, spotColor = Color.Black.copy(alpha = 0.35f))
            .clip(pillShape)
            .background(solidBgColor)
            .border(width = 1.dp, color = pillBorderColor, shape = pillShape)
    } else {
        Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { pillShape },
                effects = {
                    if (!isLightweight) {
                        vibrancy()
                        blur(6.dp.toPx() * 0.33f)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            lens(
                                refractionHeight = 16.dp.toPx() * 0.33f,
                                refractionAmount = 24.dp.toPx() * 0.33f,
                                depthEffect = true,
                                chromaticAberration = false
                            )
                        }
                    } else {
                        blur(3.dp.toPx() * 0.33f)
                    }
                },
                highlight = { Highlight.Default.copy(alpha = 0.25f) },
                shadow = { Shadow.Default },
                onDrawSurface = { drawRect(containerColor) },
                backdropScale = 0.33f
            )
            .border(width = 0.8.dp, color = pillBorderColor, shape = pillShape)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .then(toolbarModifier)
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            Box(modifier = Modifier.height(IntrinsicSize.Min)) {
                // Indicador deslizante activo (active tab sliding pill)
                if (targetWidth > 0.dp) {
                    val indicatorBgColor = if (isDarkMode) {
                        Color.White.copy(alpha = 0.16f)
                    } else {
                        accentColor.copy(alpha = 0.14f)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = slidingPillOffset)
                            .width(slidingPillWidth)
                            .fillMaxHeight()
                            .background(
                                color = indicatorBgColor,
                                shape = RoundedCornerShape(22.dp)
                            )
                    )
                }

                // Elementos de la barra
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    items.forEach { tab ->
                        val isSelected = tab.index == selectedIndex
                        M3ExpressiveNavBarItem(
                            item = tab,
                            selected = isSelected,
                            isDarkMode = isDarkMode,
                            accentColor = accentColor,
                            onClick = { onTabSelected(tab.index) },
                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                itemWidths[tab.index] = with(density) { coordinates.size.width.toDp() }
                                itemPositions[tab.index] = with(density) { coordinates.positionInParent().x.toDp() }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun M3ExpressiveNavBarItem(
    item: NavTabItem,
    selected: Boolean,
    isDarkMode: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "m3ItemPressScale"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3IconScale"
    )

    val horizontalPadding by animateDpAsState(
        targetValue = if (selected) 14.dp else 10.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "m3ItemPadding"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            accentColor
        } else {
            if (isDarkMode) Color.White.copy(alpha = 0.65f) else Color(0xFF505054)
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "m3ItemContentColor"
    )

    Row(
        modifier = modifier
            .scale(pressScale)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .padding(horizontal = horizontalPadding, vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.iconRes != null) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = stringResource(id = item.titleRes),
                tint = contentColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
            )
        }

        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                    expandHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) +
                    shrinkHorizontally(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = item.titleRes),
                    color = contentColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
