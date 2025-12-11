package com.example.taskmanagement.domain.repository

import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.TaskFullInfo
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskEntity>>
    fun getAllTasksFullInfo(): Flow<List<TaskFullInfo>>
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>>
    fun getUserRelatedTasks(userId: Long): Flow<List<TaskFullInfo>>
    fun getTasksAssignedToUser(userId: Long): Flow<List<TaskEntity>>
    suspend fun getTaskFullInfo(taskId: Long): TaskFullInfo?
    suspend fun createTask(task: TaskEntity): Long
    suspend fun updateTask(task: TaskEntity)
    suspend fun deleteTask(task: TaskEntity)
    fun searchTasks(query: String): Flow<List<TaskEntity>>
    suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean)
    suspend fun getTaskStatistics(): TaskStatistics
    suspend fun getUserTaskStatistics(userId: Long): TaskStatistics
}

data class TaskStatistics(
    val totalTasks: Int,
    val completedTasks: Int,
    val completionRate: Double
)