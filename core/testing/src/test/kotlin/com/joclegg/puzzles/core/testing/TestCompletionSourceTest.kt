package com.joclegg.puzzles.core.testing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class TestCompletionSourceTest {
    @ParameterizedTest
    @ValueSource(strings = ["done", "", "a longer result"])
    fun `given a value completed from another thread when awaited in runTest then that value is returned`(expected: String) =
        runTest {
            // Arrange
            val source = TestCompletionSource<String>()
            thread { source.complete(expected) }

            // Act
            val value = source.await()

            // Assert
            assertEquals(expected, value)
        }

    @Test
    fun `given nothing completes when awaited then it fails with an assertion error after the timeout`() =
        runTest {
            // Arrange
            val source = TestCompletionSource<Unit>(timeout = 100.milliseconds)

            // Act
            val failure = assertFailsWith<AssertionError> { source.await() }

            // Assert
            assertEquals("Not completed within 100ms", failure.message)
        }

    @Test
    fun `given a failure when awaited then the cause is thrown`() =
        runTest {
            // Arrange
            val source = TestCompletionSource<Unit>()
            source.fail(IllegalStateException("broken"))

            // Act
            val failure = assertFailsWith<IllegalStateException> { source.await() }

            // Assert
            assertEquals("broken", failure.message)
        }

    @Test
    fun `given the source is cancelled when awaited then it throws cancellation`() =
        runTest {
            // Arrange
            val source = TestCompletionSource<Unit>()
            source.cancel()

            // Act
            val failure = runCatching { source.await() }.exceptionOrNull()

            // Assert
            assertTrue(failure is CancellationException)
        }

    @Test
    fun `given a coroutine awaiting when that coroutine is cancelled then it stops waiting`() =
        runTest {
            // Arrange
            val source = TestCompletionSource<Unit>(timeout = 5.seconds)
            val waiting = async { source.await() }

            // Act
            waiting.cancel()
            waiting.join()

            // Assert
            assertTrue(waiting.isCancelled)
        }
}
