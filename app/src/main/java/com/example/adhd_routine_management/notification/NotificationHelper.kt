package com.example.adhd_routine_management.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.adhd_routine_management.MainActivity

object NotificationHelper {

    const val CHANNEL_TASK    = "task_reminder"
    const val CHANNEL_NUDGE   = "nudge_reminder"

    // タスク通知をまとめるグループキーと、サマリー通知のID
    private const val GROUP_TASKS            = "group_tasks"
    private const val NOTIF_ID_GROUP_SUMMARY = 10000

    /** 通知チャンネルを作成（Android 8.0以降で必要） */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        // タスク時刻通知チャンネル
        NotificationChannel(
            CHANNEL_TASK,
            "タスクのお知らせ",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "タスクの時刻になったときの通知"
            manager.createNotificationChannel(this)
        }

        // 催促通知チャンネル
        NotificationChannel(
            CHANNEL_NUDGE,
            "催促通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "未完了タスクがある場合の催促"
            manager.createNotificationChannel(this)
        }
    }

    /**
     * タスク時刻通知を表示する。
     * @param snoozeDurationMinutes スヌーズボタンを押したときに何分後に再通知するか
     */
    fun showTaskNotification(
        context: Context,
        taskId: Int,
        taskName: String,
        snoozeDurationMinutes: Int = 10
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, taskId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // スヌーズボタン用 Intent（SnoozeReceiver に飛ばす）
        val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
            putExtra(TaskAlarmReceiver.EXTRA_TASK_ID, taskId)
            putExtra(TaskAlarmReceiver.EXTRA_TASK_NAME, taskName)
            putExtra(TaskAlarmReceiver.EXTRA_SNOOZE_MINUTES, snoozeDurationMinutes)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context, taskId + 20000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("「${taskName}」の時間です！")
            .setContentText("タップして完了を記録しよう")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_TASKS)
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                "${snoozeDurationMinutes}分後にスヌーズ",
                snoozePendingIntent
            )
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(taskId, notification)
    }

    /**
     * タスク通知グループのサマリーを更新する。
     * Android 7.0以降では、同グループの通知が2件以上ある場合に
     * 通知ドロワーで折りたたんで1件に見せるサマリー通知が必要。
     * 1件以下のときはサマリーを消す。
     */
    fun updateTaskGroupSummary(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        // アクティブな（表示中の）タスクグループ通知の件数を数える
        val taskNotifCount = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            manager.activeNotifications.count { sbn ->
                sbn.notification.group == GROUP_TASKS &&
                (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
            }
        } else {
            // API 23未満はグループ未対応なのでサマリー不要
            0
        }

        if (taskNotifCount < 2) {
            // 1件以下ならサマリー不要なので消す
            manager.cancel(NOTIF_ID_GROUP_SUMMARY)
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summary = NotificationCompat.Builder(context, CHANNEL_TASK)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("${taskNotifCount}件のタスクが待っています")
            .setContentText("タップして確認しよう")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(GROUP_TASKS)
            .setGroupSummary(true)
            .build()

        manager.notify(NOTIF_ID_GROUP_SUMMARY, summary)
    }

    /** 催促通知を表示する（キャラのセリフ付き） */
    fun showNudgeNotification(context: Context, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 9999, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NUDGE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("まだタスクが残ってるよ…")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(9999, notification)
    }
}
