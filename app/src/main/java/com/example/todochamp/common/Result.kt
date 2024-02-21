package com.example.todochamp.common
//The use of a sealed class allows for defining a closed hierarchy of result types,
// and it is commonly used to represent the outcome of operations, such as network requests,
// database interactions, or other asynchronous actions.

//Sealed classes let you restrict the use of inheritance.
sealed class Result<out T : Any> {
    data class Success<out T : Any>(val data: T) : Result<T>()
    data class Failure(val exception: Exception) : Result<Nothing>()
}
