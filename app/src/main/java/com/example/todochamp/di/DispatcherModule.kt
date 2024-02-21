package com.example.todochamp.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier
//this code sets up a Dagger module to provide a CoroutineDispatcher specifically for IO operations
// in an Android application using Hilt for dependency injection.
// The custom qualifier annotation is used to uniquely identify the
// IO dispatcher provided by this module.
@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Retention
@Qualifier
annotation class IoDispatcher
