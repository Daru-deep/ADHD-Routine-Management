package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * タスクの完了状態を表すEnum
 * PENDING  : まだ判定されていない（今日のタスクが生成された直後）
 * COMPLETED: 完了した
 * SKIPPED  : 「あきらめる」ボタンで今日ごとスキップ
 * MISSED   : 通知を無視して完了しなかった
 */
enum class CompletionStatus { PENDING, COMPLETED, SKIPPED, MISSED }

/**
 * 各タスクの1日ごとの完了記録
 *
 * @param id         自動採番ID
 * @param taskId     どのルーティンタスクか（RoutineTask.idと紐づく）
 * @param date       日付文字列（例: "2026-03-25"）
 * @param status     完了状態
 * @param completedAt 完了した時刻（エポックミリ秒、未完了はnull）
 */
@Entity(tableName = "task_completions")
data class TaskCompletion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val date: String,
    val status: CompletionStatus = CompletionStatus.PENDING,
    val completedAt: Long? = null
)
