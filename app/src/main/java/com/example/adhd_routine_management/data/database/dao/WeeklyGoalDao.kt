package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.WeeklyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyGoalDao {

    /** 指定した週（月曜日の日付）の目標を Flow で監視する */
    @Query("SELECT * FROM weekly_goals WHERE weekStart = :weekStart LIMIT 1")
    fun getGoalForWeek(weekStart: String): Flow<WeeklyGoal?>

    /** 指定した週（月曜日の日付）の目標を一度だけ取得する（suspend 版） */
    @Query("SELECT * FROM weekly_goals WHERE weekStart = :weekStart LIMIT 1")
    suspend fun getGoalForWeekOnce(weekStart: String): WeeklyGoal?

    /** すべての目標を新しい順で取得する */
    @Query("SELECT * FROM weekly_goals ORDER BY weekStart DESC")
    fun getAllGoals(): Flow<List<WeeklyGoal>>

    /** 目標を登録する。戻り値は自動生成されたID */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: WeeklyGoal): Long

    /** リストア用：全件挿入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<WeeklyGoal>)
}
