package com.mountaincrab.logrhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(c.soft)
            .padding(start = 4.dp, end = 9.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(c.bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "$rating", color = c.fg, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(text = "blood", color = c.bg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
