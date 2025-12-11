package com.example.taskmanagement.data.database

import androidx.room.TypeConverter
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.UserEntity
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromPriority(priority: TaskEntity.Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(priority: String): TaskEntity.Priority {
        return TaskEntity.Priority.valueOf(priority)
    }

    // НОВЫЕ КОНВЕРТЕРЫ ДЛЯ РОЛИ ПОЛЬЗОВАТЕЛЯ
    @TypeConverter
    fun fromRole(role: UserEntity.Role): String {
        return role.name
    }

    @TypeConverter
    fun toRole(role: String): UserEntity.Role {
        return UserEntity.Role.valueOf(role)
    }
}