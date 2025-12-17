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
    private lateinit var tvStatsDetails: TextView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var filterDropdown: AutoCompleteTextView
    private lateinit var filterLayout: com.google.android.material.textfield.TextInputLayout

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
        tvStatsDetails = view.findViewById(R.id.tvStatsDetails)
        filterDropdown = view.findViewById(R.id.filterDropdown)
        filterLayout = view.findViewById(R.id.filterLayout)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterDropdown()
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

    private fun setupFilterDropdown() {
        val filterItems = listOf(
            "Все задачи",
            "Выполненные",
            "Активные",
            "Высокий приоритет"
        )

        val filterAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            filterItems
        )

        filterDropdown.setAdapter(filterAdapter)

        filterDropdown.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.ALL)
                1 -> viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.COMPLETED)
                2 -> viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.PENDING)
                3 -> viewModel.setFilter(com.example.taskmanagement.presentation.viewmodels.TaskFilter.HIGH_PRIORITY)
            }

            filterDropdown.clearFocus()
            hideKeyboard()
        }

        filterDropdown.setOnClickListener {
            if (!filterDropdown.isPopupShowing) {
                filterDropdown.showDropDown()
            }
        }

        filterDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                filterDropdown.showDropDown()
            }
        }

        filterDropdown.setText(filterItems[0], false)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilter.collect { filter ->
                val selectedText = when (filter) {
                    com.example.taskmanagement.presentation.viewmodels.TaskFilter.ALL -> filterItems[0]
                    com.example.taskmanagement.presentation.viewmodels.TaskFilter.COMPLETED -> filterItems[1]
                    com.example.taskmanagement.presentation.viewmodels.TaskFilter.PENDING -> filterItems[2]
                    com.example.taskmanagement.presentation.viewmodels.TaskFilter.HIGH_PRIORITY -> filterItems[3]
                }
                if (filterDropdown.text.toString() != selectedText) {
                    filterDropdown.setText(selectedText, false)
                }
            }
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

                    else -> {}
                }
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
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val searchCard = view?.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSearch)
        val searchInputLayout = searchCard?.getChildAt(0)?.let {
            it as? LinearLayout
        }?.getChildAt(0) as? com.google.android.material.textfield.TextInputLayout

        searchInputLayout?.setEndIconOnClickListener {
            etSearch.text?.clear()
            viewModel.setSearchQuery("")
            hideKeyboard()
        }
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

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        fabAddTask.isEnabled = !show
        filterDropdown.isEnabled = !show
        etSearch.isEnabled = !show
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun hideKeyboard() {
        val inputMethodManager = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
                as android.view.inputmethod.InputMethodManager
        val currentFocus = activity?.currentFocus
        currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        filterDropdown.setOnItemClickListener(null)
        filterDropdown.setOnClickListener(null)
        filterDropdown.setOnFocusChangeListener(null)
    }
}
