package com.example.taskmanagement.data.repository

import com.example.taskmanagement.data.dao.TaskDao
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.TaskFullInfo
import com.example.taskmanagement.domain.repository.TaskRepository
import com.example.taskmanagement.domain.repository.TaskStatistics
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {

    override fun getAllTasks(): Flow<List<TaskEntity>> {
        return taskDao.getAllTasks()
    }

    override fun getAllTasksFullInfo(): Flow<List<TaskFullInfo>> {
        return taskDao.getAllTasksFullInfo()
    }

    override fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksByCategory(categoryId)
    }

    override fun getTasksByPriority(priority: TaskEntity.Priority): Flow<List<TaskEntity>> {
        return taskDao.getTasksByPriority(priority)
    }

    override suspend fun toggleTaskCompletion(taskId: Long, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }

    override fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskEntity>> {
        return taskDao.getTasksByCompletion(isCompleted)
    }

    override fun getUserRelatedTasks(userId: Long): Flow<List<TaskFullInfo>> {
        return taskDao.getUserRelatedTasks(userId)
    }

    override fun getTasksAssignedToUser(userId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksAssignedToUser(userId)
    }

    override suspend fun getTaskFullInfo(taskId: Long): TaskFullInfo? {
        return taskDao.getTaskFullInfo(taskId)
    }

    override suspend fun createTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    override suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    override suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    override fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchTasks(query)
    }

    override suspend fun getTaskStatistics(): TaskStatistics {
        val totalTasks = taskDao.getTotalTasksCount()
        val completedTasks = taskDao.getCompletedTasksCount()
        return TaskStatistics(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            completionRate = if (totalTasks > 0) {
                (completedTasks.toDouble() / totalTasks) * 100
            } else 0.0
        )
    }

    override suspend fun getUserTaskStatistics(userId: Long): TaskStatistics {
        val totalTasks = taskDao.getTotalTasksCountForUser(userId)
        val completedTasks = taskDao.getCompletedTasksCountForUser(userId)
        return TaskStatistics(
            totalTasks = totalTasks,
            completedTasks = completedTasks,
            completionRate = if (totalTasks > 0) {
                (completedTasks.toDouble() / totalTasks) * 100
            } else 0.0
        )
    }
}