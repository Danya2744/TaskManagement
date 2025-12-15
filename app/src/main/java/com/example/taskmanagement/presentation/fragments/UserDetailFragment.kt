package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.UserEntity
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

    private var currentUserId: Long? = null
    private var viewedUserId: Long? = null

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        setupObservers()
        loadUser()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { currentUser ->
                currentUserId = currentUser?.id
                updateButtonsVisibility(currentUser)
            }
        }
    }

    private fun loadUser() {
        viewedUserId = arguments?.getLong("userId")

        if (viewedUserId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID пользователя не указан", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val user = viewModel.getUserById(viewedUserId!!)
            user?.let {
                updateUserInfo(it)
            } ?: run {
                Toast.makeText(requireContext(), "Пользователь не найден", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            }
        }
    }

    private fun updateUserInfo(user: UserEntity) {
        tvName.text = user.getFullName()
        tvEmail.text = "Email: ${user.email}"
        tvUsername.text = "Имя пользователя: ${user.username}"
        val roleText = when (user.role) {
            UserEntity.Role.ADMIN -> "Администратор"
            UserEntity.Role.USER -> "Пользователь"
        }
        tvRole.text = "Роль: $roleText"
        tvStatus.text = "Статус: ${if (user.isActive) "Активен" else "Неактивен"}"
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        tvCreatedAt.text = "Создан: ${dateFormat.format(user.createdAt)}"
        tvUserId.text = "ID: ${user.id}"
    }

    private fun updateButtonsVisibility(currentUser: UserEntity?) {
        if (currentUser?.role == UserEntity.Role.ADMIN) {
            val isSelf = viewedUserId == currentUserId
            btnEdit.visibility = View.VISIBLE
            btnDelete.visibility = if (!isSelf) View.VISIBLE else View.GONE
        } else {
            btnEdit.visibility = View.GONE
            btnDelete.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnEdit.setOnClickListener {
            viewedUserId?.let { userId ->
                val bundle = Bundle().apply {
                    putLong("userId", userId)
                }
                findNavController().navigate(R.id.action_userDetailFragment_to_userFormFragment, bundle)
            }
        }

        btnDelete.setOnClickListener {
            viewedUserId?.let { userId ->
                if (userId == currentUserId) {
                    Toast.makeText(requireContext(), "Вы не можете удалить свой собственный аккаунт", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                showDeleteConfirmation(userId)
            }
        }
    }

    private fun showDeleteConfirmation(userId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = viewModel.getUserById(userId)
            user?.let { userToDelete ->
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Удаление пользователя")
                    .setMessage("Вы уверены, что хотите удалить пользователя ${userToDelete.getFullName()}?")
                    .setPositiveButton("Удалить") { dialog, _ ->
                        deleteUser(userToDelete)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }
    }

    private fun deleteUser(user: UserEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                viewModel.deleteUser(user)
                Toast.makeText(requireContext(), "Пользователь удален", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка при удалении пользователя: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}