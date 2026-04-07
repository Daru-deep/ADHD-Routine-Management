package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1週間の短期目標を表すエンティティ。
 * 毎週月曜日に設定し、その週の頑張りを記録する。
 *
 * @param id        自動採番されるID
 * @param weekStart その週の月曜日の日付（YYYY-MM-DD形式）。主キーの代わりに検索キーとして使う
 * @param title     目標のタイトル（例：「今週は毎朝ストレッチする」）
 * @param createdAt 登録日時（ミリ秒）
 */
@Entity(tableName = "weekly_goals")
data class WeeklyGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weekStart: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis()
)
