package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.RoutineTask
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTaskDao {

    // Flow を返すと、データが変わったとき自動でUIに通知される
    @Query("SELECT * FROM routine_tasks ORDER BY hourOfDay, minute")
    fun getAllTasks(): Flow<List<RoutineTask>>

    @Query("SELECT * FROM routine_tasks WHERE isActive = 1 ORDER BY hourOfDay, minute")
    fun getActiveTasks(): Flow<List<RoutineTask>>

    // suspend = コルーチン（非同期）の中でだけ呼べる関数
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: RoutineTask)

    @Update
    suspend fun updateTask(task: RoutineTask)

    @Delete
    suspend fun deleteTask(task: RoutineTask)

    @Query("SELECT * FROM routine_tasks WHERE isActive = 1")
    suspend fun getActiveTasksOnce(): List<RoutineTask>

    // バックアップ用：全件取得
    @Query("SELECT * FROM routine_tasks ORDER BY hourOfDay, minute")
    suspend fun getAllTasksOnce(): List<RoutineTask>

    // リストア用：全件挿入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<RoutineTask>)
}
