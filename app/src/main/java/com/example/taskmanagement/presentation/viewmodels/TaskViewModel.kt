package com.example.taskmanagement.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.TaskFullInfo
import com.example.taskmanagement.domain.repository.TaskRepository
import com.example.taskmanagement.domain.repository.TaskStatistics
import com.example.taskmanagement.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasks: StateFlow<List<TaskEntity>> = _tasks.asStateFlow()

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Idle)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow<TaskFilter>(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    private val _formState = MutableStateFlow<TaskFormState>(TaskFormState.Idle)
    val formState: StateFlow<TaskFormState> = _formState.asStateFlow()

    init {
        loadTasks()
        observeSearch()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()

            _uiState.value = TaskUiState.Loading

            try {
                val tasksFlow = if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                    taskRepository.getAllTasks()
                } else {
                    currentUser?.let { user ->
                        taskRepository.getTasksForUser(user.id)
                    } ?: flow { emit(emptyList()) }
                }

                tasksFlow
                    .catch { exception ->
                        _uiState.value = TaskUiState.Error(exception.message ?: "Unknown error")
                    }
                    .collect { taskList ->
                        _tasks.value = taskList
                        _uiState.value = TaskUiState.Success
                    }
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to load tasks")
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    val currentUser = userRepository.getCurrentUser()

                    val baseFlow = if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                        taskRepository.getAllTasks()
                    } else {
                        currentUser?.let { user ->
                            taskRepository.getTasksForUser(user.id)
                        } ?: flow { emit(emptyList()) }
                    }

                    if (query.isBlank()) {
                        baseFlow
                    } else {
                        baseFlow.map { tasks ->
                            tasks.filter { task ->
                                task.title.contains(query, ignoreCase = true) ||
                                        (task.description?.contains(query, ignoreCase = true) ?: false)
                            }
                        }
                    }
                }
                .collect { tasks ->
                    _tasks.value = tasks
                }
        }
    }

    fun createTask(
        title: String,
        description: String?,
        priority: TaskEntity.Priority,
        categoryId: Long,
        assignedToUserId: Long?,
        createdByUserId: Long,
        dueDate: java.util.Date?
    ) {
        viewModelScope.launch {
            _formState.value = TaskFormState.Loading
            try {
                val task = TaskEntity(
                    title = title,
                    description = description,
                    priority = priority,
                    dueDate = dueDate,
                    categoryId = categoryId,
                    assignedToUserId = assignedToUserId,
                    createdByUserId = createdByUserId,
                    createdAt = java.util.Date(),
                    updatedAt = java.util.Date()
                )
                taskRepository.createTask(task)
                _formState.value = TaskFormState.Success("Задача создана успешно")
                loadTasks()
            } catch (e: Exception) {
                _formState.value = TaskFormState.Error(e.message ?: "Не удалось создать задачу")
            }
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            _formState.value = TaskFormState.Loading
            try {
                taskRepository.updateTask(task)
                _formState.value = TaskFormState.Success("Задача обновлена успешно")
                loadTasks()
            } catch (e: Exception) {
                _formState.value = TaskFormState.Error(e.message ?: "Не удалось обновить задачу")
            }
        }
    }

    fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletion(taskId, isCompleted)
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to update task")
            }
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            _uiState.value = TaskUiState.Loading
            try {
                taskRepository.deleteTask(task)
                _uiState.value = TaskUiState.Success
                loadTasks()
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to delete task")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        viewModelScope.launch {
            val currentUser = userRepository.getCurrentUser()

            val baseFlow = if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                taskRepository.getAllTasks()
            } else {
                currentUser?.let { user ->
                    taskRepository.getTasksForUser(user.id)
                } ?: flow { emit(emptyList()) }
            }

            when (filter) {
                TaskFilter.ALL -> baseFlow.collect { _tasks.value = it }
                TaskFilter.COMPLETED -> baseFlow.map { it.filter { task -> task.isCompleted } }
                    .collect { _tasks.value = it }
                TaskFilter.PENDING -> baseFlow.map { it.filter { task -> !task.isCompleted } }
                    .collect { _tasks.value = it }
                TaskFilter.HIGH_PRIORITY -> baseFlow.map { it.filter { task ->
                    task.priority == TaskEntity.Priority.HIGH
                } }.collect { _tasks.value = it }
            }
        }
    }

    fun getStatistics(): Flow<TaskStatistics> {
        return flow {
            val currentUser = userRepository.getCurrentUser()
            while (true) {
                val statistics = if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                    taskRepository.getTaskStatistics()
                } else {
                    currentUser?.let { user ->
                        taskRepository.getUserTaskStatistics(user.id)
                    } ?: TaskStatistics(0, 0, 0.0)
                }
                emit(statistics)
                delay(5000)
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun getTaskFullInfo(taskId: Long): TaskFullInfo? {
        return taskRepository.getTaskFullInfo(taskId)
    }

    fun resetFormState() {
        _formState.value = TaskFormState.Idle
    }
}

sealed class TaskUiState {
    object Idle : TaskUiState()
    object Loading : TaskUiState()
    object Success : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

sealed class TaskFormState {
    object Idle : TaskFormState()
    object Loading : TaskFormState()
    data class Success(val message: String) : TaskFormState()
    data class Error(val message: String) : TaskFormState()
}

enum class TaskFilter {
    ALL, COMPLETED, PENDING, HIGH_PRIORITY
}