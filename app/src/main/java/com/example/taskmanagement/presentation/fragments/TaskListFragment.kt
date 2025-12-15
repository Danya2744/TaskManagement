package com.example.taskmanagement.presentation.fragments

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
    private lateinit var etSearch: EditText
    private lateinit var btnFilterAll: Button
    private lateinit var btnFilterCompleted: Button
    private lateinit var btnFilterPending: Button
    private lateinit var btnFilterHigh: Button
    private lateinit var tvStatsDetails: TextView
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
        etSearch = view.findViewById(R.id.etSearch)
        btnFilterAll = view.findViewById(R.id.btnFilterAll)
        btnFilterCompleted = view.findViewById(R.id.btnFilterCompleted)
        btnFilterPending = view.findViewById(R.id.btnFilterPending)
        btnFilterHigh = view.findViewById(R.id.btnFilterHigh)
        tvStatsDetails = view.findViewById(R.id.tvStatsDetails)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        setupSearch()
        observeStatistics()
    }

    private fun setupRecyclerView() {
        taskAdapter = TaskAdapter(
            onTaskChecked = { task, isCompleted ->
                viewModel.updateTaskCompletion(task.id, isCompleted)
            },
            onTaskClicked = { task ->
                val bundle = Bundle().apply {
                    putLong("taskId", task.id)
                }
                findNavController().navigate(
                    R.id.action_taskListFragment_to_taskDetailFragment,
                    bundle
                )
            }
        )

        recyclerView.apply {
            adapter = taskAdapter
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilter.collect { filter ->
                updateFilterButtons(filter)
            }
        }
    }

    private fun setupClickListeners() {
        fabAddTask.setOnClickListener {
            val bundle = Bundle().apply {
                putLong("taskId", -1)
            }
            findNavController().navigate(R.id.action_taskListFragment_to_taskFormFragment, bundle)
        }

        btnFilterAll.setOnClickListener {
            viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.ALL)
        }

        btnFilterCompleted.setOnClickListener {
            viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.COMPLETED)
        }

        btnFilterPending.setOnClickListener {
            viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.PENDING)
        }

        btnFilterHigh.setOnClickListener {
            viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.HIGH_PRIORITY)
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun observeStatistics() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getStatistics().collect { statistics ->
                tvStatsDetails.text = "Всего: ${statistics.totalTasks} | " +
                        "Выполнено: ${statistics.completedTasks} | " +
                        "Прогресс: ${"%.1f".format(statistics.completionRate)}%"
            }
        }
    }

    private fun updateFilterButtons(currentFilter: com.example.taskmanagement.presentation.viewmodels.TaskFilter) {
        val allButtons = listOf(btnFilterAll, btnFilterCompleted, btnFilterPending, btnFilterHigh)

        allButtons.forEach { button ->
            button.isSelected = false
            button.setBackgroundResource(android.R.drawable.btn_default)
        }

        when (currentFilter) {
            com.example.taskmanagement.presentation.viewmodels.TaskFilter.ALL -> {
                btnFilterAll.isSelected = true
                btnFilterAll.setBackgroundResource(R.drawable.bg_filter_selected)
            }
            com.example.taskmanagement.presentation.viewmodels.TaskFilter.COMPLETED -> {
                btnFilterCompleted.isSelected = true
                btnFilterCompleted.setBackgroundResource(R.drawable.bg_filter_selected)
            }
            com.example.taskmanagement.presentation.viewmodels.TaskFilter.PENDING -> {
                btnFilterPending.isSelected = true
                btnFilterPending.setBackgroundResource(R.drawable.bg_filter_selected)
            }
            com.example.taskmanagement.presentation.viewmodels.TaskFilter.HIGH_PRIORITY -> {
                btnFilterHigh.isSelected = true
                btnFilterHigh.setBackgroundResource(R.drawable.bg_filter_selected)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}