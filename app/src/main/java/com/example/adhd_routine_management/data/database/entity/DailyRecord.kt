package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 1日ごとのまとめ記録
 *
 * @param date               日付文字列（主キー、例: "2026-03-25"）
 * @param totalTasks         その日のタスク総数
 * @param completedTasks     完了したタスク数
 * @param pointsEarned       その日に獲得したポイント
 * @param isGivenUp          「今日はあきらめる」フラグ
 * @param punctualityStatus  無遅刻記録："on_time"（無遅刻）/ "late"（遅刻）/ "no_appointment"（用事無）/ ""（未入力）
 */
@Entity(tableName = "daily_records")
data class DailyRecord(
    @PrimaryKey val date: String,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val pointsEarned: Int = 0,
    val isGivenUp: Boolean = false,
    val punctualityStatus: String = ""
)

/** 無遅刻ステータスの定数 */
object PunctualityStatus {
    const val ON_TIME        = "on_time"
    const val LATE           = "late"
    const val NO_APPOINTMENT = "no_appointment"
    const val NONE           = ""
}
