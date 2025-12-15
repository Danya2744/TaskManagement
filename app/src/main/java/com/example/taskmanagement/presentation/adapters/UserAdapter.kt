package com.example.taskmanagement.presentation.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.R
import com.example.taskmanagement.data.entities.UserEntity

class UserAdapter(
    private var showDeleteButtons: Boolean = false,
    private val onUserClicked: (UserEntity) -> Unit,
    private val onUserDeleted: (UserEntity) -> Unit
) : ListAdapter<UserEntity, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user)
    }

    fun setShowDeleteButtons(show: Boolean) {
        this.showDeleteButtons = show
        notifyDataSetChanged()
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        private val tvRole: TextView = itemView.findViewById(R.id.tvRole)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(user: UserEntity) {
            tvName.text = user.getFullName()
            tvEmail.text = user.email

            val roleText = when (user.role) {
                UserEntity.Role.ADMIN -> "Администратор"
                UserEntity.Role.USER -> "Пользователь"
            }
            tvRole.text = roleText

            btnDelete.visibility = if (showDeleteButtons) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onUserClicked(user)
            }

            btnDelete.setOnClickListener {
                onUserDeleted(user)
            }
        }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<UserEntity>() {
    override fun areItemsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: UserEntity, newItem: UserEntity): Boolean {
        return oldItem == newItem
    }
}