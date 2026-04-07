package com.example.adhd_routine_management.data.database

import androidx.room.TypeConverter
import com.example.adhd_routine_management.data.database.entity.CompletionStatus

/**
 * Room は Enum を直接保存できないため、文字列に変換するコンバーターが必要。
 * @TypeConverters アノテーションで AppDatabase に登録して使う。
 */
class Converters {
    @TypeConverter
    fun fromStatus(status: CompletionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): CompletionStatus = CompletionStatus.valueOf(value)
}
