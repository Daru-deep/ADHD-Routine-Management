package com.example.adhd_routine_management.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmManager から呼ばれる BroadcastReceiver。
 * 指定した時刻に OS が起動してこのクラスの onReceive() を呼ぶ。
 */
class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId       = intent.getIntExtra(EXTRA_TASK_ID, -1)
        val taskName     = intent.getStringExtra(EXTRA_TASK_NAME) ?: return
        val snoozeMins   = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)
        if (taskId == -1) return

        NotificationHelper.showTaskNotification(context, taskId, taskName, snoozeMins)
    }

    companion object {
        const val EXTRA_TASK_ID      = "extra_task_id"
        const val EXTRA_TASK_NAME    = "extra_task_name"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    }
}
