package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 毎日の体調記録エンティティ。
 * 1日を朝・昼・夕の3スロットに分けて、それぞれ1〜10で記録する（5が基準）。
 * (date, timeSlot) の組み合わせがユニークになるよう管理する。
 */
@Entity(tableName = "health_records")
data class HealthRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,       // "2026-04-06" 形式
    val timeSlot: String,   // "morning" | "noon" | "evening"
    val score: Int          // 1〜10（5が基準）
)

/** 時間帯の定数と表示名をまとめたオブジェクト */
object TimeSlot {
    const val MORNING = "morning"
    const val NOON    = "noon"
    const val EVENING = "evening"

    val all = listOf(MORNING, NOON, EVENING)

    fun label(slot: String): String = when (slot) {
        MORNING -> "朝"
        NOON    -> "昼"
        EVENING -> "夕"
        else    -> slot
    }
}
