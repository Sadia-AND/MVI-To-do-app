package com.example.todochamp.feature_tasks.side_effects
//  messgaes at the bottom
sealed class TaskScreenSideEffects {
    data class ShowSnackBarMessage(val message: String) : TaskScreenSideEffects()
}
