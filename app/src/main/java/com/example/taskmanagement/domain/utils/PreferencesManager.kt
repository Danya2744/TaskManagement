package com.example.taskmanagement.domain.utils

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferencesManager @Inject constructor(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("task_management_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
    }

    fun saveCurrentUserId(userId: Long) {
        sharedPreferences.edit().putLong(KEY_CURRENT_USER_ID, userId).apply()
    }

    fun getCurrentUserId(): Long? {
        return if (sharedPreferences.contains(KEY_CURRENT_USER_ID)) {
            sharedPreferences.getLong(KEY_CURRENT_USER_ID, -1)
        } else {
            null
        }.takeIf { it != null && it != -1L }
    }

    fun clearCurrentUser() {
        sharedPreferences.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
}