package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.R
import com.example.taskmanagement.presentation.adapters.TaskAdapter
import com.example.taskmanagement.presentation.viewmodels.TaskViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskListFragment : Fragment() {

    private val viewModel: TaskViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddTask: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_task_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewTasks)
        progressBar = view.findViewById(R.id.progressBar)
        fabAddTask = view.findViewById(R.id.fabAddTask)
        tvEmptyState = view.findViewById(R.id.tvEmptyState)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isCompleted ->
                viewModel.updateTaskCompletion(task.id, isCompleted)
            },
            onTaskClicked = { task ->
                // TODO: Навигация к деталям задачи
            }
        )

        recyclerView.apply {
            adapter = taskAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tasks.collect { tasks ->
                taskAdapter.submitList(tasks)
                updateEmptyState(tasks.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Loading -> {
                        showLoading(true)
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Success -> {
                        showLoading(false)
                    }
                    is com.example.taskmanagement.presentation.viewmodels.TaskUiState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            // TODO: Навигация к созданию задачи
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        // TODO: Показать ошибку через Snackbar
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}