package com.example.taskmanagement.presentation.fragments

import android.graphics.Color
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
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.domain.repository.UserRepository
import com.example.taskmanagement.domain.utils.PasswordManager
import com.example.taskmanagement.domain.utils.PasswordStrength
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class UserFormFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()

    @Inject
    lateinit var passwordManager: PasswordManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvTitle: TextView
    private lateinit var layoutCurrentPassword: TextInputLayout
    private lateinit var layoutNewPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var tvPasswordTitle: TextView
    private lateinit var tvCurrentPasswordLabel: TextView
    private lateinit var tvNewPasswordLabel: TextView
    private lateinit var tvConfirmPasswordLabel: TextView
    private lateinit var tvRoleLabel: TextView

    private var userId: Long = -1
    private var isEditMode = false
    private var currentUserIsAdmin = false
    private var editingSelf = false
    private var currentUserId: Long? = null

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
        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)
        spinnerRole = view.findViewById(R.id.spinnerRole)
        btnSave = view.findViewById(R.id.btnSave)
        btnCancel = view.findViewById(R.id.btnCancel)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        layoutCurrentPassword = view.findViewById(R.id.layoutCurrentPassword)
        tvCurrentPasswordLabel = view.findViewById(R.id.tvCurrentPasswordLabel)
        tvNewPasswordLabel = view.findViewById(R.id.tvNewPasswordLabel)
        tvConfirmPasswordLabel = view.findViewById(R.id.tvConfirmPasswordLabel)
        layoutNewPassword = view.findViewById(R.id.layoutNewPassword)
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword)
        tvPasswordTitle = view.findViewById(R.id.tvPasswordTitle)
        tvRoleLabel = view.findViewById(R.id.tvRoleLabel)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userId = arguments?.getLong("userId") ?: -1
        isEditMode = userId != -1L

        setupSpinner()
        setupClickListeners()
        setupObservers()
        setupTextWatchers()

        val parent = tvTitle.parent as? LinearLayout
        parent?.let {
            val layoutParams = tvTitle.layoutParams as LinearLayout.LayoutParams
            layoutParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
            tvTitle.layoutParams = layoutParams
        }

        viewLifecycleOwner.lifecycleScope.launch {
            loadCurrentUserInfoAndSetupUI()
        }
    }

    private suspend fun loadCurrentUserInfoAndSetupUI() {
        try {
            val currentUser = userRepository.getCurrentUser()
            currentUserId = currentUser?.id
            currentUserIsAdmin = currentUser?.role == UserEntity.Role.ADMIN
            editingSelf = currentUserId == userId

            requireActivity().runOnUiThread {
                if (!currentUserIsAdmin) {
                    spinnerRole.visibility = View.GONE
                    tvRoleLabel.visibility = View.GONE
                }

                if (isEditMode) {
                    tvTitle.text = "Изменение"
                    setupPasswordFieldsForEdit()
                    loadUserData()
                } else {
                    tvTitle.text = "Создание"
                    setupPasswordFieldsForCreate()
                }
            }
        } catch (e: Exception) {
        }
    }

    private fun setupSpinner() {
        val roles = listOf(
            UserEntity.Role.USER to "Пользователь",
            UserEntity.Role.ADMIN to "Администратор"
        )

        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            roles.map { it.second }
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent)
                (view as TextView).setTextColor(Color.BLACK)
                view.setBackgroundColor(Color.WHITE)
                return view
            }
        }

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
    }

    private fun setupPasswordFieldsForCreate() {
        layoutCurrentPassword.visibility = View.GONE
        tvCurrentPasswordLabel.visibility = View.GONE
        layoutNewPassword.visibility = View.VISIBLE
        layoutConfirmPassword.visibility = View.VISIBLE
        tvPasswordTitle.visibility = View.VISIBLE

        tvPasswordTitle.text = "Пароль"
        tvNewPasswordLabel.text = "Пароль*"
        tvConfirmPasswordLabel.text = "Подтверждение пароля*"

        etNewPassword.hint = ""
        etConfirmPassword.hint = ""
    }

    private fun setupPasswordFieldsForEdit() {
        layoutNewPassword.visibility = View.VISIBLE
        layoutConfirmPassword.visibility = View.VISIBLE
        tvPasswordTitle.visibility = View.VISIBLE

        tvPasswordTitle.text = "Сменить пароль (необязательно)"
        tvNewPasswordLabel.text = "Новый пароль"
        tvConfirmPasswordLabel.text = "Подтверждение нового пароля"

        if (editingSelf && !currentUserIsAdmin) {
            layoutCurrentPassword.visibility = View.VISIBLE
            tvCurrentPasswordLabel.visibility = View.VISIBLE
            tvCurrentPasswordLabel.text = "Текущий пароль*"
            etCurrentPassword.isEnabled = true
        } else {
            layoutCurrentPassword.visibility = View.GONE
            tvCurrentPasswordLabel.visibility = View.GONE
            etCurrentPassword.isEnabled = false
        }

        if (currentUserIsAdmin && !editingSelf) {
            tvNewPasswordLabel.text = "Новый пароль (необязательно)"
            tvConfirmPasswordLabel.text = "Подтверждение нового пароля"
            layoutCurrentPassword.visibility = View.GONE
            tvCurrentPasswordLabel.visibility = View.GONE
            etCurrentPassword.isEnabled = false
        }
    }

    private fun loadUserData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val user = viewModel.getUserById(userId)
                user?.let {
                    requireActivity().runOnUiThread {
                        etUsername.setText(it.username)
                        etEmail.setText(it.email)
                        etFirstName.setText(it.firstName)
                        etLastName.setText(it.lastName)

                        if (currentUserIsAdmin) {
                            val roleIndex = when (it.role) {
                                UserEntity.Role.USER -> 0
                                UserEntity.Role.ADMIN -> 1
                            }
                            spinnerRole.setSelection(roleIndex)
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearError()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        val nameTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                clearError()
                validateNameField(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        etUsername.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etFirstName.addTextChangedListener(nameTextWatcher)
        etLastName.addTextChangedListener(nameTextWatcher)
        etCurrentPassword.addTextChangedListener(textWatcher)
        etNewPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)
    }

    private fun validateNameField(text: String): Boolean {
        if (text.isBlank()) return true

        val pattern = Regex("^[A-Za-zА-Яа-яЁё\\s]{3,}\$")
        val isValid = pattern.matches(text)

        if (!isValid && text.isNotBlank()) {
            showError("Имя и фамилия должны содержать только буквы и быть не менее 3 символов")
            return false
        }

        return true
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
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val password = etNewPassword.text.toString()
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

        if (!validateNameField(firstName)) {
            return
        }

        if (!validateNameField(lastName)) {
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

        val passwordStrength = passwordManager.isPasswordStrong(password)
        if (passwordStrength == PasswordStrength.WEAK) {
            showError("Пароль слишком слабый. Используйте цифры, заглавные и строчные буквы")
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
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val currentPassword = etCurrentPassword.text.toString()
        val newPassword = etNewPassword.text.toString()
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

        if (!validateNameField(firstName)) {
            return
        }

        if (!validateNameField(lastName)) {
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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val existingUser = viewModel.getUserById(userId)
                existingUser?.let { user ->
                    val isChangingPassword = newPassword.isNotBlank() && confirmPassword.isNotBlank()

                    if (isChangingPassword) {
                        if (!currentUserIsAdmin && editingSelf) {
                            if (currentPassword.isBlank()) {
                                showError("Введите текущий пароль")
                                return@launch
                            }

                            if (!passwordManager.verifyPassword(currentPassword, user.salt, user.passwordHash)) {
                                showError("Текущий пароль неверен")
                                return@launch
                            }

                            if (newPassword.length < 6) {
                                showError("Новый пароль должен содержать минимум 6 символов")
                                return@launch
                            }

                            val passwordStrength = passwordManager.isPasswordStrong(newPassword)
                            if (passwordStrength == PasswordStrength.WEAK) {
                                showError("Новый пароль слишком слабый. Используйте цифры, заглавные и строчные буквы")
                                return@launch
                            }

                            if (newPassword != confirmPassword) {
                                showError("Новые пароли не совпадают")
                                return@launch
                            }

                            val newSalt = passwordManager.generateSalt()
                            val newPasswordHash = passwordManager.hashPassword(newPassword, newSalt)

                            val updatedUser = user.copy(
                                username = username,
                                email = email,
                                firstName = firstName,
                                lastName = lastName,
                                role = user.role,
                                passwordHash = newPasswordHash,
                                salt = newSalt,
                                updatedAt = Date()
                            )

                            viewModel.updateUser(updatedUser)
                        }
                        else if (currentUserIsAdmin) {
                            if (newPassword.length < 6) {
                                showError("Новый пароль должен содержать минимум 6 символов")
                                return@launch
                            }

                            val passwordStrength = passwordManager.isPasswordStrong(newPassword)
                            if (passwordStrength == PasswordStrength.WEAK) {
                                showError("Новый пароль слишком слабый. Используйте цифры, заглавные и строчные буквы")
                                return@launch
                            }

                            if (newPassword != confirmPassword) {
                                showError("Новые пароли не совпадают")
                                return@launch
                            }

                            val newSalt = passwordManager.generateSalt()
                            val newPasswordHash = passwordManager.hashPassword(newPassword, newSalt)

                            val role = if (selectedRoleIndex == 0) UserEntity.Role.USER else UserEntity.Role.ADMIN

                            val updatedUser = user.copy(
                                username = username,
                                email = email,
                                firstName = firstName,
                                lastName = lastName,
                                role = role,
                                passwordHash = newPasswordHash,
                                salt = newSalt,
                                updatedAt = Date()
                            )

                            viewModel.updateUser(updatedUser)
                        }
                    } else {
                        val role = if (currentUserIsAdmin) {
                            if (selectedRoleIndex == 0) UserEntity.Role.USER else UserEntity.Role.ADMIN
                        } else {
                            user.role
                        }

                        val updatedUser = user.copy(
                            username = username,
                            email = email,
                            firstName = firstName,
                            lastName = lastName,
                            role = role,
                            updatedAt = Date()
                        )

                        viewModel.updateUser(updatedUser)
                    }
                } ?: run {
                    showError("Пользователь не найден")
                }
            } catch (e: Exception) {
                showError("Ошибка при обновлении пользователя: ${e.message}")
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