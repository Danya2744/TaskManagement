package com.example.taskmanagement.di

import com.example.taskmanagement.data.repository.TaskRepositoryImpl
import com.example.taskmanagement.data.repository.UserRepositoryImpl
import com.example.taskmanagement.domain.repository.TaskRepository
import com.example.taskmanagement.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideTaskRepository(impl: TaskRepositoryImpl): TaskRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideUserRepository(impl: UserRepositoryImpl): UserRepository {
        return impl
    }
}