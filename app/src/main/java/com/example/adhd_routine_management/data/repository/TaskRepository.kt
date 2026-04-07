package com.example.adhd_routine_management.data.repository

import com.example.adhd_routine_management.data.database.dao.DailyRecordDao
import com.example.adhd_routine_management.data.database.dao.HealthRecordDao
import com.example.adhd_routine_management.data.database.dao.RoutineTaskDao
import com.example.adhd_routine_management.data.database.dao.TaskCompletionDao
import com.example.adhd_routine_management.data.database.dao.UserProgressDao
import com.example.adhd_routine_management.data.database.entity.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository はデータ取得の窓口。
 * ViewModel はこのクラスを通じてDBを操作する。
 * ViewModel が直接 DAO を触らない設計にすることで、将来DB以外のソースに切り替えやすくなる。
 */
class TaskRepository(
    private val taskDao: RoutineTaskDao,
    private val completionDao: TaskCompletionDao,
    private val dailyRecordDao: DailyRecordDao,
    private val progressDao: UserProgressDao,
    private val healthRecordDao: HealthRecordDao
) {
    // --- タスク ---
    fun getAllTasks(): Flow<List<RoutineTask>> = taskDao.getAllTasks()
    fun getActiveTasks(): Flow<List<RoutineTask>> = taskDao.getActiveTasks()
    suspend fun insertTask(task: RoutineTask) = taskDao.insertTask(task)
    suspend fun updateTask(task: RoutineTask) = taskDao.updateTask(task)
    suspend fun deleteTask(task: RoutineTask) = taskDao.deleteTask(task)
    suspend fun getActiveTasksOnce(): List<RoutineTask> = taskDao.getActiveTasksOnce()
    suspend fun getAllTasksOnce(): List<RoutineTask> = taskDao.getAllTasksOnce()
    suspend fun insertAllTasks(tasks: List<RoutineTask>) = taskDao.insertAll(tasks)

    // --- 完了記録 ---
    fun getCompletionsForDate(date: String): Flow<List<TaskCompletion>> =
        completionDao.getCompletionsForDate(date)
    suspend fun getAllCompletionsOnce(): List<TaskCompletion> = completionDao.getAllCompletionsOnce()
    suspend fun insertAllCompletions(completions: List<TaskCompletion>) = completionDao.insertAll(completions)

    suspend fun markCompleted(taskId: Int, date: String) {
        val existing = completionDao.getCompletion(taskId, date)
        if (existing != null) {
            completionDao.updateStatus(taskId, date, CompletionStatus.COMPLETED, System.currentTimeMillis())
        } else {
            completionDao.insertCompletion(
                TaskCompletion(
                    taskId = taskId,
                    date = date,
                    status = CompletionStatus.COMPLETED,
                    completedAt = System.currentTimeMillis()
                )
            )
        }
    }

    /** 今日のタスクが未作成なら PENDING レコードを生成する */
    suspend fun ensureTodayRecords(date: String) {
        val existing = completionDao.getCompletionsForDateOnce(date)
        val activeTasks = taskDao.getActiveTasksOnce()
        val existingTaskIds = existing.map { it.taskId }.toSet()

        activeTasks.forEach { task ->
            if (task.id !in existingTaskIds) {
                completionDao.insertCompletion(
                    TaskCompletion(taskId = task.id, date = date)
                )
            }
        }
    }

    /** 今日を「あきらめる」：全 PENDING タスクを SKIPPED にする */
    suspend fun giveUpToday(date: String) {
        val completions = completionDao.getCompletionsForDateOnce(date)
        completions.filter { it.status == CompletionStatus.PENDING }.forEach { c ->
            completionDao.updateStatus(c.taskId, date, CompletionStatus.SKIPPED, null)
        }
    }

    /** 特定のタスクだけ「今日はあきらめる」：そのタスクのみ SKIPPED にする */
    suspend fun giveUpTask(taskId: Int, date: String) {
        val completion = completionDao.getCompletion(taskId, date)
        if (completion != null && completion.status == CompletionStatus.PENDING) {
            completionDao.updateStatus(taskId, date, CompletionStatus.SKIPPED, null)
        }
    }

    /** 当日の未完了（PENDING）数を返す */
    suspend fun getPendingCount(date: String): Int =
        completionDao.getPendingCountForDate(date)

    /** 当日の未完了（PENDING）タスクIDの一覧を返す */
    suspend fun getPendingTaskIdsForDate(date: String): Set<Int> =
        completionDao.getCompletionsForDateOnce(date)
            .filter { it.status == com.example.adhd_routine_management.data.database.entity.CompletionStatus.PENDING }
            .map { it.taskId }
            .toSet()

    // --- 日次記録 ---
    fun getRecordForDate(date: String): Flow<DailyRecord?> =
        dailyRecordDao.getRecordForDate(date)

    suspend fun saveDailyRecord(record: DailyRecord) =
        dailyRecordDao.insertOrUpdate(record)

    fun getRecentRecords(limit: Int = 30): Flow<List<DailyRecord>> =
        dailyRecordDao.getRecentRecords(limit)
    suspend fun getAllDailyRecordsOnce(): List<DailyRecord> = dailyRecordDao.getAllRecordsOnce()
    suspend fun insertAllDailyRecords(records: List<DailyRecord>) = dailyRecordDao.insertAll(records)

    // --- 体調記録 ---
    fun getHealthRecordsForDate(date: String): Flow<List<HealthRecord>> =
        healthRecordDao.getRecordsForDate(date)

    suspend fun saveHealthRecord(record: HealthRecord) =
        healthRecordDao.insertOrUpdate(record)

    suspend fun getHealthRecordsForRange(from: String, to: String): List<HealthRecord> =
        healthRecordDao.getRecordsForRange(from, to)

    suspend fun getAllHealthRecordsOnce(): List<HealthRecord> =
        healthRecordDao.getAllRecordsOnce()

    suspend fun insertAllHealthRecords(records: List<HealthRecord>) =
        healthRecordDao.insertAll(records)

    // --- ユーザー進捗 ---
    fun getProgress(): Flow<UserProgress?> = progressDao.getProgress()

    suspend fun getProgressOnce(): UserProgress =
        progressDao.getProgressOnce() ?: UserProgress()

    suspend fun saveProgress(progress: UserProgress) =
        progressDao.insertOrUpdate(progress)

    /**
     * タスク完了後にポイント・streak を更新する。
     * ロジック：
     *   - completedCount / totalCount で今日の状況を確認
     *   - 全完了なら完了ボーナスを付与
     *   - 日付が前日と連続しているなら streak を継続、そうでなければリセット
     */
    suspend fun updateProgressAfterCompletion(
        date: String,
        completedCount: Int,
        totalCount: Int,
        earnedPoints: Int
    ) {
        val current = getProgressOnce()
        val today = LocalDate.parse(date)
        val lastDate = if (current.lastActiveDate.isNotEmpty())
            LocalDate.parse(current.lastActiveDate) else null

        val newStreak = when {
            lastDate == null -> 1
            lastDate == today -> current.currentStreak          // 同日の複数完了
            lastDate == today.minusDays(1) -> current.currentStreak + 1  // 昨日から連続
            else -> 1                                           // 途切れた
        }

        val newTotal = current.totalPoints + earnedPoints
        val newMax = maxOf(current.maxStreak, newStreak)

        saveProgress(
            current.copy(
                totalPoints = newTotal,
                currentStreak = newStreak,
                maxStreak = newMax,
                lastActiveDate = date
            )
        )

        // 日次記録も更新
        val existing = dailyRecordDao.getRecordForDateOnce(date)
        dailyRecordDao.insertOrUpdate(
            DailyRecord(
                date = date,
                totalTasks = totalCount,
                completedTasks = completedCount,
                pointsEarned = (existing?.pointsEarned ?: 0) + earnedPoints,
                isGivenUp = existing?.isGivenUp ?: false
            )
        )
    }

    /** 「あきらめる」時の streak 処理：streak は維持、ポイントは 0 */
    suspend fun updateProgressForGiveUp(date: String) {
        val current = getProgressOnce()
        val today = LocalDate.parse(date)
        val lastDate = if (current.lastActiveDate.isNotEmpty())
            LocalDate.parse(current.lastActiveDate) else null

        val newStreak = when {
            lastDate == null -> 0
            lastDate == today -> current.currentStreak
            lastDate == today.minusDays(1) -> current.currentStreak // あきらめでも streak 維持
            else -> 0
        }

        saveProgress(
            current.copy(
                currentStreak = newStreak,
                maxStreak = maxOf(current.maxStreak, newStreak),
                lastActiveDate = date
            )
        )

        val existing = dailyRecordDao.getRecordForDateOnce(date)
        val total = taskDao.getActiveTasksOnce().size
        dailyRecordDao.insertOrUpdate(
            DailyRecord(
                date = date,
                totalTasks = total,
                completedTasks = existing?.completedTasks ?: 0,
                pointsEarned = existing?.pointsEarned ?: 0,
                isGivenUp = true
            )
        )
    }
}
