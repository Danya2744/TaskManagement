package com.example.taskmanagement.data.dao

import androidx.room.*
import com.example.taskmanagement.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserEntity.Role): Flow<List<UserEntity>>

    @Query("UPDATE users SET is_active = :isActive WHERE user_id = :userId")
    suspend fun updateUserStatus(userId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun checkEmailExists(email: String): Int

    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun checkUsernameExists(username: String): Int

    @Query("UPDATE users SET password_hash = :newPasswordHash, salt = :newSalt WHERE user_id = :userId")
    suspend fun updatePassword(userId: Long, newPasswordHash: String, newSalt: String)

    @Delete
    suspend fun deleteUser(user: UserEntity)
}