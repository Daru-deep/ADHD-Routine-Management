package com.example.adhd_routine_management.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import java.util.Calendar

/**
 * タスクの時刻通知をセットする AlarmManager ラッパー。
 *
 * AlarmManager は Android OS が管理するタイマーで、
 * アプリが終了していても指定時刻に処理を起動できる。
 * これが WorkManager より正確な時刻通知に向いている理由。
 */
object AlarmScheduler {

    /** タスクの毎日通知をセット（既存のものは上書き） */
    fun schedule(context: Context, task: RoutineTask) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_NAME, task.name)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, task.snoozeDurationMinutes)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 次回の指定時刻を計算
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, task.hourOfDay)
            set(Calendar.MINUTE, task.minute)
            set(Calendar.SECOND, 0)
            // 今日の時刻がすでに過ぎていたら翌日にセット
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // Android 12以降は正確なアラームに setExactAndAllowWhileIdle + 権限が必要
        // 毎日繰り返すために setRepeating を使う
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    /** タスクの通知をキャンセル */
    fun cancel(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }
}
