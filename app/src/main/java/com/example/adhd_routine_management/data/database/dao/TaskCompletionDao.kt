package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.CompletionStatus
import com.example.adhd_routine_management.data.database.entity.TaskCompletion
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskCompletionDao {

    @Query("SELECT * FROM task_completions WHERE date = :date")
    fun getCompletionsForDate(date: String): Flow<List<TaskCompletion>>

    @Query("SELECT * FROM task_completions WHERE date = :date")
    suspend fun getCompletionsForDateOnce(date: String): List<TaskCompletion>

    @Query("SELECT * FROM task_completions WHERE taskId = :taskId AND date = :date LIMIT 1")
    suspend fun getCompletion(taskId: Int, date: String): TaskCompletion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: TaskCompletion)

    @Update
    suspend fun updateCompletion(completion: TaskCompletion)

    @Query("UPDATE task_completions SET status = :status, completedAt = :completedAt WHERE taskId = :taskId AND date = :date")
    suspend fun updateStatus(taskId: Int, date: String, status: CompletionStatus, completedAt: Long?)

    // 指定日に未完了（PENDING）のタスクが1件以上あるか確認
    @Query("SELECT COUNT(*) FROM task_completions WHERE date = :date AND status = 'PENDING'")
    suspend fun getPendingCountForDate(date: String): Int

    // バックアップ用：全件取得
    @Query("SELECT * FROM task_completions")
    suspend fun getAllCompletionsOnce(): List<TaskCompletion>

    // リストア用：全件挿入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(completions: List<TaskCompletion>)
}
