package com.example.taskmanagement.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.TaskEntity
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onTaskChecked: (TaskEntity, Boolean) -> Unit,
    private val onTaskClicked: (TaskEntity) -> Unit
) : ListAdapter<TaskEntity, TaskAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = getItem(position)
        holder.bind(task)
    }

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)

        fun bind(task: TaskEntity) {
            tvTitle.text = task.title
            tvDescription.text = task.description ?: ""

            val priorityText = when (task.priority) {
                TaskEntity.Priority.LOW -> "Низкий"
                TaskEntity.Priority.MEDIUM -> "Средний"
                TaskEntity.Priority.HIGH -> "Высокий"
            }
            tvPriority.text = priorityText

            tvDueDate.text = if (task.dueDate != null) {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(task.dueDate)
            } else {
                "Без срока"
            }

            cbCompleted.isChecked = task.isCompleted

            // Обработчики кликов
            cbCompleted.setOnCheckedChangeListener { _, isChecked ->
                onTaskChecked(task, isChecked)
            }

            itemView.setOnClickListener {
                onTaskClicked(task)
            }
        }
    }
}

class TaskDiffCallback : DiffUtil.ItemCallback<TaskEntity>() {
    override fun areItemsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: TaskEntity, newItem: TaskEntity): Boolean {
        return oldItem == newItem
    }
}