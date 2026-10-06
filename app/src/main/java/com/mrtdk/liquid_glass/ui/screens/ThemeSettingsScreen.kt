package com.mrtdk.liquid_glass.ui.screens

import android.graphics.Color.colorToHSV
import android.graphics.Color.HSVToColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrtdk.liquid_glass.R
import com.mrtdk.liquid_glass.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit
) {
    val themeMode by ThemeManager.themeMode.collectAsState()
    val isDynamicTheme by ThemeManager.isDynamicTheme.collectAsState()
    val pureBlack by ThemeManager.pureBlack.collectAsState()
    val selectedThemeColor by ThemeManager.selectedThemeColor.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ThemeManager.backgroundColor)
            .statusBarsPadding()
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(id = R.drawable.flecha_atras),
                    contentDescription = stringResource(R.string.back_action),
                    tint = ThemeManager.accentColor,
                    modifier = Modifier.size(20.dp).offset(x = (-1).dp)
                )
            }
            Text(
                text = stringResource(R.string.theme_colors),
                color = ThemeManager.textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Theme Mode Section (Apple Music / iOS style: Claro & Oscuro first, then Automático & AMOLED)
            item {
                Text(
                    text = stringResource(R.string.theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeManager.accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Row 1: Claro & Oscuro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ThemeModeCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.theme_light_mode),
                            icon = Icons.Rounded.LightMode,
                            isSelected = themeMode == ThemeManager.MODE_LIGHT,
                            onClick = {
                                ThemeManager.setThemeMode(ThemeManager.MODE_LIGHT, isSystemDark)
                            }
                        )
                        ThemeModeCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.theme_dark_mode_default),
                            icon = Icons.Rounded.DarkMode,
                            isSelected = themeMode == ThemeManager.MODE_DARK && !pureBlack,
                            onClick = {
                                ThemeManager.setThemeMode(ThemeManager.MODE_DARK, isSystemDark)
                                ThemeManager.setPureBlack(false)
                            }
                        )
                    }
                    // Row 2: Automático (Sistema) & Negro puro (AMOLED)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ThemeModeCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.dark_theme_follow_system),
                            icon = Icons.Rounded.BrightnessAuto,
                            isSelected = themeMode == ThemeManager.MODE_SYSTEM,
                            onClick = {
                                ThemeManager.setThemeMode(ThemeManager.MODE_SYSTEM, isSystemDark)
                            }
                        )
                        ThemeModeCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.theme_amoled),
                            icon = Icons.Rounded.Contrast,
                            isSelected = themeMode == ThemeManager.MODE_AMOLED || (themeMode == ThemeManager.MODE_DARK && pureBlack),
                            onClick = {
                                ThemeManager.setThemeMode(ThemeManager.MODE_AMOLED, isSystemDark)
                            }
                        )
                    }
                }
            }

            // Divider
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = ThemeManager.dividerColor
                )
            }

            // Color Palette / Dynamic Panel Section
            item {
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = ThemeManager.accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ThemeManager.surfaceColor
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = BorderStroke(1.dp, ThemeManager.dividerColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Dynamic switch row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    val newDynamic = !isDynamicTheme
                                    ThemeManager.setDynamicTheme(newDynamic)
                                    if (newDynamic) {
                                        ThemeManager.setSelectedThemeColor(ThemeManager.DefaultThemeColor)
                                    }
                                }
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ThemeManager.accentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.palette),
                                        contentDescription = null,
                                        tint = ThemeManager.accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = stringResource(R.string.palette_dynamic),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ThemeManager.textColor
                                    )
                                    Text(
                                        text = stringResource(R.string.theme_dynamic_color_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ThemeManager.subtextColor
                                    )
                                }
                            }
                            Switch(
                                checked = isDynamicTheme,
                                onCheckedChange = { checked ->
                                    ThemeManager.setDynamicTheme(checked)
                                    if (checked) {
                                        ThemeManager.setSelectedThemeColor(ThemeManager.DefaultThemeColor)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = ThemeManager.accentColor
                                )
                            )
                        }

                        // HSV Color Picker panel (appears when dynamic palette is disabled)
                        AnimatedVisibility(
                            visible = !isDynamicTheme,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            HsvColorPicker(
                                initialColor = selectedThemeColor,
                                onColorCommit = { newColor ->
                                    ThemeManager.setSelectedThemeColor(newColor)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeModeCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "borderWidth"
    )

    val borderColor = if (isSelected) {
        ThemeManager.accentColor
    } else {
        ThemeManager.dividerColor
    }

    val backgroundBrush = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                ThemeManager.accentColor.copy(alpha = 0.22f),
                ThemeManager.accentColor.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                ThemeManager.surfaceColor,
                ThemeManager.surfaceColor
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(22.dp))
            .background(backgroundBrush)
            .border(borderWidth, borderColor, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 20.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) ThemeManager.accentColor else ThemeManager.subtextColor,
                modifier = Modifier.size(30.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) ThemeManager.accentColor else ThemeManager.textColor
            )
        }
    }
}

@Composable
fun HsvColorPicker(
    modifier: Modifier = Modifier,
    initialColor: Color,
    onColorCommit: (Color) -> Unit
) {
    var hsv by remember {
        val h = FloatArray(3)
        colorToHSV(initialColor.toArgb(), h)
        if (h[1] == 0f) h[1] = 0.85f // default saturation if black/white
        if (h[2] == 0f) h[2] = 0.95f // default value
        mutableStateOf(h)
    }

    LaunchedEffect(initialColor) {
        val currentC = HSVToColor(hsv)
        if (currentC != initialColor.toArgb()) {
            val h = FloatArray(3)
            colorToHSV(initialColor.toArgb(), h)
            if (h[1] == 0f) h[1] = 0.85f
            if (h[2] == 0f) h[2] = 0.95f
            hsv = h
        }
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1]) }
    var value by remember { mutableFloatStateOf(hsv[2]) }

    LaunchedEffect(hsv) {
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
    }

    fun getLocalColor(): Color =
        Color(HSVToColor(floatArrayOf(hue, saturation, value)))

    fun commitColor() {
        hsv = floatArrayOf(hue, saturation, value)
        onColorCommit(getLocalColor())
    }

    val presetColors = remember {
        listOf(
            Color(0xFFFA243C), // Apple Music Red
            Color(0xFF007AFF), // iOS Blue
            Color(0xFFBF5AF2), // Purple
            Color(0xFF34C759), // Emerald Green
            Color(0xFFFF9500), // Sunset Orange
            Color(0xFFFF2D55), // Hot Pink
            Color(0xFF30B0C7), // Teal
            Color(0xFFFFCC00), // Amber Gold
            Color(0xFFED5564)  // Echo Coral
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Live Color Preview Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(getLocalColor())
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            val hexString = String.format("#%06X", 0xFFFFFF and getLocalColor().toArgb())
            Text(
                text = hexString,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }

        // Quick Preset Swatches
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(presetColors) { preset ->
                val isSelectedPreset = preset.toArgb() == getLocalColor().toArgb()
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset)
                        .border(
                            width = if (isSelectedPreset) 3.dp else 1.dp,
                            color = if (isSelectedPreset) Color.White else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable {
                            val h = FloatArray(3)
                            colorToHSV(preset.toArgb(), h)
                            hue = h[0]
                            saturation = h[1]
                            value = h[2]
                            hsv = h
                            onColorCommit(preset)
                        }
                )
            }
        }

        // Hue Slider
        CustomColorSlider(
            value = hue,
            onValueChange = {
                hue = it
                commitColor()
            },
            onValueChangeFinished = { commitColor() },
            valueRange = 0f..360f,
            label = stringResource(R.string.palette_hue),
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
            )
        )

        // Saturation Slider
        CustomColorSlider(
            value = saturation,
            onValueChange = {
                saturation = it
                commitColor()
            },
            onValueChangeFinished = { commitColor() },
            valueRange = 0.1f..1f,
            label = stringResource(R.string.palette_saturation),
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(HSVToColor(floatArrayOf(hue, 0.1f, 1f))),
                    Color(HSVToColor(floatArrayOf(hue, 1f, 1f)))
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    label: String,
    brush: Brush
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = ThemeManager.textColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(brush)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(17.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
