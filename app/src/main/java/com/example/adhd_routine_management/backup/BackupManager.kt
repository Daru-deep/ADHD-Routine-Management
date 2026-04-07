package com.example.adhd_routine_management.backup

import android.content.Context
import android.net.Uri
import com.example.adhd_routine_management.data.database.entity.*
import com.example.adhd_routine_management.data.repository.TaskRepository
import com.example.adhd_routine_management.data.repository.WeeklyGoalRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * データのエクスポート（バックアップ）とインポート（リストア）を担当するクラス。
 *
 * JSON フォーマットを使う理由：
 * - 人間が読めるテキスト形式なので、万一の時に手動確認できる
 * - 外部ライブラリ不要（Android 標準の org.json を使用）
 * - バージョンアップ時の互換性管理がしやすい
 *
 * 保存先に SAF（Storage Access Framework）を使う理由：
 * - Android 10以降は外部ストレージへの直接書き込みに制限がある
 * - SAF を使うとユーザーが Google Drive や Files など好きな場所を選べる
 * - 特別なパーミッション宣言が不要
 */
object BackupManager {

    private const val BACKUP_VERSION = 1

    // ========== エクスポート（バックアップ） ==========

    /**
     * 全データを JSON 文字列に変換して Uri（ユーザーが選んだファイル）に書き込む。
     * @return 成功なら true、失敗なら false
     */
    suspend fun exportToUri(
        context: Context,
        uri: Uri,
        taskRepository: TaskRepository,
        weeklyGoalRepository: WeeklyGoalRepository
    ): Boolean {
        return try {
            val json = buildJson(taskRepository, weeklyGoalRepository)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toString(2).toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 全テーブルのデータを JSONObject にまとめる */
    private suspend fun buildJson(
        taskRepository: TaskRepository,
        weeklyGoalRepository: WeeklyGoalRepository
    ): JSONObject {
        val root = JSONObject()
        root.put("backupVersion", BACKUP_VERSION)
        root.put("exportedAt", LocalDate.now().toString())

        // ルーティンタスク
        val tasks = taskRepository.getAllTasksOnce()
        root.put("tasks", JSONArray().apply {
            tasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("hourOfDay", t.hourOfDay)
                    put("minute", t.minute)
                    put("isActive", t.isActive)
                    put("snoozeDurationMinutes", t.snoozeDurationMinutes)
                })
            }
        })

        // タスク完了記録
        val completions = taskRepository.getAllCompletionsOnce()
        root.put("completions", JSONArray().apply {
            completions.forEach { c ->
                put(JSONObject().apply {
                    put("id", c.id)
                    put("taskId", c.taskId)
                    put("date", c.date)
                    put("status", c.status.name)
                    put("completedAt", c.completedAt ?: JSONObject.NULL)
                })
            }
        })

        // 日次記録
        val records = taskRepository.getAllDailyRecordsOnce()
        root.put("dailyRecords", JSONArray().apply {
            records.forEach { r ->
                put(JSONObject().apply {
                    put("date", r.date)
                    put("totalTasks", r.totalTasks)
                    put("completedTasks", r.completedTasks)
                    put("pointsEarned", r.pointsEarned)
                    put("isGivenUp", r.isGivenUp)
                })
            }
        })

        // ユーザー進捗
        val progress = taskRepository.getProgressOnce()
        root.put("userProgress", JSONObject().apply {
            put("totalPoints", progress.totalPoints)
            put("currentStreak", progress.currentStreak)
            put("maxStreak", progress.maxStreak)
            put("lastActiveDate", progress.lastActiveDate)
        })

        // 週間目標
        val goals = weeklyGoalRepository.getAllGoalsOnce()
        root.put("weeklyGoals", JSONArray().apply {
            goals.forEach { g ->
                put(JSONObject().apply {
                    put("id", g.id)
                    put("weekStart", g.weekStart)
                    put("title", g.title)
                    put("createdAt", g.createdAt)
                })
            }
        })

        // 週間目標タスク
        val goalTasks = weeklyGoalRepository.getAllGoalTasksOnce()
        root.put("weeklyGoalTasks", JSONArray().apply {
            goalTasks.forEach { t ->
                put(JSONObject().apply {
                    put("id", t.id)
                    put("goalId", t.goalId)
                    put("name", t.name)
                    put("isCompleted", t.isCompleted)
                    put("taskOrder", t.taskOrder)
                })
            }
        })

        return root
    }

    // ========== インポート（リストア） ==========

    /**
     * Uri から JSON を読み込んで DB を上書きする。
     * 既存データはすべて置き換えられる（REPLACE ストラテジー）。
     * @return 成功なら true、失敗（フォーマット不一致など）なら false
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        taskRepository: TaskRepository,
        weeklyGoalRepository: WeeklyGoalRepository
    ): Boolean {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return false

            val root = JSONObject(json)

            // バージョンチェック（将来の互換性のため）
            val version = root.optInt("backupVersion", 0)
            if (version != BACKUP_VERSION) return false

            restoreFromJson(root, taskRepository, weeklyGoalRepository)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun restoreFromJson(
        root: JSONObject,
        taskRepository: TaskRepository,
        weeklyGoalRepository: WeeklyGoalRepository
    ) {
        // ルーティンタスク
        val tasksJson = root.getJSONArray("tasks")
        val tasks = (0 until tasksJson.length()).map { i ->
            val o = tasksJson.getJSONObject(i)
            RoutineTask(
                id                   = o.getInt("id"),
                name                 = o.getString("name"),
                hourOfDay            = o.getInt("hourOfDay"),
                minute               = o.getInt("minute"),
                isActive             = o.getBoolean("isActive"),
                snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10)
            )
        }
        taskRepository.insertAllTasks(tasks)

        // タスク完了記録
        val completionsJson = root.getJSONArray("completions")
        val completions = (0 until completionsJson.length()).map { i ->
            val o = completionsJson.getJSONObject(i)
            TaskCompletion(
                id          = o.getInt("id"),
                taskId      = o.getInt("taskId"),
                date        = o.getString("date"),
                status      = CompletionStatus.valueOf(o.getString("status")),
                completedAt = if (o.isNull("completedAt")) null else o.getLong("completedAt")
            )
        }
        taskRepository.insertAllCompletions(completions)

        // 日次記録
        val recordsJson = root.getJSONArray("dailyRecords")
        val records = (0 until recordsJson.length()).map { i ->
            val o = recordsJson.getJSONObject(i)
            DailyRecord(
                date           = o.getString("date"),
                totalTasks     = o.getInt("totalTasks"),
                completedTasks = o.getInt("completedTasks"),
                pointsEarned   = o.getInt("pointsEarned"),
                isGivenUp      = o.getBoolean("isGivenUp")
            )
        }
        taskRepository.insertAllDailyRecords(records)

        // ユーザー進捗
        val p = root.getJSONObject("userProgress")
        taskRepository.saveProgress(
            UserProgress(
                totalPoints    = p.getInt("totalPoints"),
                currentStreak  = p.getInt("currentStreak"),
                maxStreak      = p.getInt("maxStreak"),
                lastActiveDate = p.getString("lastActiveDate")
            )
        )

        // 週間目標
        val goalsJson = root.getJSONArray("weeklyGoals")
        val goals = (0 until goalsJson.length()).map { i ->
            val o = goalsJson.getJSONObject(i)
            WeeklyGoal(
                id        = o.getInt("id"),
                weekStart = o.getString("weekStart"),
                title     = o.getString("title"),
                createdAt = o.getLong("createdAt")
            )
        }
        weeklyGoalRepository.insertAllGoals(goals)

        // 週間目標タスク
        val goalTasksJson = root.getJSONArray("weeklyGoalTasks")
        val goalTasks = (0 until goalTasksJson.length()).map { i ->
            val o = goalTasksJson.getJSONObject(i)
            WeeklyGoalTask(
                id          = o.getInt("id"),
                goalId      = o.getInt("goalId"),
                name        = o.getString("name"),
                isCompleted = o.getBoolean("isCompleted"),
                taskOrder   = o.getInt("taskOrder")
            )
        }
        weeklyGoalRepository.insertAllGoalTasks(goalTasks)
    }
}
