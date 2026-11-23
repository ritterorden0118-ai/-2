package jp.example.yaberu

import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    /** Start time. Kept as hour/minute so V1 saved tasks migrate without data loss. */
    val hour: Int,
    val minute: Int,
    val repeat: String = "単発",
    val date: String = LocalDate.now().toString(),
    val weekdays: Set<Int> = emptySet(),
    val completedDates: Set<String> = emptySet(),
    val enabled: Boolean = true,
    val endHour: Int? = null,
    val endMinute: Int? = null
) {
    fun isDueOn(day: LocalDate): Boolean = when (repeat) {
        "毎日" -> true
        "毎週" -> weekdays.contains(day.dayOfWeek.value)
        "毎月" -> day.dayOfMonth == runCatching { LocalDate.parse(date).dayOfMonth }.getOrDefault(day.dayOfMonth)
        else -> date == day.toString()
    }

    fun nextDate(from: LocalDate = LocalDate.now()): LocalDate? = nextDateFor(LocalTime.of(hour, minute), from)

    fun nextDateFor(time: LocalTime, from: LocalDate = LocalDate.now()): LocalDate? {
        if (!enabled) return null
        return (0..370).asSequence().map { from.plusDays(it.toLong()) }
            .firstOrNull { day -> isDueOn(day) && day.toString() !in completedDates &&
                (day != from || time.isAfter(LocalTime.now())) }
    }

    fun toJson() = JSONObject().apply {
        put("id", id); put("title", title); put("hour", hour); put("minute", minute)
        put("repeat", repeat); put("date", date); put("enabled", enabled)
        put("weekdays", weekdays.joinToString(",")); put("completed", completedDates.joinToString(","))
        if (endHour != null && endMinute != null) { put("endHour", endHour); put("endMinute", endMinute) }
    }

    companion object {
        fun fromJson(o: JSONObject) = TodoTask(
            id=o.optString("id", UUID.randomUUID().toString()), title=o.optString("title"),
            hour=o.optInt("hour"), minute=o.optInt("minute"), repeat=o.optString("repeat", "単発"),
            date=o.optString("date", LocalDate.now().toString()),
            weekdays=o.optString("weekdays").split(",").mapNotNull { it.toIntOrNull() }.toSet(),
            completedDates=o.optString("completed").split(",").filter { it.isNotBlank() }.toSet(),
            enabled=o.optBoolean("enabled", true),
            endHour=if (o.has("endHour") && !o.isNull("endHour")) o.optInt("endHour") else null,
            endMinute=if (o.has("endMinute") && !o.isNull("endMinute")) o.optInt("endMinute") else null
        )
    }
}
