package jp.example.yaberu

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/** Stores the temporary alarm pause independently from task data. */
object AlarmPauseStore {
    private const val PREF = "yaberu_alarm_pause"
    private const val KEY_UNTIL = "paused_until"
    private const val MANUAL = -1L
    private const val REQUEST_CODE = 73021

    fun isPaused(context: Context): Boolean {
        val until = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_UNTIL, 0L)
        return until == MANUAL || until > System.currentTimeMillis()
    }

    fun label(context: Context): String {
        val until = context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(KEY_UNTIL, 0L)
        return when {
            until == MANUAL -> "アラーム停止中（手動で再開）"
            until > System.currentTimeMillis() -> {
                val minutes = ((until - System.currentTimeMillis() + 59_999) / 60_000).toInt()
                "アラーム停止中（あと約${minutes}分）"
            }
            else -> "アラームはオン"
        }
    }

    fun pause(context: Context, durationMillis: Long?) {
        val until = durationMillis?.let { System.currentTimeMillis() + it } ?: MANUAL
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(KEY_UNTIL, until).apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = expiryPendingIntent(context)
        alarmManager.cancel(pending)
        if (durationMillis != null) {
            if (Build.VERSION.SDK_INT < 31 || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, until, pending)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, until, pending)
            }
        }
        context.stopService(Intent(context, AlarmService::class.java))
        AlarmScheduler.rescheduleAll(context)
    }

    fun resume(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putLong(KEY_UNTIL, 0L).apply()
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(expiryPendingIntent(context))
        AlarmScheduler.rescheduleAll(context)
    }

    private fun expiryPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE, Intent(context, AlarmPauseExpiryReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

class AlarmPauseExpiryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        AlarmPauseStore.resume(context)
    }
}
