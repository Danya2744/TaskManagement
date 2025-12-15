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
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    private val _searchQuery = MutableStateFlow("")

    val users: StateFlow<List<UserEntity>> = _users.asStateFlow()
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredUsers: StateFlow<List<UserEntity>> = combine(
        _users,
        _searchQuery
    ) { users, query ->
        if (query.isBlank()) {
            users
        } else {
            users.filter { user ->
                user.getFullName().contains(query, ignoreCase = true) ||
                        user.email.contains(query, ignoreCase = true) ||
                        user.username.contains(query, ignoreCase = true) ||
                        user.firstName.contains(query, ignoreCase = true) ||
                        user.lastName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
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