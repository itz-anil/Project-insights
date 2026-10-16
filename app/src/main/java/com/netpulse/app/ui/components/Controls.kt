package com.netpulse.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netpulse.app.ui.theme.Mono
import com.netpulse.app.ui.theme.Palette

/** White pill button (primary) or glass pill button (secondary). */
@Composable
fun PillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .then(
                if (primary) Modifier.background(Palette.Selected)
                else Modifier.background(Palette.Fill).border(1.dp, Palette.FillStrong, CircleShape)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 26.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = if (primary) Palette.OnSelected else Palette.TextPrimary,
        )
    }
}

/** Label on the left, − value + on the right. */
@Composable
fun Stepper(
    label: String,
    value: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Palette.TextSecondary, modifier = Modifier.weight(1f))
        RoundIconButton("−", "Decrease $label", onMinus)
        Text(
            value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = Mono),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 84.dp),
        )
        RoundIconButton("+", "Increase $label", onPlus)
    }
}

@Composable
private fun RoundIconButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Palette.Fill)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleLarge, color = Palette.TextPrimary)
    }
}
