package com.example.taskmanagement.data.database

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.taskmanagement.data.dao.CategoryDao
import com.example.taskmanagement.data.dao.TaskDao
import com.example.taskmanagement.data.dao.UserDao
import com.example.taskmanagement.data.entities.CategoryEntity
import com.example.taskmanagement.data.entities.TaskEntity
import com.example.taskmanagement.data.entities.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.example.taskmanagement.domain.utils.PasswordManager

@Database(
    entities = [TaskEntity::class, CategoryEntity::class, UserEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskmanagement_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).prepopulateData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private suspend fun prepopulateData() {
        val defaultCategories = listOf(
            CategoryEntity(name = "Работа", color = android.graphics.Color.BLUE, createdAt = Date()),
            CategoryEntity(name = "Личное", color = android.graphics.Color.GREEN, createdAt = Date()),
            CategoryEntity(name = "Срочные", color = android.graphics.Color.RED, createdAt = Date()),
            CategoryEntity(name = "Планирование", color = android.graphics.Color.YELLOW, createdAt = Date())
        )
        defaultCategories.forEach { categoryDao().insertCategory(it) }

        val passwordManager = PasswordManager()

        val adminSalt = passwordManager.generateSalt()
        val adminHash = passwordManager.hashPassword("admin123", adminSalt)
        val admin = UserEntity(
            username = "admin",
            email = "admin@company.com",
            passwordHash = adminHash,
            salt = adminSalt,
            firstName = "Администратор",
            lastName = "Системы",
            role = UserEntity.Role.ADMIN,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao().insertUser(admin)

        val userSalt = passwordManager.generateSalt()
        val userHash = passwordManager.hashPassword("user123", userSalt)
        val user = UserEntity(
            username = "user1",
            email = "user1@company.com",
            passwordHash = userHash,
            salt = userSalt,
            firstName = "Иван",
            lastName = "Иванов",
            role = UserEntity.Role.USER,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao().insertUser(user)
    }
}