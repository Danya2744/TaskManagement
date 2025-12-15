package com.example.taskmanagement.data.entities

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithTasks(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val tasks: List<TaskEntity>
)

data class TaskWithCategory(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "category_id"
    )
    val category: CategoryEntity
)

data class TaskWithAssignee(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "assigned_to_user_id",
        entityColumn = "user_id"
    )
    val assignedTo: UserEntity?
)

data class TaskWithCreator(
    @Embedded val task: TaskEntity,
    @Relation(
        parentColumn = "created_by_user_id",
        entityColumn = "user_id"
    )
    val createdBy: UserEntity?
)

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