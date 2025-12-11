package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.taskmanagement.R
import com.example.taskmanagement.presentation.viewmodels.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class TaskDetailFragment : Fragment() {

    private val viewModel: TaskViewModel by viewModels()

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadTask()
    }

    private fun loadTask() {
        val taskId = arguments?.getLong("taskId") ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val taskFullInfo = viewModel.getTaskFullInfo(taskId)
            taskFullInfo?.let { taskInfo ->
                tvTitle.text = taskInfo.task.title
                tvDescription.text = taskInfo.task.description ?: "Нет описания"

                val priorityText = when (taskInfo.task.priority) {
                    com.example.taskmanagement.data.entities.TaskEntity.Priority.LOW -> "Низкий"
                    com.example.taskmanagement.data.entities.TaskEntity.Priority.MEDIUM -> "Средний"
                    com.example.taskmanagement.data.entities.TaskEntity.Priority.HIGH -> "Высокий"
                }
                tvPriority.text = "Приоритет: $priorityText"

                tvCategory.text = "Категория: ${taskInfo.category.name}"

                tvDueDate.text = if (taskInfo.task.dueDate != null) {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    "Срок: ${dateFormat.format(taskInfo.task.dueDate)}"
                } else {
                    "Срок: не установлен"
                }

                tvAssignedTo.text = "Назначено: ${taskInfo.assignedTo?.getFullName() ?: "Не назначено"}"
                tvCreatedBy.text = "Создано: ${taskInfo.createdBy.getFullName()}"

                cbCompleted.isChecked = taskInfo.task.isCompleted
            }
        }
    }

    private fun setupClickListeners() {
        cbCompleted.setOnCheckedChangeListener { _, isChecked ->
            val taskId = arguments?.getLong("taskId") ?: return@setOnCheckedChangeListener
            viewModel.updateTaskCompletion(taskId, isChecked)
        }

        btnEdit.setOnClickListener {
            // TODO: Навигация к редактированию задачи
        }

        btnDelete.setOnClickListener {
            // TODO: Удаление задачи
        }

        btnBack.setOnClickListener {
            requireActivity().onBackPressed()
        }
    }
}