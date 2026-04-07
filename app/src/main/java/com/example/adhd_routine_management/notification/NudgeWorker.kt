package com.example.adhd_routine_management.notification

import android.content.Context
import androidx.work.*
import com.example.adhd_routine_management.data.database.AppDatabase
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.domain.model.CharacterDialogue
import com.example.adhd_routine_management.domain.model.CharacterStage
import com.example.adhd_routine_management.domain.model.DialogueType
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * 未完了タスクがある場合に30分おきに催促通知を送る Worker。
 * WorkManager は OS がバックグラウンドで管理するジョブスケジューラ。
 * アプリが終了していても動作する。
 */
class NudgeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val repository = TaskRepository(
            taskDao         = db.routineTaskDao(),
            completionDao   = db.taskCompletionDao(),
            dailyRecordDao  = db.dailyRecordDao(),
            progressDao     = db.userProgressDao(),
            healthRecordDao = db.healthRecordDao()
        )

        val today = LocalDate.now().toString()
        val pendingTaskIds = repository.getPendingTaskIdsForDate(today)

        // 予定時刻をすでに過ぎているPENDINGタスクのみカウントする。
        // ※時刻前のタスクまで催促してしまうバグを防ぐため。
        val nowHour   = java.time.LocalTime.now().hour
        val nowMinute = java.time.LocalTime.now().minute
        val overduePendingCount = if (pendingTaskIds.isEmpty()) 0 else {
            val activeTasks = repository.getActiveTasksOnce()
            activeTasks.count { task ->
                task.id in pendingTaskIds &&
                (task.hourOfDay < nowHour ||
                    (task.hourOfDay == nowHour && task.minute <= nowMinute))
            }
        }

        // 予定時刻を過ぎた未完了タスクがあれば催促通知
        if (overduePendingCount > 0) {
            val progress = repository.getProgressOnce()
            val stage = CharacterStage.fromPoints(progress.totalPoints)
            val message = CharacterDialogue.get(stage, DialogueType.NEGLECTED)
            NotificationHelper.showNudgeNotification(applicationContext, message)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "nudge_worker"

        /**
         * 30分おきの催促 Worker をスケジュール。
         * KEEP = 既に登録済みなら上書きしない（重複防止）
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NudgeWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
