package com.example.taskmanagement.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.TaskFullInfo
import com.example.taskmanagement.domain.repository.TaskRepository
import com.example.taskmanagement.domain.repository.TaskStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasks: StateFlow<List<TaskEntity>> = _tasks.asStateFlow()

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow<TaskFilter>(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()

    init {
        loadTasks()
        observeSearch()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            taskRepository.getAllTasks()
                .catch { exception ->
                    _uiState.value = TaskUiState.Error(exception.message ?: "Unknown error")
                }
                .collect { taskList ->
                    _tasks.value = taskList
                    _uiState.value = TaskUiState.Success
                }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        taskRepository.getAllTasks()
                    } else {
                        taskRepository.searchTasks(query)
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
            _uiState.value = TaskUiState.Loading
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
                _uiState.value = TaskUiState.Success
            } catch (e: Exception) {
                _uiState.value = TaskUiState.Error(e.message ?: "Failed to create task")
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        viewModelScope.launch {
            when (filter) {
                TaskFilter.ALL -> taskRepository.getAllTasks().collect { _tasks.value = it }
                TaskFilter.COMPLETED -> taskRepository.getTasksByCompletion(true).collect { _tasks.value = it }
                TaskFilter.PENDING -> taskRepository.getTasksByCompletion(false).collect { _tasks.value = it }
                TaskFilter.HIGH_PRIORITY -> taskRepository.getTasksByPriority(TaskEntity.Priority.HIGH).collect { _tasks.value = it }
            }
        }
    }

    fun getStatistics(): Flow<TaskStatistics> {
        return flow {
            while (true) {
                emit(taskRepository.getTaskStatistics())
                kotlinx.coroutines.delay(5000) // Обновлять каждые 5 секунд
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    fun getUserStatistics(userId: Long): Flow<TaskStatistics> {
        return flow {
            while (true) {
                emit(taskRepository.getUserTaskStatistics(userId))
                kotlinx.coroutines.delay(5000)
            }
        }.flowOn(kotlinx.coroutines.Dispatchers.IO)
    }

    suspend fun getTaskFullInfo(taskId: Long): TaskFullInfo? {
        return taskRepository.getTaskFullInfo(taskId)
    }
}

sealed class TaskUiState {
    object Loading : TaskUiState()
    object Success : TaskUiState()
    data class Error(val message: String) : TaskUiState()
}

enum class TaskFilter {
    ALL, COMPLETED, PENDING, HIGH_PRIORITY
}