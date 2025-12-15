package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.R
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class UserDetailFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvCreatedAt: TextView
    private lateinit var tvUserId: TextView
    private lateinit var btnEdit: Button
    private lateinit var btnDelete: Button
    private lateinit var btnBack: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_detail, container, false)

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvUsername = view.findViewById(R.id.tvUsername)
        tvRole = view.findViewById(R.id.tvRole)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvCreatedAt = view.findViewById(R.id.tvCreatedAt)
        tvUserId = view.findViewById(R.id.tvUserId)
        btnEdit = view.findViewById(R.id.btnEdit)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnBack = view.findViewById(R.id.btnBack)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadUser()
    }

    private fun loadUser() {
        val userId = arguments?.getLong("userId") ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            val user = viewModel.getUserById(userId)
            user?.let {
                tvName.text = it.getFullName()
                tvEmail.text = "Email: ${it.email}"
                tvUsername.text = "Имя пользователя: ${it.username}"

                val roleText = when (it.role) {
                    com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN -> "Администратор"
                    com.example.taskmanagement.data.entities.UserEntity.Role.USER -> "Пользователь"
                }
                tvRole.text = "Роль: $roleText"

                tvStatus.text = "Статус: ${if (it.isActive) "Активен" else "Неактивен"}"

                val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                tvCreatedAt.text = "Создан: ${dateFormat.format(it.createdAt)}"

                tvUserId.text = "ID: ${it.id}"

                val currentUser = viewModel.currentUser.value
                if (currentUser?.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                    btnEdit.visibility = View.VISIBLE
                    btnDelete.visibility = View.VISIBLE
                } else {
                    btnEdit.visibility = View.GONE
                    btnDelete.visibility = View.GONE
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnEdit.setOnClickListener {
            val userId = arguments?.getLong("userId") ?: return@setOnClickListener
            val bundle = Bundle().apply {
                putLong("userId", userId)
            }
            findNavController().navigate(R.id.action_userDetailFragment_to_userFormFragment, bundle)
        }

        btnDelete.setOnClickListener {
            val userId = arguments?.getLong("userId") ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                val user = viewModel.getUserById(userId)
                user?.let {
                    viewModel.deleteUser(it)
                    findNavController().navigateUp()
                }
            }
        }

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }
}