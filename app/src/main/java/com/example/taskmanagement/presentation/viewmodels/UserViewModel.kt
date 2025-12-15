package com.example.taskmanagement.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.domain.repository.AuthResult
import com.example.taskmanagement.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _users = MutableStateFlow<List<UserEntity>>(emptyList())
    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        loadUsers()
        loadCurrentUser()
    }

    suspend fun getUserById(userId: Long): UserEntity? {
        return userRepository.getUserById(userId)
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getAllUsers()
                .collect { userList ->
                    _users.value = userList
                }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            _currentUser.value = userRepository.getCurrentUser()
        }
    }

    fun login(emailOrUsername: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = userRepository.authenticate(emailOrUsername, password)) {
                is AuthResult.Success -> {
                    _currentUser.value = result.user
                    _authState.value = AuthState.Success
                }
                is AuthResult.Error -> {
                    _authState.value = AuthState.Error(result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            userRepository.logout()
            _currentUser.value = null
            _authState.value = AuthState.LoggedOut
        }
    }

    fun createUser(user: UserEntity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                userRepository.createUser(user)
                _authState.value = AuthState.Success
                loadUsers()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to create user: ${e.message}")
            }
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                userRepository.updateUser(user)
                _authState.value = AuthState.Success
                loadUsers()
                if (user.id == _currentUser.value?.id) {
                    loadCurrentUser()
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to update user: ${e.message}")
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                userRepository.deleteUser(user)
                _authState.value = AuthState.Success
                loadUsers()
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Failed to delete user: ${e.message}")
            }
        }
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    object LoggedOut : AuthState()
    data class Error(val message: String) : AuthState()
}