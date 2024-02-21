package com.example.todochamp.data.repositories

import com.example.todochamp.common.Result
import com.example.todochamp.data.model.Task

interface TaskRepository {
    suspend fun addTask(title: String, body: String): Result<Unit>

    suspend fun getAllTasks(): Result<List<Task>>

    suspend fun deleteTask(taskId: String): Result<Unit>

    suspend fun updateTask(title: String, body: String, taskId: String): Result<Unit>
}
