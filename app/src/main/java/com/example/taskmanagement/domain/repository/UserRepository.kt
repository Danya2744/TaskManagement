package com.example.taskmanagement.domain.repository

import com.example.taskmanagement.data.entities.UserEntity
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getAllUsers(): Flow<List<UserEntity>>
    fun getUsersByRole(role: UserEntity.Role): Flow<List<UserEntity>>
    fun searchUsers(query: String): Flow<List<UserEntity>>
    suspend fun getUserById(userId: Long): UserEntity?
    suspend fun getUserByEmail(email: String): UserEntity?
    suspend fun getUserByUsername(username: String): UserEntity?
    suspend fun createUser(user: UserEntity): Long
    suspend fun updateUser(user: UserEntity)
    suspend fun deleteUser(user: UserEntity)
    suspend fun checkEmailExists(email: String): Boolean
    suspend fun checkUsernameExists(username: String): Boolean
    suspend fun authenticate(emailOrUsername: String, password: String): AuthResult
    suspend fun getCurrentUser(): UserEntity?
    suspend fun logout()
}

sealed class AuthResult {
    data class Success(val user: UserEntity) : AuthResult()
    data class Error(val message: String) : AuthResult()
}