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
import com.example.taskmanagement.presentation.adapters.UserAdapter
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserListFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddUser: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var userAdapter: UserAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_list, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewUsers)
        progressBar = view.findViewById(R.id.progressBar)
        fabAddUser = view.findViewById(R.id.fabAddUser)
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
        userAdapter = UserAdapter(
            onUserClicked = { user ->
                // TODO: Навигация к деталям пользователя
            },
            onUserDeleted = { user ->
                // TODO: Удаление пользователя (только для админа)
                if (viewModel.currentUser.value?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                    viewModel.deleteUser(user)
                }
            }
        )

        recyclerView.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.users.collect { users ->
                userAdapter.submitList(users)
                updateEmptyState(users.isEmpty())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { currentUser ->
                // Скрываем FAB если текущий пользователь не админ
                fabAddUser.visibility = if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is com.example.taskmanagement.presentation.viewmodels.AuthState.Loading -> {
                        showLoading(true)
                    }
                    is com.example.taskmanagement.presentation.viewmodels.AuthState.Success -> {
                        showLoading(false)
                    }
                    is com.example.taskmanagement.presentation.viewmodels.AuthState.Error -> {
                        showLoading(false)
                        showError(state.message)
                    }
                    else -> showLoading(false)
                }
            }
        }
    }

    private fun setupClickListeners() {
        fabAddUser.setOnClickListener {
            // TODO: Навигация к созданию пользователя
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