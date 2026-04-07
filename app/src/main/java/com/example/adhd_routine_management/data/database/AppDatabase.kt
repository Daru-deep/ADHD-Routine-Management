package com.example.adhd_routine_management.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.adhd_routine_management.data.database.dao.*
import com.example.adhd_routine_management.data.database.entity.*

/**
 * アプリ全体で1つだけ存在するデータベースクラス。
 * シングルトンパターンで、複数箇所から同じインスタンスを使い回す。
 *
 * version = スキーマ変更時にバージョンを上げる（マイグレーション対象）
 */
@Database(
    entities = [
        RoutineTask::class,
        TaskCompletion::class,
        DailyRecord::class,
        UserProgress::class,
        WeeklyGoal::class,
        WeeklyGoalTask::class,
        HealthRecord::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineTaskDao(): RoutineTaskDao
    abstract fun taskCompletionDao(): TaskCompletionDao
    abstract fun dailyRecordDao(): DailyRecordDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun weeklyGoalDao(): WeeklyGoalDao
    abstract fun weeklyGoalTaskDao(): WeeklyGoalTaskDao
    abstract fun healthRecordDao(): HealthRecordDao

    companion object {
        @Volatile // 複数スレッドから安全にアクセスするためのアノテーション
        private var INSTANCE: AppDatabase? = null

        /**
         * バージョン1→2：routine_tasks テーブルに snoozeDurationMinutes カラムを追加。
         * 既存のタスクはデフォルト値の10分として扱われる。
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE routine_tasks ADD COLUMN snoozeDurationMinutes INTEGER NOT NULL DEFAULT 10"
                )
            }
        }

        /**
         * バージョン2→3：週間短期目標のテーブルを2つ追加する。
         * weekly_goals：目標のタイトルと開始週を管理する
         * weekly_goal_tasks：目標に紐づく5つのタスクを管理する
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        weekStart TEXT NOT NULL,
                        title TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_goal_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        goalId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        taskOrder INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * バージョン3→4：体調記録テーブルを追加する。
         * id, date（日付文字列）, timeSlot（朝/昼/夕）, score（1〜10）の4カラム。
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS health_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        timeSlot TEXT NOT NULL,
                        score INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adhd_routine_db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { INSTANCE = it }
            }
        }
    }
}
