package com.joclegg.puzzles.core.testing

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TestCompletionSource<T>(
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {
    private val deferred = CompletableDeferred<T>()

    val isCompleted: Boolean get() = deferred.isCompleted

    fun complete(value: T): Boolean = deferred.complete(value)

    fun fail(cause: Throwable): Boolean = deferred.completeExceptionally(cause)

    fun cancel() = deferred.cancel()

    suspend fun await(): T =
        withContext(Dispatchers.Default) {
            try {
                withTimeout(timeout) { deferred.await() }
            } catch (timedOut: TimeoutCancellationException) {
                throw AssertionError("Not completed within $timeout", timedOut)
            }
        }

    companion object {
        val DEFAULT_TIMEOUT = 5.seconds
    }
}

fun TestCompletionSource<Unit>.complete(): Boolean = complete(Unit)
