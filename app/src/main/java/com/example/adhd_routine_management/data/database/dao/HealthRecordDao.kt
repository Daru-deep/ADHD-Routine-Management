package com.example.adhd_routine_management.data.database.dao

import androidx.room.*
import com.example.adhd_routine_management.data.database.entity.HealthRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthRecordDao {

    /** 指定日の全スロット（朝・昼・夕）を監視する */
    @Query("SELECT * FROM health_records WHERE date = :date ORDER BY timeSlot")
    fun getRecordsForDate(date: String): Flow<List<HealthRecord>>

    /** 指定日の全スロットを一度だけ取得する（バックアップ用） */
    @Query("SELECT * FROM health_records WHERE date = :date")
    suspend fun getRecordsForDateOnce(date: String): List<HealthRecord>

    /** 指定期間の記録を取得する（週次サマリー用） */
    @Query("SELECT * FROM health_records WHERE date >= :from AND date <= :to ORDER BY date, timeSlot")
    suspend fun getRecordsForRange(from: String, to: String): List<HealthRecord>

    /** 全記録を取得（バックアップ用） */
    @Query("SELECT * FROM health_records ORDER BY date, timeSlot")
    suspend fun getAllRecordsOnce(): List<HealthRecord>

    /**
     * 既存レコードがあれば上書き、なければ挿入する。
     * REPLACE ストラテジーは PrimaryKey が一致する行を置き換える。
     * (date, timeSlot) でユニーク管理したいが、Room の複合ユニーク制約は
     * REPLACE では使いにくいため、アプリ側で upsert ロジックを持つ。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: HealthRecord)

    /** バックアップリストア用：複数件を一括挿入 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HealthRecord>)
}
