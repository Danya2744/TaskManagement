package com.example.taskmanagement.data.entities

import androidx.room.Embedded
import androidx.room.Relation

// 1. Категория со всеми своими задачами
data class CategoryWithTasks(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val tasks: List<TaskEntity>
)

// 2. Задача с информацией о своей категории
data class TaskWithCategory(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: CategoryEntity
)

// 3. Задача с информацией о назначенном пользователе (НОВОЕ)
data class TaskWithAssignee(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "assigned_to_user_id",
        entityColumn = "user_id"
    )
    val assignedTo: UserEntity?
)

// 4. Задача с информацией о создавшем пользователе (НОВОЕ)
data class TaskWithCreator(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "created_by_user_id",
        entityColumn = "user_id"
    )
    val createdBy: UserEntity?
)

// 5. Полная информация о задаче: задача, категория, назначенный, создатель (НОВОЕ, объединение)
data class TaskFullInfo(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: CategoryEntity,
    @Relation(
        parentColumn = "assigned_to_user_id",
        entityColumn = "user_id"
    )
    val assignedTo: UserEntity?,
    @Relation(
        parentColumn = "created_by_user_id",
        entityColumn = "user_id"
    )
    val createdBy: UserEntity
)