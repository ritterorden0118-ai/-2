package jp.example.yaberu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (AlarmPauseStore.isPaused(context)) return
        val taskId = intent.getStringExtra("taskId") ?: return
        val date = intent.getStringExtra("date") ?: return
        val task = TaskStore.load(context).firstOrNull { it.id == taskId } ?: return
        if (!task.enabled || date in task.completedDates) return
        val service = Intent(context, AlarmService::class.java).putExtras(intent)
        ContextCompat.startForegroundService(context, service)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { AlarmScheduler.rescheduleAll(context) }
}
