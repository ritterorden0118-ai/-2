package jp.example.yaberu

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class AlarmActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val taskTitle = intent.getStringExtra("title") ?: "予定の時間です"
        val taskId = intent.getStringExtra("taskId") ?: ""
        val date = intent.getStringExtra("date") ?: ""
        val alarmType = intent.getStringExtra("alarmType") ?: "start"

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 48, 32, 48)
            setBackgroundColor(android.graphics.Color.rgb(247, 249, 252))
        }
        layout.addView(TextView(this).apply {
            text = if (alarmType == "start") "やるベル｜始める時間" else "やるベル｜終了時間"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        layout.addView(TextView(this).apply {
            text = taskTitle
            textSize = 28f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        })

        fun addAction(label: String, action: String) {
            layout.addView(Button(this).apply {
                text = label
                textSize = 18f
                setOnClickListener {
                    val i = Intent(this@AlarmActivity, AlarmActionReceiver::class.java)
                        .putExtra("action", action)
                        .putExtra("taskId", taskId)
                        .putExtra("date", date)
                        .putExtra("alarmType", alarmType)
                    sendBroadcast(i)
                    finish()
                }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 12, 0, 12) })
        }

        addAction(if (alarmType == "start") "始める" else "今日はここまで",
            if (alarmType == "start") "begin" else "done")
        addAction("5分後", "snooze")
        addAction("音の停止", "stop")
        setContentView(layout)
    }
}
