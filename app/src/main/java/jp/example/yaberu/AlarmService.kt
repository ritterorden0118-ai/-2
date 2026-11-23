package jp.example.yaberu

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.time.LocalDate

class AlarmService : Service() {
    private var ringtone: Ringtone? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var retryTimeout: Runnable? = null
    private var taskId: String = ""
    private var taskDate: String = LocalDate.now().toString()
    private var alarmType: String = "start"
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        taskId = intent?.getStringExtra("taskId") ?: ""
        taskDate = intent?.getStringExtra("date") ?: LocalDate.now().toString()
        alarmType = intent?.getStringExtra("alarmType") ?: "start"
        val title = intent?.getStringExtra("title") ?: "予定の時間です"
        createChannel()
        startForeground(1001, buildNotification(title))
        if (ringtone == null || ringtone?.isPlaying != true) {
            val uri = AlarmSoundStore.chooseUri(this)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            if (Build.VERSION.SDK_INT >= 28) ringtone?.isLooping = true
            ringtone?.play()
        }
        retryTimeout?.let { handler.removeCallbacks(it) }
        retryTimeout = Runnable {
            stopSoundOnly()
            scheduleRetry()
        }.also { handler.postDelayed(it, 25_000) }
        return START_NOT_STICKY
    }
    private fun buildNotification(title: String): Notification {
        fun action(label: String, code: String): NotificationCompat.Action {
            val i=Intent(this, AlarmActionReceiver::class.java).putExtra("action",code).putExtra("taskId",taskId).putExtra("date",taskDate).putExtra("alarmType",alarmType)
            val p=PendingIntent.getBroadcast(this, code.hashCode() + alarmType.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            return NotificationCompat.Action.Builder(0,label,p).build()
        }
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val isStart = alarmType == "start"
        val primary = if (isStart) action("始める","begin") else action("今日はここまで","done")
        return NotificationCompat.Builder(this,"yaberu_alarm").setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(if (isStart) "やるベル｜始める時間" else "やるベル｜終了時間").setContentText(title).setContentIntent(open).setFullScreenIntent(open,true).setCategory(NotificationCompat.CATEGORY_ALARM).setPriority(NotificationCompat.PRIORITY_MAX).setOngoing(true).addAction(primary).addAction(action("5分後","snooze")).addAction(action("停止","stop")).build()
    }
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val c=NotificationChannel("yaberu_alarm","やるベル アラーム",NotificationManager.IMPORTANCE_HIGH).apply { description="タスク時刻のアラーム"; setSound(null,null); lockscreenVisibility=Notification.VISIBILITY_PUBLIC }
            getSystemService(NotificationManager::class.java).createNotificationChannel(c)
        }
    }
    private fun scheduleRetry() {
        val task = TaskStore.load(this).firstOrNull { it.id == taskId } ?: return
        if (taskDate in task.completedDates) return
        if (AlarmPauseStore.isPaused(this)) return
        val i=Intent(this,AlarmReceiver::class.java).putExtra("taskId",taskId).putExtra("title",task.title).putExtra("date",taskDate).putExtra("alarmType",alarmType)
        val p=PendingIntent.getBroadcast(this,taskId.hashCode()+taskDate.hashCode()+alarmType.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+5*60*1000,p)
    }
    private fun stopSoundOnly() { ringtone?.stop(); ringtone=null }
    override fun onDestroy() {
        retryTimeout?.let { handler.removeCallbacks(it) }
        retryTimeout = null
        stopSoundOnly()
        super.onDestroy()
    }
}

class AlarmActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("taskId") ?: return
        val date = intent.getStringExtra("date") ?: LocalDate.now().toString()
        val alarmType = intent.getStringExtra("alarmType") ?: "start"
        val action = intent.getStringExtra("action") ?: "stop"
        val tasks = TaskStore.load(context)
        val index = tasks.indexOfFirst { it.id == id }
        if (index < 0) return
        val task = tasks[index]
        context.stopService(Intent(context, AlarmService::class.java))
        when (action) {
            "done" -> {
                tasks[index] = task.copy(completedDates = task.completedDates + date)
                TaskStore.save(context, tasks)
                AlarmScheduler.rescheduleAll(context)
            }
            "snooze" -> {
                if (AlarmPauseStore.isPaused(context)) return
                val i = Intent(context, AlarmReceiver::class.java).putExtra("taskId", id)
                    .putExtra("title", task.title).putExtra("date", date).putExtra("alarmType", alarmType)
                val p = PendingIntent.getBroadcast(context, id.hashCode() + date.hashCode() + alarmType.hashCode(), i,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 5 * 60 * 1000, p)
            }
            // "begin" and "stop" silence this occurrence without marking the task complete.
        }
    }
}
