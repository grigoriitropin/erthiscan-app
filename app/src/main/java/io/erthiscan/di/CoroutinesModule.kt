package io.erthiscan.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * APPLICATION SCOPE QUALIFIER
 * Used to distinguish the long-lived application CoroutineScope from 
 * short-lived ones like ViewModelScope.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * COROUTINES MODULE
 * 
 * ARCHITECTURAL ROLE:
 * Centrally defines and provides coroutine-related dependencies for the app.
 * This ensures that background tasks are handled consistently and allows for 
 * easier testing by swapping dispatchers in test builds.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoroutinesModule {

    /**
     * Provides a [CoroutineScope] that exists for the entire life of the application.
     * 
     * WHY SUPERVISORJOB: 
     * Ensures that if one background task (child) fails, it doesn't cancel the 
     * entire application scope, allowing other tasks to continue running.
     * 
     * WHY DISPATCHERS.IO: 
     * Tasks launched in this scope are typically network or disk operations that 
     * should persist even if the initiating screen (ViewModel) is destroyed.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
