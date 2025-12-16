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
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.UserEntity
import com.example.taskmanagement.domain.utils.PasswordManager
import com.example.taskmanagement.domain.utils.PasswordStrength
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
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvError: TextView
    private lateinit var tvTitle: TextView
    private lateinit var layoutCurrentPassword: LinearLayout
    private lateinit var layoutNewPassword: LinearLayout
    private lateinit var layoutConfirmPassword: LinearLayout
    private lateinit var tvPasswordTitle: TextView
    private lateinit var tvCurrentPasswordLabel: TextView
    private lateinit var tvNewPasswordLabel: TextView
    private lateinit var tvConfirmPasswordLabel: TextView
    private lateinit var tvRoleLabel: TextView

    private var userId: Long = -1
    private var isEditMode = false
    private var currentUserIsAdmin = false
    private var editingSelf = false

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
        layoutNewPassword = view.findViewById(R.id.layoutNewPassword)
        layoutConfirmPassword = view.findViewById(R.id.layoutConfirmPassword)
        tvPasswordTitle = view.findViewById(R.id.tvPasswordTitle)
        tvCurrentPasswordLabel = view.findViewById(R.id.tvCurrentPasswordLabel)
        tvNewPasswordLabel = view.findViewById(R.id.tvNewPasswordLabel)
        tvConfirmPasswordLabel = view.findViewById(R.id.tvConfirmPasswordLabel)
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
        loadCurrentUserInfo()

        val parent = tvTitle.parent as? LinearLayout
        parent?.let {
            val layoutParams = tvTitle.layoutParams as LinearLayout.LayoutParams
            layoutParams.gravity = android.view.Gravity.CENTER_HORIZONTAL
            tvTitle.layoutParams = layoutParams
        }

        if (isEditMode) {
            tvTitle.text = "Редактирование"
            loadUserData()
            setupPasswordFieldsForEdit()
        } else {
            tvTitle.text = "Создание пользователя"
            setupPasswordFieldsForCreate()
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

    private fun loadCurrentUserInfo() {
        viewLifecycleOwner.lifecycleScope.launch {
            val currentUser = viewModel.currentUser.value
            currentUserIsAdmin = currentUser?.role == UserEntity.Role.ADMIN
            editingSelf = currentUser?.id == userId

            if (!currentUserIsAdmin) {
                spinnerRole.visibility = View.GONE
                tvRoleLabel.visibility = View.GONE
            }
        }
    }

    private fun setupPasswordFieldsForCreate() {
        layoutCurrentPassword.visibility = View.GONE
        layoutNewPassword.visibility = View.VISIBLE
        layoutConfirmPassword.visibility = View.VISIBLE
        tvPasswordTitle.visibility = View.VISIBLE
        tvCurrentPasswordLabel.visibility = View.GONE
        tvNewPasswordLabel.visibility = View.VISIBLE
        tvConfirmPasswordLabel.visibility = View.VISIBLE

        tvPasswordTitle.text = "Пароль"
        tvNewPasswordLabel.text = "Пароль*"
        tvConfirmPasswordLabel.text = "Подтверждение пароля*"

        etNewPassword.hint = ""
        etConfirmPassword.hint = ""
    }

    private fun setupPasswordFieldsForEdit() {
        layoutCurrentPassword.visibility = View.GONE
        layoutNewPassword.visibility = View.VISIBLE
        layoutConfirmPassword.visibility = View.VISIBLE
        tvPasswordTitle.visibility = View.VISIBLE
        tvCurrentPasswordLabel.visibility = View.GONE
        tvNewPasswordLabel.visibility = View.VISIBLE
        tvConfirmPasswordLabel.visibility = View.VISIBLE

        tvPasswordTitle.text = "Сменить пароль (необязательно)"
        tvNewPasswordLabel.text = "Новый пароль"
        tvConfirmPasswordLabel.text = "Подтверждение нового пароля"

        etCurrentPassword.hint = ""
        etNewPassword.hint = ""
        etConfirmPassword.hint = ""

        if (editingSelf) {
            layoutCurrentPassword.visibility = View.VISIBLE
            tvCurrentPasswordLabel.visibility = View.VISIBLE
            tvCurrentPasswordLabel.text = "Текущий пароль*"
        }

        if (currentUserIsAdmin && !editingSelf) {
            tvNewPasswordLabel.text = "Новый пароль (необязательно)"
            tvConfirmPasswordLabel.text = "Подтверждение нового пароля"
        }
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

        val role = if (selectedRoleIndex == 0) UserEntity.Role.USER else UserEntity.Role.ADMIN

        viewLifecycleOwner.lifecycleScope.launch {
            val existingUser = viewModel.getUserById(userId)
            existingUser?.let { user ->
                val isChangingPassword = newPassword.isNotBlank() && confirmPassword.isNotBlank()

                if (isChangingPassword) {
                    if (currentUserIsAdmin && !editingSelf) {
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
                            role = role,
                            passwordHash = newPasswordHash,
                            salt = newSalt,
                            updatedAt = Date()
                        )

                        viewModel.updateUser(updatedUser)
                    } else {
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
                            role = role,
                            passwordHash = newPasswordHash,
                            salt = newSalt,
                            updatedAt = Date()
                        )

                        viewModel.updateUser(updatedUser)
                    }
                } else {
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
