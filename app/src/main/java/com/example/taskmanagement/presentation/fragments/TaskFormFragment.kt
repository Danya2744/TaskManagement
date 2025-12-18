package com.example.taskmanagement.presentation.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.presentation.viewmodels.TaskViewModel
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
@AndroidEntryPoint
class TaskFormFragment : Fragment() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerAssignedTo: Spinner
    private lateinit var btnDueDate: Button
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView

    private var selectedDueDate: Date? = null
    private var selectedTime: Calendar? = null
    private var taskId: Long = -1
    private var isEditMode = false
    private var categories = listOf<Pair<Long, String>>()
    private var users = listOf<Pair<Long, String>>()
    private var currentUserIsAdmin = false
    private var currentUserId: Long? = null
    private lateinit var userAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_form, container, false)

        tvTitle = view.findViewById(R.id.tvTitle)
        etTitle = view.findViewById(R.id.etTitle)
        etDescription = view.findViewById(R.id.etDescription)
        spinnerPriority = view.findViewById(R.id.spinnerPriority)
        spinnerCategory = view.findViewById(R.id.spinnerCategory)
        spinnerAssignedTo = view.findViewById(R.id.spinnerAssignedTo)
        btnDueDate = view.findViewById(R.id.btnDueDate)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        taskId = arguments?.getLong("taskId") ?: -1
        isEditMode = taskId != -1L

        setupSpinners()
        setupTextWatchers()
        setupClickListeners()
        setupObservers()

        loadCurrentUserInfo {
            loadCategoriesAndUsers()

            if (isEditMode) {
                tvTitle.text = "Редактировать задачу"
            } else {
                tvTitle.text = "Создать задачу"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        taskViewModel.resetFormState()
        clearError()
    }

    private fun setupSpinners() {
        val priorities = listOf(
            TaskEntity.Priority.LOW to "Низкий",
            TaskEntity.Priority.MEDIUM to "Средний",
            TaskEntity.Priority.HIGH to "Высокий"
        )

        val priorityAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            priorities.map { it.second }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }
        }

        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = priorityAdapter

        val categoryAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf<String>()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }
        }

        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        userAdapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            mutableListOf("Загрузка пользователей...")
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }
        }

        userAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerAssignedTo.adapter = userAdapter
        spinnerAssignedTo.isEnabled = false
    }

    private fun loadCurrentUserInfo(onComplete: () -> Unit) {
        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.currentUser.collect { currentUser ->
                if (currentUser != null) {
                    currentUserIsAdmin = currentUser.role == UserEntity.Role.ADMIN
                    currentUserId = currentUser.id
                    onComplete()
                    return@collect
                }
            }
        }
    }

    private fun setupTextWatchers() {
        etTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateTitleInRealTime(s.toString())
            }
        })

        etTitle.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            private var previousLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                previousLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s.isNullOrEmpty()) return

                if (previousLength == 0 && s.length == 1) {
                    isFormatting = true
                    val firstChar = s[0]
                    if (firstChar.isLetter() && firstChar.isLowerCase()) {
                        s.replace(0, 1, firstChar.uppercaseChar().toString())
                    }
                    isFormatting = false
                }
            }
        })

        etDescription.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s.isNullOrEmpty()) return

                val text = s.toString()
                val sentences = text.split(Regex("(?<=[.!?])\\s+"))

                if (sentences.size > 1) {
                    isFormatting = true
                    var formattedText = ""

                    for (sentence in sentences) {
                        if (sentence.isNotEmpty()) {
                            val trimmedSentence = sentence.trim()
                            if (trimmedSentence.isNotEmpty()) {
                                val firstChar = trimmedSentence[0]
                                val formattedSentence = if (firstChar.isLetter() && firstChar.isLowerCase()) {
                                    firstChar.uppercaseChar() + trimmedSentence.substring(1)
                                } else {
                                    trimmedSentence
                                }
                                formattedText += formattedSentence + " "
                            }
                        }
                    }

                    s?.replace(0, s.length, formattedText.trim())
                    isFormatting = false
                }
            }
        })
    }

    private fun validateTitleInRealTime(title: String) {
        if (title.length in 1..4) {
            showTitleHint("Название должно содержать минимум 5 символов")
        } else {
            clearTitleHint()
        }
    }

    private fun showTitleHint(message: String) {
        etTitle.error = message
    }

    private fun clearTitleHint() {
        etTitle.error = null
    }

    private fun loadCategoriesAndUsers() {
        categories = listOf(
            1L to "Работа",
            2L to "Личное",
            3L to "Срочные",
            4L to "Планирование"
        )

        updateCategorySpinner()

        viewLifecycleOwner.lifecycleScope.launch {
            userViewModel.users.collect { userList ->
                if (userList.isNotEmpty()) {
                    users = userList.map { it.id to it.getFullName() }
                    updateUserSpinner()

                    spinnerAssignedTo.isEnabled = true

                    if (isEditMode) {
                        loadTaskData()
                    }
                } else {
                    userAdapter.clear()
                    userAdapter.add("Нет пользователей в системе")
                    userAdapter.notifyDataSetChanged()
                }
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
        userAdapter.clear()

        if (users.isEmpty()) {
            userAdapter.add("Нет пользователей в системе")
            userAdapter.notifyDataSetChanged()
            return
        }

        if (currentUserIsAdmin) {
            userAdapter.addAll(users.map { it.second })
        } else {
            val currentUserItem = users.find { it.first == currentUserId }
            if (currentUserItem != null) {
                userAdapter.add(currentUserItem.second)
            } else {
                userAdapter.add("Текущий пользователь не найден")
            }
        }

        userAdapter.notifyDataSetChanged()

        if (!isEditMode) {
            if (currentUserIsAdmin && users.isNotEmpty()) {
                spinnerAssignedTo.setSelection(0)
            } else {
                spinnerAssignedTo.setSelection(0)
            }
        }
    }

    private fun loadTaskData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val taskFullInfo = taskViewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                etTitle.setText(taskInfo.task.title)
                etDescription.setText(taskInfo.task.description ?: "")

                val priorityIndex = when (taskInfo.task.priority) {
                    TaskEntity.Priority.LOW -> 0
                    TaskEntity.Priority.MEDIUM -> 1
                    TaskEntity.Priority.HIGH -> 2
                }

                spinnerPriority.setSelection(priorityIndex)

                val categoryIndex = categories.indexOfFirst { it.first == taskInfo.task.categoryId }
                if (categoryIndex != -1) {
                    spinnerCategory.setSelection(categoryIndex)
                }

                if (currentUserIsAdmin) {
                    if (taskInfo.task.assignedToUserId != null && users.isNotEmpty()) {
                        val userIndex = users.indexOfFirst { it.first == taskInfo.task.assignedToUserId }
                        if (userIndex >= 0 && userIndex < userAdapter.count) {
                            spinnerAssignedTo.setSelection(userIndex)
                        } else if (userAdapter.count > 0) {
                            spinnerAssignedTo.setSelection(0)
                        }
                    } else if (userAdapter.count > 0) {
                        spinnerAssignedTo.setSelection(0)
                    }
                } else {
                    if (userAdapter.count > 0) {
                        spinnerAssignedTo.setSelection(0)
                    }
                }

                selectedDueDate = taskInfo.task.dueDate
                if (selectedDueDate != null) {
                    val calendar = Calendar.getInstance().apply {
                        time = selectedDueDate!!
                    }
                    selectedTime = calendar
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    btnDueDate.text = dateFormat.format(selectedDueDate)
                } else {
                    btnDueDate.text = "Установить срок"
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnDueDate.setOnClickListener {
            showDatePicker()
        }

        btnSave.setOnClickListener {
            if (isEditMode) {
                updateTask()
            } else {
                createTask()
            }
        }

        btnCancel.setOnClickListener {
            navigateBackToTaskList()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDueDate?.let {
            calendar.time = it
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                showTimePicker(calendar)
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showTimePicker(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
                selectedDueDate = calendar.time
                selectedTime = calendar
                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                btnDueDate.text = dateFormat.format(selectedDueDate)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.setTitle("Выберите время")
        timePickerDialog.show()
    }

    private fun validateTaskInput(
        title: String,
        selectedCategoryIndex: Int
    ): Boolean {
        if (title.isBlank()) {
            showError("Введите название задачи")
            return false
        }

        if (title.length < 5) {
            showError("Название должно содержать минимум 5 символов")
            return false
        }

        if (selectedCategoryIndex < 0 || selectedCategoryIndex >= categories.size) {
            showError("Выберите категорию")
            return false
        }

        if (userAdapter.count == 0) {
            showError("Нет доступных пользователей для назначения")
            return false
        }

        if (!spinnerAssignedTo.isEnabled) {
            showError("Список пользователей еще не загружен")
            return false
        }

        if (spinnerAssignedTo.selectedItemPosition < 0) {
            showError("Выберите пользователя для назначения")
            return false
        }

        val selectedUserText = spinnerAssignedTo.selectedItem as? String
        if (selectedUserText == null || selectedUserText == "Загрузка пользователей..." ||
            selectedUserText == "Нет пользователей в системе" ||
            selectedUserText == "Текущий пользователь не найден") {
            showError("Некорректный выбор пользователя")
            return false
        }

        return true
    }

    private fun createTask() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val selectedCategoryIndex = spinnerCategory.selectedItemPosition

        if (!validateTaskInput(title, selectedCategoryIndex)) {
            return
        }

        val selectedPriorityIndex = spinnerPriority.selectedItemPosition
        val priority = when (selectedPriorityIndex) {
            0 -> TaskEntity.Priority.LOW
            1 -> TaskEntity.Priority.MEDIUM
            2 -> TaskEntity.Priority.HIGH
            else -> TaskEntity.Priority.MEDIUM
        }

        val categoryId = categories[selectedCategoryIndex].first
        val selectedUserIndex = spinnerAssignedTo.selectedItemPosition

        val currentUser = userViewModel.currentUser.value
        if (currentUser == null) {
            showError("Пользователь не авторизован")
            return
        }

        val assignedToUserId = if (currentUserIsAdmin) {
            if (selectedUserIndex >= 0 && users.isNotEmpty() && selectedUserIndex < users.size) {
                users[selectedUserIndex].first
            } else {
                showError("Выберите пользователя для назначения задачи")
                return
            }
        } else {
            currentUser.id
        }

        if (assignedToUserId == null) {
            showError("Задача должна быть назначена пользователю")
            return
        }

        selectedDueDate?.let { dueDate ->
            if (dueDate.before(Date())) {
                showError("Срок выполнения не может быть в прошлом")
                return
            }
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

    private fun updateTask() {
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val selectedCategoryIndex = spinnerCategory.selectedItemPosition

        if (!validateTaskInput(title, selectedCategoryIndex)) {
            return
        }

        val selectedPriorityIndex = spinnerPriority.selectedItemPosition
        val priority = when (selectedPriorityIndex) {
            0 -> TaskEntity.Priority.LOW
            1 -> TaskEntity.Priority.MEDIUM
            2 -> TaskEntity.Priority.HIGH
            else -> TaskEntity.Priority.MEDIUM
        }

        val categoryId = categories[selectedCategoryIndex].first

        val currentUser = userViewModel.currentUser.value
        if (currentUser == null) {
            showError("Пользователь не авторизован")
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val taskFullInfo = taskViewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                val assignedToUserId = if (currentUserIsAdmin) {
                    val selectedUserIndex = spinnerAssignedTo.selectedItemPosition
                    if (selectedUserIndex >= 0 && users.isNotEmpty() && selectedUserIndex < users.size) {
                        users[selectedUserIndex].first
                    } else {
                        showError("Выберите пользователя для назначения задачи")
                        return@launch
                    }
                } else {
                    currentUser.id
                }

                if (assignedToUserId == null) {
                    showError("Задача должна быть назначена пользователю")
                    return@launch
                }

                val dueDateToUse = if (selectedDueDate != null) {
                    if (selectedDueDate!!.before(Date())) {
                        showError("Срок выполнения не может быть в прошлом")
                        return@launch
                    }
                    selectedDueDate
                } else {
                    taskInfo.task.dueDate
                }

                val updatedTask = taskInfo.task.copy(
                    title = title,
                    description = if (description.isBlank()) null else description,
                    priority = priority,
                    categoryId = categoryId,
                    assignedToUserId = assignedToUserId,
                    dueDate = dueDateToUse,
                    updatedAt = Date()
                )

                taskViewModel.updateTask(updatedTask)
            } ?: run {
                showError("Задача не найдена")
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.formState.collect { state ->
                when (state) {
                    is com.example.taskmanagement.presentation.viewmodels.TaskFormState.Loading -> {
                        showLoading(true)
                        clearError()
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskFormState.Success -> {
                        showLoading(false)
                        Toast.makeText(
                            requireContext(),
                            state.message,
                            Toast.LENGTH_SHORT
                        ).show()
                        lifecycleScope.launch {
                            delay(400)
                            navigateBackToTaskList()
                        }
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskFormState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskFormState.Idle -> {
                        showLoading(false)
                        clearError()
                    }
                }
            }
        }
    }

    private fun navigateBackToTaskList() {
        if (!findNavController().popBackStack()) {
            findNavController().navigate(R.id.taskListFragment)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
        btnCancel.isEnabled = !show
        btnDueDate.isEnabled = !show
        spinnerPriority.isEnabled = !show
        spinnerCategory.isEnabled = !show
        spinnerAssignedTo.isEnabled = !show && spinnerAssignedTo.isEnabled
        etTitle.isEnabled = !show
        etDescription.isEnabled = !show
    }

    private fun showError(message: String) {
        tvError.setTextColor(resources.getColor(R.color.login_error, null))
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun clearError() {
        tvError.visibility = View.GONE
        tvError.text = ""
    }
}