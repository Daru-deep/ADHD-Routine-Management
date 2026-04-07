package com.example.adhd_routine_management.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.adhd_routine_management.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 端末再起動後にアラームを再登録するレシーバー。
 * Android は再起動すると AlarmManager の設定がリセットされるため、
 * BOOT_COMPLETED を受け取ってここで再スケジュールする。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val tasks = db.routineTaskDao().getActiveTasksOnce()
            tasks.forEach { task ->
                AlarmScheduler.schedule(context, task)
            }
            // 催促通知も再スケジュール
            NudgeWorker.schedule(context)
        }
    }
}
