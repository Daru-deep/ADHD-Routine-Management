package com.example.adhd_routine_management.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 週間短期目標に紐づく個別タスク。
 * 1つの WeeklyGoal に対して最大5つのタスクが存在する。
 *
 * @param id          自動採番されるID
 * @param goalId      紐づく WeeklyGoal の ID（外部キー相当）
 * @param name        タスク名
 * @param isCompleted この週にタスクを達成したかどうか
 * @param taskOrder   表示順（1〜5）
 */
@Entity(tableName = "weekly_goal_tasks")
data class WeeklyGoalTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val goalId: Int,
    val name: String,
    val isCompleted: Boolean = false,
    val taskOrder: Int
)
