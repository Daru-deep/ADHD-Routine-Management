package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 毎日繰り返すルーティンタスク（例：「7時に起きる」「朝ご飯を食べる」）
 *
 * @param id                   自動採番されるID
 * @param name                 タスク名
 * @param hourOfDay            通知する時刻（時）0〜23
 * @param minute               通知する時刻（分）0〜59
 * @param isActive             有効/無効フラグ（削除せず無効化できる）
 * @param snoozeDurationMinutes スヌーズ時間（分）。通知を後回しにする時間。デフォルト10分
 */
@Entity(tableName = "routine_tasks")
data class RoutineTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val hourOfDay: Int,
    val minute: Int,
    val isActive: Boolean = true,
    val snoozeDurationMinutes: Int = 10
)
