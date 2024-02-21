package com.example.todochamp.data.repositories

import android.util.Log
import com.example.todochamp.common.COLLECTION_PATH_NAME
import com.example.todochamp.common.PLEASE_CHECK_INTERNET_CONNECTION
import com.example.todochamp.common.Result
import com.example.todochamp.common.convertDateFormat
import com.example.todochamp.common.getCurrentTimeAsString
import com.example.todochamp.data.model.Task
import com.example.todochamp.di.IoDispatcher
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class TaskRepositoryImpl @Inject constructor(
    private val todoChampDB: FirebaseFirestore,
    //coroutine is an instance of a suspendable computation. It is conceptually similar to a thread, in the sense that it takes a block of code to run that works concurrently with the rest of the code.
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TaskRepository {

    //"override" and "suspend" keywords, indicating that it is overriding a function and can suspend the execution of a coroutine(thread).
    override suspend fun addTask(title: String, body: String): Result<Unit> {
       // If an exception occurs, it logs the error and returns a failure Result with the exception
        return try {
            withContext(ioDispatcher) {
                val task = hashMapOf(
                    "title" to title,
                    "body" to body,
                    "createdAt" to getCurrentTimeAsString(),
                )

                //sets a timeout of 10 seconds using "withTimeoutOrNull" to restrict the duration for adding the task to a database
                // If the operation exceeds this time, it will return null.
                val addTaskTimeout = withTimeoutOrNull(10000L) {
                    todoChampDB.collection(COLLECTION_PATH_NAME)
                        .add(task)
                }

                //Based on the result of the timeout check, it either logs an error and returns a failure Result with an IllegalStateException,
                // indicating a timeout due to a potential lack of internet connection, or it returns a success Result.
                if (addTaskTimeout == null) {
                    Log.d("ERROR: ", PLEASE_CHECK_INTERNET_CONNECTION)

                    Result.Failure(IllegalStateException(PLEASE_CHECK_INTERNET_CONNECTION))
                }

                Result.Success(Unit)
            }
            // If an exception occurs, it logs the error and returns a failure Result with the exception
        } catch (exception: Exception) {
            Log.d("ERROR: ", "$exception")

            Result.Failure(exception = exception)
        }
    }

    override suspend fun getAllTasks(): Result<List<Task>> {
        return try {
            withContext(ioDispatcher) {
                val fetchingTasksTimeout = withTimeoutOrNull(10000L) {
                    // then fetches the tasks from a database collection
                    //and maps the resulting documents to a list of Task objects.
                    todoChampDB.collection(COLLECTION_PATH_NAME)
                        .get()
                        .await()
                        .documents.map { document ->
                            Task(
                                taskId = document.id,
                                title = document.getString("title") ?: "",
                                body = document.getString("body") ?: "",
                                createdAt = convertDateFormat(
                                    document.getString("createdAt") ?: "",
                                ),
                            )
                        }
                }

                if (fetchingTasksTimeout == null) {
                    Log.d("ERROR: ", PLEASE_CHECK_INTERNET_CONNECTION)

                    Result.Failure(IllegalStateException(PLEASE_CHECK_INTERNET_CONNECTION))
                }

                Log.d("TASKS: ", "${fetchingTasksTimeout?.toList()}")

                Result.Success(fetchingTasksTimeout?.toList() ?: emptyList())
            }
        } catch (exception: Exception) {
            Log.d("ERROR: ", "$exception")

            Result.Failure(exception = exception)
        }
    }

    override suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                val addTaskTimeout = withTimeoutOrNull(10000L) {
                    todoChampDB.collection(COLLECTION_PATH_NAME)
                        .document(taskId)
                        .delete()
                }

                if (addTaskTimeout == null) {
                    Log.d("ERROR: ", PLEASE_CHECK_INTERNET_CONNECTION)

                    Result.Failure(IllegalStateException(PLEASE_CHECK_INTERNET_CONNECTION))
                }

                Result.Success(Unit)
            }
        } catch (exception: Exception) {
            Log.d("ERROR: ", "$exception")

            Result.Failure(exception = exception)
        }
    }

    override suspend fun updateTask(title: String, body: String, taskId: String): Result<Unit> {
        return try {
            withContext(ioDispatcher) {
                val taskUpdate: Map<String, Any> = hashMapOf(
                    "title" to title,
                    "body" to body,
                )

                val addTaskTimeout = withTimeoutOrNull(10000L) {
                    todoChampDB.collection(COLLECTION_PATH_NAME)
                        .document(taskId)
                        .update(taskUpdate)
                }

                if (addTaskTimeout == null) {
                    Log.d("ERROR: ", PLEASE_CHECK_INTERNET_CONNECTION)

                    Result.Failure(IllegalStateException(PLEASE_CHECK_INTERNET_CONNECTION))
                }

                Result.Success(Unit)
            }
        } catch (exception: Exception) {
            Log.d("ERROR: ", "$exception")

            Result.Failure(exception = exception)
        }
    }
}
