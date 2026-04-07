package com.example.adhd_routine_management.data.repository

import com.example.adhd_routine_management.data.database.dao.WeeklyGoalDao
import com.example.adhd_routine_management.data.database.dao.WeeklyGoalTaskDao
import com.example.adhd_routine_management.data.database.entity.WeeklyGoal
import com.example.adhd_routine_management.data.database.entity.WeeklyGoalTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** 週間短期目標の目標とタスクをまとめた表示用データクラス */
data class WeeklyGoalWithTasks(
    val goal: WeeklyGoal,
    val tasks: List<WeeklyGoalTask>
) {
    val completedCount: Int get() = tasks.count { it.isCompleted }
    val totalCount: Int get() = tasks.size
    /** 達成率（0.0〜1.0） */
    val achievementRate: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
}

class WeeklyGoalRepository(
    private val goalDao: WeeklyGoalDao,
    private val taskDao: WeeklyGoalTaskDao
) {
    /** 指定した週の目標を Flow で監視する */
    fun getGoalForWeek(weekStart: String): Flow<WeeklyGoal?> =
        goalDao.getGoalForWeek(weekStart)

    /** 指定した週の目標を一度だけ取得する */
    suspend fun getGoalForWeekOnce(weekStart: String): WeeklyGoal? =
        goalDao.getGoalForWeekOnce(weekStart)

    /** すべての目標を新しい順で Flow 監視する */
    fun getAllGoals(): Flow<List<WeeklyGoal>> = goalDao.getAllGoals()

    /** 特定のゴールIDに紐づくタスクを Flow 監視する */
    fun getTasksForGoal(goalId: Int): Flow<List<WeeklyGoalTask>> =
        taskDao.getTasksForGoal(goalId)

    /**
     * 新しい週間目標とタスク5つを登録する。
     * goalId は Room が自動採番する値を使う。
     */
    suspend fun createGoal(weekStart: String, title: String, taskNames: List<String>) {
        val goalId = goalDao.insertGoal(
            WeeklyGoal(weekStart = weekStart, title = title)
        ).toInt()

        val tasks = taskNames.mapIndexed { index, name ->
            WeeklyGoalTask(goalId = goalId, name = name, taskOrder = index + 1)
        }
        taskDao.insertTasks(tasks)
    }

    /** タスクの達成状態をトグルする（未達成→達成、達成→未達成） */
    suspend fun toggleTask(task: WeeklyGoalTask) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    // --- バックアップ用 ---
    suspend fun getAllGoalsOnce(): List<WeeklyGoal> = goalDao.getAllGoals().first().let {
        // Flow の first() で一度だけ取得する
        it
    }
    suspend fun getAllGoalTasksOnce(): List<WeeklyGoalTask> = taskDao.getAllTasksOnce()

    // --- リストア用 ---
    suspend fun insertAllGoals(goals: List<WeeklyGoal>) = goalDao.insertAll(goals)
    suspend fun insertAllGoalTasks(tasks: List<WeeklyGoalTask>) = taskDao.insertAll(tasks)
}
