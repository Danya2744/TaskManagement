package com.example.taskmanagement.presentation.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.presentation.viewmodels.TaskViewModel
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class CreateTaskFragment : Fragment() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAssignedTo: Spinner
    private lateinit var btnDueDate: Button
    private lateinit var btnCreate: Button
    private lateinit var btnCancel: Button // ДОБАВЛЕНО
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private var selectedDueDate: Date? = null
    private var categories = listOf<Pair<Long, String>>() // ID и название
    private var users = listOf<Pair<Long, String>>() // ID и имя

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_create_task, container, false)

        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        spinnerPriority = view.findViewById(R.id.spinnerPriority)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerAssignedTo = view.findViewById(R.id.spinnerAssignedTo)
        btnDueDate = view.findViewById(R.id.btnDueDate)
        btnCreate = view.findViewById(R.id.btnCreate)
        btnCancel = view.findViewById(R.id.btnCancel) // ДОБАВЛЕНО
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        setupObservers()
    }

    private fun setupSpinners() {
        // Приоритеты
        val priorities = listOf(
            TaskEntity.Priority.LOW to "Низкий",
            TaskEntity.Priority.MEDIUM to "Средний",
            TaskEntity.Priority.HIGH to "Высокий"
        )
        val priorityAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            priorities.map { it.second }
        )
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter

        // Категории (загружаем позже)
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        )
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Пользователи (загружаем позже)
        val userAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        )
        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAssignedTo.adapter = userAdapter

        // Загружаем данные
        loadCategories()
        loadUsers()
    }

    private fun loadCategories() {
        // TODO: Загрузить категории из ViewModel
        categories = listOf(
            1L to "Работа",
            2L to "Личное",
            3L to "Срочные",
            4L to "Планирование"
        )
        updateCategorySpinner()
    }

    private fun loadUsers() {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.users.collect { userList ->
                users = userList.map { it.id to it.getFullName() }
                updateUserSpinner()
            }
        }
    }

    private fun updateCategorySpinner() {
        val adapter = spinnerCategory.adapter as ArrayAdapter<String>
        adapter.clear()
        adapter.addAll(categories.map { it.second })
        adapter.notifyDataSetChanged()
    }

    private fun updateUserSpinner() {
        val adapter = spinnerAssignedTo.adapter as ArrayAdapter<String>
        adapter.clear()
        adapter.addAll(users.map { it.second })
        adapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        btnDueDate.setOnClickListener {
            showDatePicker()
        }

        btnCreate.setOnClickListener {
            createTask()
        }

        btnCancel.setOnClickListener { // ДОБАВЛЕНО
            findNavController().navigateUp()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDueDate = calendar.time
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                btnDueDate.text = dateFormat.format(selectedDueDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun createTask() {
        val title = etTitle.text.toString()
        val description = etDescription.text.toString()

        if (title.isBlank()) {
            showError("Введите название задачи")
            return
        }

        val selectedPriorityIndex = spinnerPriority.selectedItemPosition
        val selectedCategoryIndex = spinnerCategory.selectedItemPosition
        val selectedUserIndex = spinnerAssignedTo.selectedItemPosition

        if (selectedCategoryIndex < 0 || categories.isEmpty()) {
            showError("Выберите категорию")
            return
        }

        val priority = when (selectedPriorityIndex) {
            0 -> TaskEntity.Priority.LOW
            1 -> TaskEntity.Priority.MEDIUM
            2 -> TaskEntity.Priority.HIGH
            else -> TaskEntity.Priority.MEDIUM
        }

        val categoryId = categories[selectedCategoryIndex].first
        val assignedToUserId = if (selectedUserIndex >= 0 && users.isNotEmpty()) {
            users[selectedUserIndex].first
        } else null

        val currentUser = userViewModel.currentUser.value
        if (currentUser == null) {
            showError("Пользователь не авторизован")
            return
        }

        taskViewModel.createTask(
            title = title,
            description = if (description.isBlank()) null else description,
            priority = priority,
            categoryId = categoryId,
            assignedToUserId = assignedToUserId,
            createdByUserId = currentUser.id,
            dueDate = selectedDueDate
        )
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.uiState.collect { state ->
                when (state) {
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Loading -> {
                        showLoading(true)
                        clearError()
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Success -> {
                        showLoading(false)
                        // Возвращаемся к списку задач
                        findNavController().navigateUp()
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    else -> showLoading(false)
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnCreate.isEnabled = !show
        btnCancel.isEnabled = !show // ДОБАВЛЕНО
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun clearError() {
        tvError.visibility = View.GONE
    }
}