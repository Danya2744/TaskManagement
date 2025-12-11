package com.example.taskmanagement.data.dao

import androidx.room.*
import com.example.taskmanagement.data.entities.CategoryEntity
import com.example.taskmanagement.data.entities.CategoryWithTasks
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Transaction
    @Query("SELECT * FROM categories")
    fun getCategoriesWithTasks(): Flow<List<CategoryWithTasks>>

    @Query("DELETE FROM categories WHERE category_id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)
}