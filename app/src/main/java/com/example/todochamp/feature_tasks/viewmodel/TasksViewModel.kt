package com.example.todochamp.feature_tasks.viewmodel // ktlint-disable package-name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.todochamp.common.Result
import com.example.todochamp.data.model.Task
import com.example.todochamp.data.repositories.TaskRepository
import com.example.todochamp.feature_tasks.events.TasksScreenUiEvent
import com.example.todochamp.feature_tasks.side_effects.TaskScreenSideEffects
import com.example.todochamp.feature_tasks.state.TasksScreenUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//The TasksViewModel interacts with the UI and data layer in the following ways:

//UI Interaction:
//The ViewModel receives UI events triggered by user interactions, such as adding a task, updating a task, deleting a task, and changing dialog states. These events are received through the sendEvent function, which is called from the UI layer to convey user actions.
//The ViewModel updates its internal state in response to UI events and changes in the UI state. It uses functions such as setState and setEffect to manage the state and emit side effects to the UI layer using coroutines and flows.
//It exposes the UI state using a StateFlow, allowing the UI layer to observe and react to changes in the task-related data and UI state, such as task list updates, loading indicators, and dialog visibility.

//Data Layer Interaction:
//The ViewModel interacts with the data layer, specifically the TaskRepository, to perform operations related to tasks, such as adding, updating, and deleting tasks, as well as retrieving the list of tasks.
//When handling UI events related to task operations, the ViewModel calls functions from the TaskRepository to execute the corresponding data operations asynchronously using coroutines. For example, when adding a task, the ViewModel calls the addTask function of the TaskRepository.
//The ViewModel processes the results of data operations, such as successful task additions or errors encountered during data operations, and updates the UI state and emits relevant side effects to the UI layer based on the outcomes of these data operations.


@HiltViewModel
class TasksViewModel @Inject constructor(private val taskRepository: TaskRepository) : ViewModel() {
// coroutines and Flow from Kotlin's coroutines library to handle asynchronous operations
// and flow of data. It uses viewModelScope to launch coroutines, and it uses MutableStateFlow
// and Channel to manage and emit state updates and side effects.

    private val _state: MutableStateFlow<TasksScreenUiState> =
        MutableStateFlow(TasksScreenUiState())
    val state: StateFlow<TasksScreenUiState> = _state.asStateFlow()

    private val _effect: Channel<TaskScreenSideEffects> = Channel()
    val effect = _effect.receiveAsFlow()

    //The init block of the ViewModel sends an initial event (GetTasks) upon initialization.
    // Events related to user interactions or changes in the UI are sent to the ViewModel
    // using the sendEvent function.
    init {
        sendEvent(TasksScreenUiEvent.GetTasks)
    }

    fun sendEvent(event: TasksScreenUiEvent) {
        reduce(oldState = _state.value, event = event)
    }

    private fun setEffect(builder: () -> TaskScreenSideEffects) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }

    private fun setState(newState: TasksScreenUiState) {
        _state.value = newState
    }

    private fun reduce(oldState: TasksScreenUiState, event: TasksScreenUiEvent) {
        when (event) {
            is TasksScreenUiEvent.AddTask -> {
                addTask(oldState = oldState, title = event.title, body = event.body)
            }

            is TasksScreenUiEvent.DeleteNote -> {
                deleteNote(oldState = oldState, taskId = event.taskId)
            }

            TasksScreenUiEvent.GetTasks -> {
                getTasks(oldState = oldState)
            }

            is TasksScreenUiEvent.OnChangeAddTaskDialogState -> {
                onChangeAddTaskDialog(oldState = oldState, isShown = event.show)
            }

            is TasksScreenUiEvent.OnChangeUpdateTaskDialogState -> {
                onUpdateAddTaskDialog(oldState = oldState, isShown = event.show)
            }

            is TasksScreenUiEvent.OnChangeTaskBody -> {
                onChangeTaskBody(oldState = oldState, body = event.body)
            }

            is TasksScreenUiEvent.OnChangeTaskTitle -> {
                onChangeTaskTitle(oldState = oldState, title = event.title)
            }

            is TasksScreenUiEvent.SetTaskToBeUpdated -> {
                setTaskToBeUpdated(oldState = oldState, task = event.taskToBeUpdated)
            }

            TasksScreenUiEvent.UpdateNote -> {
                updateNote(oldState = oldState)
            }
        }
    }

    private fun addTask(title: String, body: String, oldState: TasksScreenUiState) {
        viewModelScope.launch {
            setState(oldState.copy(isLoading = true))

            when (val result = taskRepository.addTask(title = title, body = body)) {
                is Result.Failure -> {
                    setState(oldState.copy(isLoading = false))

                    val errorMessage =
                        result.exception.message ?: "An error occurred when adding task"
                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = errorMessage) }
                }

                is Result.Success -> {
                    setState(
                        oldState.copy(
                            isLoading = false,
                            currentTextFieldTitle = "",
                            currentTextFieldBody = "",
                        ),
                    )

                    sendEvent(TasksScreenUiEvent.OnChangeAddTaskDialogState(show = false))

                    sendEvent(TasksScreenUiEvent.GetTasks)

                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = "Task added successfully") }
                }
            }
        }
    }

    private fun getTasks(oldState: TasksScreenUiState) {
        viewModelScope.launch {
            setState(oldState.copy(isLoading = true))

            when (val result = taskRepository.getAllTasks()) {
                is Result.Failure -> {
                    setState(oldState.copy(isLoading = false))

                    val errorMessage =
                        result.exception.message ?: "An error occurred when getting your task"
                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = errorMessage) }
                }

                is Result.Success -> {
                    val tasks = result.data
                    setState(oldState.copy(isLoading = false, tasks = tasks))
                }
            }
        }
    }

    private fun deleteNote(oldState: TasksScreenUiState, taskId: String) {
        viewModelScope.launch {
            setState(oldState.copy(isLoading = true))

            when (val result = taskRepository.deleteTask(taskId = taskId)) {
                is Result.Failure -> {
                    setState(oldState.copy(isLoading = false))

                    val errorMessage =
                        result.exception.message ?: "An error occurred when deleting task"
                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = errorMessage) }
                }

                is Result.Success -> {
                    setState(oldState.copy(isLoading = false))

                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = "Task deleted successfully") }

                    sendEvent(TasksScreenUiEvent.GetTasks)
                }
            }
        }
    }

    private fun updateNote(oldState: TasksScreenUiState) {
        viewModelScope.launch {
            setState(oldState.copy(isLoading = true))

            val title = oldState.currentTextFieldTitle
            val body = oldState.currentTextFieldBody
            val taskToBeUpdated = oldState.taskToBeUpdated

            when (
                val result = taskRepository.updateTask(
                    title = title,
                    body = body,
                    taskId = taskToBeUpdated?.taskId ?: "",
                )
            ) {
                is Result.Failure -> {
                    setState(oldState.copy(isLoading = false))

                    val errorMessage =
                        result.exception.message ?: "An error occurred when updating task"
                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = errorMessage) }
                }

                is Result.Success -> {
                    setState(
                        oldState.copy(
                            isLoading = false,
                            currentTextFieldTitle = "",
                            currentTextFieldBody = "",
                        ),
                    )

                    sendEvent(TasksScreenUiEvent.OnChangeUpdateTaskDialogState(show = false))

                    setEffect { TaskScreenSideEffects.ShowSnackBarMessage(message = "Task updated successfully") }

                    sendEvent(TasksScreenUiEvent.GetTasks)
                }
            }
        }
    }

    private fun onChangeAddTaskDialog(oldState: TasksScreenUiState, isShown: Boolean) {
        setState(oldState.copy(isShowAddTaskDialog = isShown))
    }

    private fun onUpdateAddTaskDialog(oldState: TasksScreenUiState, isShown: Boolean) {
        setState(oldState.copy(isShowUpdateTaskDialog = isShown))
    }

    private fun onChangeTaskBody(oldState: TasksScreenUiState, body: String) {
        setState(oldState.copy(currentTextFieldBody = body))
    }

    private fun onChangeTaskTitle(oldState: TasksScreenUiState, title: String) {
        setState(oldState.copy(currentTextFieldTitle = title))
    }

    private fun setTaskToBeUpdated(oldState: TasksScreenUiState, task: Task) {
        setState(oldState.copy(taskToBeUpdated = task))
    }
}
