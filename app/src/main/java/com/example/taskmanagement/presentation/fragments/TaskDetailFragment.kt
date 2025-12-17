package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.presentation.viewmodels.TaskViewModel
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class TaskDetailFragment : Fragment() {

    private val taskViewModel: TaskViewModel by viewModels()
    private val userViewModel: UserViewModel by viewModels()

    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvPriority: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDueDate: TextView
    private lateinit var tvAssignedTo: TextView
    private lateinit var tvCreatedBy: TextView
    private lateinit var cbCompleted: CheckBox
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button
    private lateinit var scrollView: ScrollView
    private lateinit var tvStatusText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_detail, container, false)

        tvTitle = view.findViewById(R.id.tvTitle)
        tvDescription = view.findViewById(R.id.tvDescription)
        tvPriority = view.findViewById(R.id.tvPriority)
        tvCategory = view.findViewById(R.id.tvCategory)
        tvDueDate = view.findViewById(R.id.tvDueDate)
        tvAssignedTo = view.findViewById(R.id.tvAssignedTo)
        tvCreatedBy = view.findViewById(R.id.tvCreatedBy)
        cbCompleted = view.findViewById(R.id.cbCompleted)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnBack = view.findViewById(R.id.btnBack)
        scrollView = view.findViewById(R.id.scrollView ?: view.findViewById(android.R.id.content))
        tvStatusText = view.findViewById(R.id.tvStatusText)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadTask()
        checkUserPermissions()

        setupScrollView()
    }

    private fun setupScrollView() {
        scrollView.isVerticalScrollBarEnabled = true
        scrollView.isSmoothScrollingEnabled = true

        scrollView.post {
            scrollView.fullScroll(View.FOCUS_DOWN)
            scrollView.fullScroll(View.FOCUS_UP)
        }
    }

    private fun loadTask() {
        val taskId = arguments?.getLong("taskId") ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val taskFullInfo = taskViewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                tvTitle.text = taskInfo.task.title
                tvDescription.text = taskInfo.task.description ?: "Нет описания"

                val priorityText = when (taskInfo.task.priority) {
                    TaskEntity.Priority.LOW -> "Низкий"
                    TaskEntity.Priority.MEDIUM -> "Средний"
                    TaskEntity.Priority.HIGH -> "Высокий"
                }
                tvPriority.text = priorityText

                tvCategory.text = taskInfo.category.name

                tvDueDate.text = if (taskInfo.task.dueDate != null) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    dateFormat.format(taskInfo.task.dueDate)
                } else {
                    "не установлен"
                }

                tvAssignedTo.text = taskInfo.assignedTo?.getFullName() ?: "Не назначено"
                tvCreatedBy.text = taskInfo.createdBy.getFullName()

                cbCompleted.isChecked = taskInfo.task.isCompleted

                tvStatusText.text = if (taskInfo.task.isCompleted) "Выполнено" else "В работе"
            }
        }
    }

    private fun checkUserPermissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = userViewModel.currentUser.value
            val taskId = arguments?.getLong("taskId") ?: return@launch

            val taskFullInfo = taskViewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                val canEdit = when {
                    currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN -> true
                    currentUser?.id == taskInfo.createdBy.id -> true
                    currentUser?.id == taskInfo.task.assignedToUserId -> true
                    else -> false
                }

                btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
                btnDelete.visibility = if (canEdit) View.VISIBLE else View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            val taskId = arguments?.getLong("taskId") ?: return@setOnCheckedChangeListener
            taskViewModel.updateTaskCompletion(taskId, isChecked)

            tvStatusText.text = if (isChecked) "Выполнено" else "В работе"
        }

        btnEdit.setOnClickListener {
            val taskId = arguments?.getLong("taskId") ?: return@setOnClickListener
            val bundle = Bundle().apply {
                putLong("taskId", taskId)
            }
            findNavController().navigate(R.id.action_taskDetailFragment_to_taskFormFragment, bundle)
        }

        btnDelete.setOnClickListener {
            val taskId = arguments?.getLong("taskId") ?: return@setOnClickListener
            showDeleteConfirmation(taskId)
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showDeleteConfirmation(taskId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val taskFullInfo = taskViewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Удаление задачи")
                    .setMessage("Вы уверены, что хотите удалить задачу \"${taskInfo.task.title}\"?")
                    .setPositiveButton("Удалить") { dialog, _ ->
                        deleteTask(taskInfo.task)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun deleteTask(task: TaskEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            taskViewModel.deleteTask(task)
            findNavController().navigateUp()
        }
    }
}