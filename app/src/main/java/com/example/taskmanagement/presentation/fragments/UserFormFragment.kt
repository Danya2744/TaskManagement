package com.example.taskmanagement.presentation.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.domain.utils.PasswordManager
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UserFormFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()

    @Inject
    lateinit var passwordManager: PasswordManager

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvTitle: TextView
    private lateinit var layoutPassword: LinearLayout
    private lateinit var layoutConfirmPassword: LinearLayout

    private var userId: Long = -1
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_user_form, container, false)

        tvTitle = view.findViewById(R.id.tvTitle)
        etUsername = view.findViewById(R.id.etUsername)
        etEmail = view.findViewById(R.id.etEmail)
        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etPassword = view.findViewById(R.id.etPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        spinnerRole = view.findViewById(R.id.spinnerRole)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        layoutPassword = view.findViewById(R.id.layoutPassword)
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getLong("userId") ?: -1
        isEditMode = userId != -1L

        setupSpinner()
        setupClickListeners()
        setupObservers()

        if (isEditMode) {
            tvTitle.text = "Редактировать пользователя"
            loadUserData()
            layoutPassword.visibility = View.GONE
            layoutConfirmPassword.visibility = View.GONE
        } else {
            tvTitle.text = "Создать пользователя"
            layoutPassword.visibility = View.VISIBLE
            layoutConfirmPassword.visibility = View.VISIBLE
        }
    }

    private fun setupSpinner() {
        val roles = listOf(
            UserEntity.Role.USER to "Пользователь",
            UserEntity.Role.ADMIN to "Администратор"
        )
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roles.map { it.second }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = viewModel.getUserById(userId)
            user?.let {
                etUsername.setText(it.username)
                etEmail.setText(it.email)
                etFirstName.setText(it.firstName)
                etLastName.setText(it.lastName)

                val roleIndex = when (it.role) {
                    UserEntity.Role.USER -> 0
                    UserEntity.Role.ADMIN -> 1
                }
                spinnerRole.setSelection(roleIndex)
            }
        }
    }

    private fun setupClickListeners() {
        btnSave.setOnClickListener {
            if (isEditMode) {
                updateUser()
            } else {
                createUser()
            }
        }

        btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun createUser() {
        val username = etUsername.text.toString()
        val email = etEmail.text.toString()
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()
        val selectedRoleIndex = spinnerRole.selectedItemPosition

        if (username.isBlank()) {
            showError("Введите имя пользователя")
            return
        }

        if (email.isBlank()) {
            showError("Введите email")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Введите корректный email")
            return
        }

        if (firstName.isBlank()) {
            showError("Введите имя")
            return
        }

        if (lastName.isBlank()) {
            showError("Введите фамилию")
            return
        }

        if (password.length < 6) {
            showError("Пароль должен содержать минимум 6 символов")
            return
        }

        if (password != confirmPassword) {
            showError("Пароли не совпадают")
            return
        }

        val role = if (selectedRoleIndex == 0) UserEntity.Role.USER else UserEntity.Role.ADMIN

        val salt = passwordManager.generateSalt()
        val passwordHash = passwordManager.hashPassword(password, salt)

        val user = UserEntity(
            username = username,
            email = email,
            passwordHash = passwordHash,
            salt = salt,
            firstName = firstName,
            lastName = lastName,
            role = role,
            createdAt = Date(),
            updatedAt = Date()
        )

        viewModel.createUser(user)
    }

    private fun updateUser() {
        val username = etUsername.text.toString()
        val email = etEmail.text.toString()
        val firstName = etFirstName.text.toString()
        val lastName = etLastName.text.toString()
        val password = etPassword.text.toString()
        val selectedRoleIndex = spinnerRole.selectedItemPosition

        if (username.isBlank()) {
            showError("Введите имя пользователя")
            return
        }

        if (email.isBlank()) {
            showError("Введите email")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Введите корректный email")
            return
        }

        if (firstName.isBlank()) {
            showError("Введите имя")
            return
        }

        if (lastName.isBlank()) {
            showError("Введите фамилию")
            return
        }

        val role = if (selectedRoleIndex == 0) UserEntity.Role.USER else UserEntity.Role.ADMIN

        viewLifecycleOwner.lifecycleScope.launch {
            val existingUser = viewModel.getUserById(userId)
            existingUser?.let { user ->
                val updatedUser = user.copy(
                    username = username,
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    role = role,
                    updatedAt = Date()
                )

                if (password.isNotBlank()) {
                    if (password.length < 6) {
                        showError("Пароль должен содержать минимум 6 символов")
                        return@launch
                    }
                    val newSalt = passwordManager.generateSalt()
                    val newPasswordHash = passwordManager.hashPassword(password, newSalt)
                    updatedUser.copy(
                        passwordHash = newPasswordHash,
                        salt = newSalt
                    )
                }

                viewModel.updateUser(updatedUser)
            }
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is com.example.taskmanagement.presentation.viewmodels.AuthState.Loading -> {
                        showLoading(true)
                        clearError()
                    }
                    is com.example.taskmanagement.presentation.viewmodels.AuthState.Success -> {
                        showLoading(false)
                        findNavController().navigateUp()
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

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        btnSave.isEnabled = !show
        btnCancel.isEnabled = !show
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun clearError() {
        tvError.visibility = View.GONE
    }
}