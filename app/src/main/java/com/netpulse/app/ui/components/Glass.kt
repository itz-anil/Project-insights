package com.netpulse.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.netpulse.app.ui.theme.Palette

/**
 * Soft coloured light behind the glass. Plain radial gradients instead of a live blur
 * keep it cheap on the Tab A9's Helio G99.
 */
@Composable
fun GlassBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = modifier
            .background(Palette.Background)
            .drawBehind {
                fun orb(color: Color, alpha: Float, cx: Float, cy: Float, r: Float) {
                    val center = Offset(size.width * cx, size.height * cy)
                    val radius = size.maxDimension * r
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                orb(Palette.OrbBlue, 0.40f, 0.05f, 0.0f, 0.55f)
                orb(Palette.OrbViolet, 0.28f, 0.95f, 0.35f, 0.50f)
                orb(Palette.OrbOrange, 0.18f, 0.45f, 1.05f, 0.50f)
            },
        content = content,
    )
}

private val glassFill = Brush.verticalGradient(listOf(Palette.GlassTop, Palette.GlassBottom))
private val glassEdge = Brush.verticalGradient(listOf(Palette.GlassEdgeTop, Palette.GlassEdgeBottom))

fun Modifier.glass(shape: Shape): Modifier = this
    .clip(shape)
    .background(glassFill)
    .border(1.dp, glassEdge, shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    corner: Dp = 26.dp,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    verticalSpacing: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .glass(RoundedCornerShape(corner))
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing),
        content = content,
    )
}

/** Floating pill tab bar with a white capsule on the selected tab. */
@Composable
fun <T> PillTabs(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .glass(CircleShape)
            .padding(5.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            val bg by animateColorAsState(if (isSelected) Palette.Selected else Color.Transparent, label = "tabBg")
            val fg by animateColorAsState(if (isSelected) Palette.OnSelected else Palette.TextPrimary, label = "tabFg")
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(item) })
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(item), style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** iOS-style segmented control. */
@Composable
fun <T> Segmented(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1AFFFFFF))
            .padding(3.dp)
            .selectableGroup(),
    ) {
        items.forEach { item ->
            val isSelected = item == selected
            val bg by animateColorAsState(if (isSelected) Palette.Selected else Color.Transparent, label = "segBg")
            val fg by animateColorAsState(if (isSelected) Palette.OnSelected else Palette.TextPrimary, label = "segFg")
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(item) })
                    .heightIn(min = 44.dp)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(item), style = MaterialTheme.typography.labelLarge, color = fg)
            }
        }
    }
}

/** iOS-style switch: green when on, white knob. */
@Composable
fun GlassSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track by animateColorAsState(if (checked) Palette.Good else Color(0x38FFFFFF), label = "track")
    val knobX by animateDpAsState(if (checked) 22.dp else 2.dp, label = "knob")
    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(track)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobX.roundToPx(), 2.dp.roundToPx()) }
                .size(27.dp)
                .shadow(3.dp, CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

@Composable
fun Chip(text: String, color: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.40f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (dot) Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}
