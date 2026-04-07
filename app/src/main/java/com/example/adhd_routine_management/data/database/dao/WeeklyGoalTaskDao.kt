package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.WeeklyGoalTask
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyGoalTaskDao {

    /** 特定のゴールIDに紐づくタスクを表示順で Flow 監視する */
    @Query("SELECT * FROM weekly_goal_tasks WHERE goalId = :goalId ORDER BY taskOrder")
    fun getTasksForGoal(goalId: Int): Flow<List<WeeklyGoalTask>>

    /** 特定のゴールIDに紐づくタスクを一度だけ取得する（suspend 版） */
    @Query("SELECT * FROM weekly_goal_tasks WHERE goalId = :goalId ORDER BY taskOrder")
    suspend fun getTasksForGoalOnce(goalId: Int): List<WeeklyGoalTask>

    /** タスクを一括登録する */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<WeeklyGoalTask>)

    /** タスクの達成状態を更新する */
    @Update
    suspend fun updateTask(task: WeeklyGoalTask)

    /** バックアップ用：全タスクを一度だけ取得する */
    @Query("SELECT * FROM weekly_goal_tasks ORDER BY goalId, taskOrder")
    suspend fun getAllTasksOnce(): List<WeeklyGoalTask>

    /** リストア用：全件挿入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<WeeklyGoalTask>)
}
