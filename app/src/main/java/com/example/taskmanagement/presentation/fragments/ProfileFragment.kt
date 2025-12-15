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
import com.example.taskmanagement.R
import com.example.taskmanagement.presentation.viewmodels.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private val viewModel: UserViewModel by viewModels()
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvUserId: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnEditProfile: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvRole = view.findViewById(R.id.tvRole)
        tvUserId = view.findViewById(R.id.tvUserId)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnEditProfile = view.findViewById(R.id.btnEditProfile)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentUser.collect { user ->
                user?.let {
                    tvName.text = it.getFullName()
                    tvEmail.text = it.email

                    val roleText = when (it.role) {
                        com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN -> "Администратор"
                        com.example.taskmanagement.data.entities.UserEntity.Role.USER -> "Пользователь"
                    }
                    tvRole.text = roleText
                    tvUserId.text = "ID: ${it.id}"

                    btnEditProfile.visibility = if (it.role == com.example.taskmanagement.data.entities.UserEntity.Role.ADMIN) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            viewModel.logout()
            requireActivity().finish()
        }

        btnEditProfile.setOnClickListener {
            // TODO: Навигация к редактированию профиля
        }
    }
}