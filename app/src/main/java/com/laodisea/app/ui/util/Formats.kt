package com.laodisea.app.ui.util

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formats {
    private val spanish = Locale.forLanguageTag("es-MX")
    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", spanish)
    private val dateFormat = DateTimeFormatter.ofPattern("d 'de' MMMM", spanish)

    /** 3600 s → «1 h 0 min»; menos de una hora → «25 min». */
    fun playtime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return if (hours > 0) "$hours h ${minutes} min" else "$minutes min"
    }

    /** «hoy a las 21:40», «ayer a las 09:15» o «12 de octubre a las 18:00». */
    fun relativeDateTime(epochMillis: Long): String {
        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        val today = LocalDate.now()
        val day = when (dateTime.toLocalDate()) {
            today -> "hoy"
            today.minusDays(1) -> "ayer"
            else -> "el " + dateTime.format(dateFormat)
        }
        return "$day a las ${dateTime.format(timeFormat)}"
    }

    /** Tiempo hasta [isoInstant]: «en 2 h 10 min», «en 5 min» o «en un momento». */
    fun untilReset(isoInstant: String?): String {
        val target = parse(isoInstant) ?: return "pronto"
        val remaining = Duration.between(Instant.now(), target.toInstant())
        if (remaining.isNegative || remaining.toMinutes() < 1) return "en un momento"
        val hours = remaining.toHours()
        val minutes = remaining.toMinutes() % 60
        return if (hours > 0) "en $hours h $minutes min" else "en $minutes min"
    }

    fun date(isoInstant: String): String =
        parse(isoInstant)?.atZoneSameInstant(ZoneId.systemDefault())?.format(dateFormat) ?: isoInstant

    private fun parse(iso: String?): OffsetDateTime? =
        iso?.let { runCatching { OffsetDateTime.parse(it) }.getOrNull() }
}
