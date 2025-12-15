package com.example.taskmanagement.data.repository

import com.example.taskmanagement.data.dao.UserDao
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.domain.repository.AuthResult
import com.example.taskmanagement.domain.repository.UserRepository
import com.example.taskmanagement.domain.utils.PasswordManager
import com.example.taskmanagement.domain.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val passwordManager: PasswordManager,
    private val preferencesManager: PreferencesManager
) : UserRepository {


    override fun getAllUsers(): Flow<List<UserEntity>> {
        return userDao.getAllUsers()
    }

    override fun getUsersByRole(role: UserEntity.Role): Flow<List<UserEntity>> {
        return userDao.getUsersByRole(role)
    }

    override suspend fun getUserById(userId: Long): UserEntity? {
        return userDao.getUserById(userId)
    }

    override suspend fun getUserByEmail(email: String): UserEntity? {
        return userDao.getUserByEmail(email)
    }

    override suspend fun getUserByUsername(username: String): UserEntity? {
        return userDao.getUserByUsername(username)
    }

    override suspend fun createUser(user: UserEntity): Long {
        return userDao.insertUser(user)
    }

    override suspend fun updateUser(user: UserEntity) {
        userDao.updateUser(user)
    }

    override suspend fun deleteUser(user: UserEntity) {
        userDao.deleteUser(user)
    }

    override suspend fun checkEmailExists(email: String): Boolean {
        return userDao.checkEmailExists(email) > 0
    }

    override suspend fun checkUsernameExists(username: String): Boolean {
        return userDao.checkUsernameExists(username) > 0
    }

    override suspend fun authenticate(emailOrUsername: String, password: String): AuthResult {
        return try {
            val user = userDao.getUserByEmail(emailOrUsername)
                ?: userDao.getUserByUsername(emailOrUsername)
                ?: return AuthResult.Error("Пользователь не найден")

            if (!user.isActive) {
                return AuthResult.Error("Аккаунт деактивирован")
            }

            if (!passwordManager.verifyPassword(password, user.salt, user.passwordHash)) {
                return AuthResult.Error("Неверный пароль")
            }

            preferencesManager.saveCurrentUserId(user.id)
            AuthResult.Success(user)
        } catch (e: Exception) {
            AuthResult.Error("Ошибка аутентификации: ${e.message}")
        }
    }

    override suspend fun getCurrentUser(): UserEntity? {
        val userId = preferencesManager.getCurrentUserId()
        return userId?.let { userDao.getUserById(it) }
    }

    override suspend fun logout() {
        preferencesManager.clearCurrentUser()
    }
}