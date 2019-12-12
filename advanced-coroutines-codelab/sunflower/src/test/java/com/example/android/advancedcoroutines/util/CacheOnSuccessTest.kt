package com.example.android.advancedcoroutines.util

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.withTimeout
import org.junit.Test

class CacheOnSuccessTest {

    @Test
    fun getOrAwait_caches() = runBlockingTest {
        var counter = 0
        val subject = CacheOnSuccess {
            counter++
        }

        subject.getOrAwait()
        assertThat(subject.getOrAwait()).isEqualTo(0)
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun getOrAwait_reentrant_caches() = runBlockingTest {
        val completable = CompletableDeferred<Unit>()
        var counter = 0

        val subject = CacheOnSuccess {
            completable.await()
            counter++
        }

        launch {
            subject.getOrAwait()
        }

        val actual = async {
            subject.getOrAwait()
        }

        completable.complete(Unit)

        assertThat(actual.await()).isEqualTo(0)
        assertThat(counter).isEqualTo(1)
    }

    class SomeException: Throwable()

    @Test
    fun getOrAwait_throws() = runBlockingTest {
        val subject = CacheOnSuccess {
            throw SomeException()
        }

        val result = kotlin.runCatching { subject.getOrAwait() }
        assertThat(result.exceptionOrNull()).isInstanceOf(SomeException::class.java)
    }

    @Test
    fun getOrAwait_doesntCacheExceptions() = runBlockingTest {
        var counter = 0
        val subject = CacheOnSuccess {
            if(counter++ == 0) {
                throw SomeException()
            } else {
                counter
            }
        }

        kotlin.runCatching { subject.getOrAwait() }
        assertThat(subject.getOrAwait()).isEqualTo(2)
    }

    @Test
    fun getOrAwait_propagatesCancellation() = runBlockingTest {
        val subject = CacheOnSuccess {
            withTimeout(100) {
                delay(100)
            }
            1
        }
        val result = kotlin.runCatching { subject.getOrAwait() }
        assertThat(result.exceptionOrNull()).isInstanceOf(CancellationException::class.java)
    }

    @Test(expected = TimeoutCancellationException::class)
    fun getOrAwait_propagatesCancellation_evenWithFallback() = runBlockingTest {
        var called = false
        val subject = CacheOnSuccess(onErrorFallback = { called = true; 5 }) {
            withTimeout(100) {
                delay(100)
            }
            1
        }
        val result = async { subject.getOrAwait() }

        advanceTimeBy(100)
        assertThat(called).isFalse()

        // and throw it
        result.await()
    }

    @Test
    fun getOrAwait_fallsBackOnException() = runBlockingTest {
        var calls = 0
        val subject = CacheOnSuccess(onErrorFallback = { calls++ } ) {
            throw SomeException()
        }

        assertThat(subject.getOrAwait()).isEqualTo(0)
        assertThat(subject.getOrAwait()).isEqualTo(1)
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun getOrAwait_fallsBackOnException_withMultipleAwaits() = runBlockingTest {
        var calls = 0
        val subject = CacheOnSuccess(onErrorFallback = { calls++ } ) {
            delay(100)
            throw SomeException()
        }

        launch { subject.getOrAwait() }
        launch { subject.getOrAwait() }
        launch { subject.getOrAwait() }

        assertThat(subject.getOrAwait()).isEqualTo(3)
        assertThat(calls).isEqualTo(4)
    }
}