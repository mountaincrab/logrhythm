package com.mountaincrab.logrhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.logrhythm.ui.theme.RatingColors

@Composable
fun RatingPill(rating: Int) {
    val c = RatingColors[rating.coerceIn(1, 5)] ?: RatingColors.getValue(1)
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(c.bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "$rating", color = c.fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
    }
}
