package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.DailyRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyRecordDao {

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    fun getRecordForDate(date: String): Flow<DailyRecord?>

    @Query("SELECT * FROM daily_records WHERE date = :date LIMIT 1")
    suspend fun getRecordForDateOnce(date: String): DailyRecord?

    @Query("SELECT * FROM daily_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 30): Flow<List<DailyRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: DailyRecord)

    // バックアップ用：全件取得
    @Query("SELECT * FROM daily_records ORDER BY date DESC")
    suspend fun getAllRecordsOnce(): List<DailyRecord>

    // リストア用：全件挿入
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<DailyRecord>)
}
