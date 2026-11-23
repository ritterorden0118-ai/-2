package jp.example.yaberu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

object AlarmScheduler {
    private fun requestCode(task: TodoTask, type: String): Int = 31 * task.id.hashCode() + type.hashCode()

    fun schedule(context: Context, task: TodoTask, @Suppress("UNUSED_PARAMETER") date: LocalDate) {
        if (AlarmPauseStore.isPaused(context) || !task.enabled) return
        task.nextDate()?.let { scheduleOne(context, task, it, "start", LocalTime.of(task.hour, task.minute)) }
        if (task.endHour != null && task.endMinute != null) {
            val endTime = LocalTime.of(task.endHour, task.endMinute)
            task.nextDateFor(endTime)?.let { scheduleOne(context, task, it, "end", endTime) }
        }
    }

    private fun scheduleOne(context: Context, task: TodoTask, date: LocalDate, type: String, time: LocalTime) {
        val at = date.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (at <= System.currentTimeMillis()) return
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra("taskId", task.id).putExtra("title", task.title)
            .putExtra("date", date.toString()).putExtra("alarmType", type)
        val pi = PendingIntent.getBroadcast(context, requestCode(task, type), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= 31 && !am.canScheduleExactAlarms()) return
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun cancel(context: Context, task: TodoTask) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf("start", "end").forEach { type ->
            val pi = PendingIntent.getBroadcast(context, requestCode(task, type),
                Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            am.cancel(pi)
        }
    }

    fun rescheduleAll(context: Context) {
        val tasks = TaskStore.load(context)
        tasks.forEach { cancel(context, it) }
        if (AlarmPauseStore.isPaused(context)) return
        tasks.forEach { task ->
            task.nextDate()?.let { scheduleOne(context, task, it, "start", LocalTime.of(task.hour, task.minute)) }
            if (task.endHour != null && task.endMinute != null) {
                val endTime = LocalTime.of(task.endHour, task.endMinute)
                task.nextDateFor(endTime)?.let { scheduleOne(context, task, it, "end", endTime) }
            }
        }
    }
}
