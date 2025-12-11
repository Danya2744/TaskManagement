package com.example.taskmanagement.data.dao

import androidx.room.*
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.TaskFullInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE task_id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM tasks ORDER BY created_at DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE category_id = :categoryId")
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE is_completed = :isCompleted")
    fun getTasksByCompletion(isCompleted: Boolean): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority")
    fun getTasksByPriority(priority: TaskEntity.Priority): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("UPDATE tasks SET is_completed = :isCompleted WHERE task_id = :taskId")
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean)

    // НОВЫЕ ЗАПРОСЫ ДЛЯ УПРАВЛЕНИЯ ЗАДАЧАМИ СОТРУДНИКОВ
    @Query("SELECT * FROM tasks WHERE assigned_to_user_id = :userId")
    fun getTasksAssignedToUser(userId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE created_by_user_id = :userId")
    fun getTasksCreatedByUser(userId: Long): Flow<List<TaskEntity>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE task_id = :taskId")
    suspend fun getTaskFullInfo(taskId: Long): TaskFullInfo?

    @Transaction
    @Query("SELECT * FROM tasks")
    fun getAllTasksFullInfo(): Flow<List<TaskFullInfo>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE assigned_to_user_id = :userId OR created_by_user_id = :userId")
    fun getUserRelatedTasks(userId: Long): Flow<List<TaskFullInfo>>

    @Query("SELECT COUNT(*) FROM tasks WHERE is_completed = 1")
    suspend fun getCompletedTasksCount(): Int

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun getTotalTasksCount(): Int

    // Статистика по пользователю
    @Query("SELECT COUNT(*) FROM tasks WHERE assigned_to_user_id = :userId AND is_completed = 1")
    suspend fun getCompletedTasksCountForUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE assigned_to_user_id = :userId")
    suspend fun getTotalTasksCountForUser(userId: Long): Int
}