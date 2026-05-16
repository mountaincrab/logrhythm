package com.mountaincrab.logrhythm.ui.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val zone: ZoneId = ZoneId.systemDefault()
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dayFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val fullDayFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

fun Long.toLocalDateTime(): LocalDateTime =
    java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDateTime()

fun Long.toLocalDate(): LocalDate = toLocalDateTime().toLocalDate()

fun Long.formatTime(): String = toLocalDateTime().format(timeFormatter)

fun Long.formatDayLabel(today: LocalDate = LocalDate.now()): String {
    val d = toLocalDate()
    return when (d) {
        today -> "Today · ${d.format(dayFormatter)}"
        today.minusDays(1) -> "Yesterday · ${d.format(dayFormatter)}"
        else -> d.format(dayFormatter)
    }
}

fun Long.formatFullDayWithTime(): String =
    toLocalDateTime().let { "${it.toLocalDate().format(fullDayFormatter)} · ${it.format(timeFormatter)}" }

fun Long.formatFullDay(): String = toLocalDate().format(fullDayFormatter)

fun startOfDayMillis(date: LocalDate): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

fun endOfDayMillis(date: LocalDate): Long =
    date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

fun daysAgoMillis(days: Long): Long =
    LocalDateTime.now().minus(days, ChronoUnit.DAYS).atZone(zone).toInstant().toEpochMilli()
