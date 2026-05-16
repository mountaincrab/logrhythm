package com.mountaincrab.logrhythm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mountaincrab.logrhythm.ui.theme.LocalAppPalette
import com.mountaincrab.logrhythm.ui.util.formatTime
import com.mountaincrab.logrhythm.ui.util.toLocalDate
import com.mountaincrab.logrhythm.ui.util.toLocalDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayLabelFormatter = DateTimeFormatter.ofPattern("EEE d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhenPicker(
    occurredAt: Long,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalAppPalette.current
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }

    val current = occurredAt.toLocalDateTime()
    val dayLabel = when (current.toLocalDate()) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> current.format(dayLabelFormatter)
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pill(
            text = dayLabel,
            leadingIcon = Icons.Outlined.CalendarToday,
            modifier = Modifier.weight(1f),
            onClick = { showDate = true },
        )
        Pill(
            text = occurredAt.formatTime(),
            leadingIcon = Icons.Outlined.Schedule,
            modifier = Modifier.weight(1f),
            onClick = { showTime = true },
        )
    }

    if (showDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = occurredAt,
        )
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val newDateMillis = state.selectedDateMillis ?: occurredAt
                    val newDate = java.time.Instant.ofEpochMilli(newDateMillis)
                        .atZone(ZoneId.of("UTC")).toLocalDate()
                    val time = current.toLocalTime()
                    onChange(toEpochMillis(newDate, time))
                    showDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTime) {
        val timeState = rememberTimePickerState(
            initialHour = current.hour,
            initialMinute = current.minute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    val time = LocalTime.of(timeState.hour, timeState.minute)
                    onChange(toEpochMillis(current.toLocalDate(), time))
                    showTime = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
            title = { Text("Pick a time") },
            text = { TimePicker(state = timeState) },
        )
    }
}

@Composable
private fun Pill(
    text: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalAppPalette.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surfaceRaised)
            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(leadingIcon, contentDescription = null, tint = palette.fgMuted)
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun toEpochMillis(date: LocalDate, time: LocalTime): Long =
    LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
