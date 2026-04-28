package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * ユーザーの累積進捗（1レコードのみ使用、id=1固定）
 *
 * @param totalPoints         累積ポイント（キャラクターの成長に使う）
 * @param currentStreak       現在の連続達成日数
 * @param maxStreak           過去最高連続日数
 * @param lastActiveDate      最後にタスクを完了 or あきらめた日（連続判定に使う）
 * @param punctualStreak      現在の無遅刻連続記録（用事無の日はカウントされないが継続する）
 * @param maxPunctualStreak   過去最高の無遅刻連続記録
 */
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val id: Int = 1,
    val totalPoints: Int = 0,
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val lastActiveDate: String = "",
    val punctualStreak: Int = 0,
    val maxPunctualStreak: Int = 0
)
