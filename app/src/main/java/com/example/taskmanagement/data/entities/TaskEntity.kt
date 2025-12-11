package com.example.taskmanagement.data.entities

import androidx.room.*
import java.util.Date

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["title"], unique = false)]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id")
    val id: Long = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "priority")
    val priority: Priority,

    @ColumnInfo(name = "due_date")
    val dueDate: Date?,

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "category_id")
    val categoryId: Long,

    @ColumnInfo(name = "assigned_to_user_id")
    val assignedToUserId: Long? = null,

    @ColumnInfo(name = "created_by_user_id")
    val createdByUserId: Long,

    @ColumnInfo(name = "created_at")
    val createdAt: Date,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Date
) {
    enum class Priority {
        LOW, MEDIUM, HIGH
    }
}