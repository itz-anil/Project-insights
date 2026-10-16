package com.netpulse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netpulse.app.ui.components.Chip
import com.netpulse.app.ui.components.GlassCard
import com.netpulse.app.ui.theme.Palette

/** Placeholder for features planned in the next phase. */
@Composable
fun ComingSoonScreen(title: String, points: List<String>) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard(Modifier.widthIn(max = 560.dp), verticalSpacing = 14.dp) {
            Chip("Next phase", Palette.Download)
            Text(title, style = MaterialTheme.typography.headlineLarge)
            points.forEach { point ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(6.dp).background(Palette.TextSecondary, CircleShape))
                    Text(point, style = MaterialTheme.typography.bodyLarge, color = Palette.TextSecondary, modifier = Modifier.padding(end = 4.dp))
                }
            }
        }
    }
}
