package com.example.adhd_routine_management.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 通知の「スヌーズ」ボタンが押されたときに呼ばれるレシーバー。
 * 指定した分数後に TaskAlarmReceiver を呼び出して通知を再表示する。
 */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId     = intent.getIntExtra(TaskAlarmReceiver.EXTRA_TASK_ID, -1)
        val taskName   = intent.getStringExtra(TaskAlarmReceiver.EXTRA_TASK_NAME) ?: return
        val snoozeMins = intent.getIntExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, 10)
        if (taskId == -1) return

        // スヌーズ後に TaskAlarmReceiver を起動するための Intent
        val alarmIntent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeMins)
        }
        // リクエストコードを taskId+10000 にして、毎日のアラームと衝突しないようにする
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId + 10000,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = System.currentTimeMillis() + snoozeMins * 60 * 1000L
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // setExactAndAllowWhileIdle: 省電力モード中でも正確な時刻に発火する
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
