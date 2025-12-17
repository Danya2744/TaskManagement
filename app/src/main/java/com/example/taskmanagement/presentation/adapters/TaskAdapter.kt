package com.example.taskmanagement.presentation.adapters

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
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
        private val tvPriority: TextView = itemView.findViewById(R.id.tvPriority)
        private val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        private val cbCompleted: CheckBox = itemView.findViewById(R.id.cbCompleted)
        private val layoutPriority: LinearLayout = itemView.findViewById(R.id.layoutPriority)
        private val cardTask: CardView = itemView.findViewById(R.id.cardTask)

        fun bind(task: TaskEntity) {
            tvTitle.text = task.title

            val priorityText = when (task.priority) {
                TaskEntity.Priority.LOW -> "Низкий"
                TaskEntity.Priority.MEDIUM -> "Средний"
                TaskEntity.Priority.HIGH -> "Высокий"
            }
            tvPriority.text = priorityText

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val dueDateText = if (task.dueDate != null) {
                val date = dateFormat.format(task.dueDate)
                val time = timeFormat.format(task.dueDate)
                "Срок до $date, $time"
            } else {
                "Без срока"
            }
            tvDueDate.text = dueDateText

            cbCompleted.isChecked = task.isCompleted

            val isOverdue = if (task.dueDate != null) {
                task.dueDate.before(Date()) && !task.isCompleted
            } else {
                false
            }

            cardTask.alpha = 1.0f
            tvTitle.paintFlags = tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            tvDueDate.paintFlags = tvDueDate.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            val priorityColor = when (task.priority) {
                TaskEntity.Priority.LOW -> Color.parseColor("#66BB6A")
                TaskEntity.Priority.MEDIUM -> Color.parseColor("#FFA726")
                TaskEntity.Priority.HIGH -> Color.parseColor("#EF5350")
            }
            layoutPriority.setBackgroundColor(priorityColor)

            when {
                isOverdue -> {
                    cardTask.setCardBackgroundColor(Color.parseColor("#546E7A"))

                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvDueDate.paintFlags = tvDueDate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    if (task.dueDate != null) {
                        val date = dateFormat.format(task.dueDate)
                        val time = timeFormat.format(task.dueDate)
                        tvDueDate.text = "⏰ Просрочено! ($date, $time)"
                    } else {
                        tvDueDate.text = "⏰ Просрочено!"
                    }
                }

                task.isCompleted -> {
                    cardTask.setCardBackgroundColor(Color.parseColor("#78909C"))

                    cardTask.alpha = 0.7f

                    tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    tvDueDate.paintFlags = tvDueDate.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

                    if (task.dueDate != null) {
                        val date = dateFormat.format(task.dueDate)
                        val time = timeFormat.format(task.dueDate)
                        tvDueDate.text = "✓ Выполнено ($date, $time)"
                    } else {
                        tvDueDate.text = "✓ Выполнено"
                    }
                }

                else -> {
                    val cardColor = when (task.priority) {
                        TaskEntity.Priority.LOW -> Color.parseColor("#81D4FA")
                        TaskEntity.Priority.MEDIUM -> Color.parseColor("#FFCC80")
                        TaskEntity.Priority.HIGH -> Color.parseColor("#FF8A80")
                    }
                    cardTask.setCardBackgroundColor(cardColor)
                }
            }

            cbCompleted.setOnCheckedChangeListener(null) // Убираем старый listener
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