package com.example.android.advancedcoroutines.util

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Cache the first non-error result from an async computation passed as [block].
 *
 * Usage:
 *
 * ```
 * val cachedSuccess: CacheOnSuccess<Int> = CacheOnSuccess(onErrorFallback = { 3 }) {
 *     delay(1_000) // compute value using coroutines
 *     5
 * }
 *
 * cachedSuccess.getOrAwait() // get the result from the cache, calling [block], or fallback on
 *                            // exception
 * ```
 *
 * @param onErrorFallback: Invoke this if [block] throws exception other than cancellation, the
 *        result of this lambda will be returned for this call to [getOrAwait] but will not be
 *        cached for future calls to [getOrAwait]
 * @param block Suspending lambda that produces the cached value. The first non-exceptional value
 *        returned by [block] will be cached, and future calls to [getOrAwait] will return the
 *        cached value or throw a [CancellationException].
 */
class CacheOnSuccess<T: Any>(
    private val onErrorFallback: (suspend () -> T)? = null,
    private val block: suspend () -> T
) {
    private val mutex =  Mutex()

    @Volatile
    private var deferred: Deferred<T>? = null

    /**
     * Get the current cached value, or await the completion of [block].
     *
     * The result of [block] will be cached after the fist successful result, and future calls to
     * [getOrAwait] will return the cached value.
     *
     * If multiple coroutines call [getOrAwait] before [block] returns, then [block] will only
     * execute one time. If successful, they will all get the same success result. In the case of
     * error it will not cache, and a later call to [getOrAwait] will retry the [block].
     *
     * If [onErrorFallback] is not null, this function will *always* call the lambda in case of
     * error and will never cache the error result.
     *
     * @throws Throwable the exception thrown by [block] if [onErrorFallback] is not provided.
     * @throws CancellationException will throw a [CancellationException] if called in a cancelled
     *          coroutine context. This will happen even when reading the cached value.
     */
    suspend fun getOrAwait(): T {
        return supervisorScope {
            // This function is thread-safe _iff_ deferred is @Volatile and all reads and writes
            // hold the mutex.

            // only allow one coroutine to try running block at a time by using a coroutine-base
            // Mutex
            val currentDeferred = mutex.withLock {
                deferred?.let { return@withLock it }

                async {
                    // Note: mutex is not held in this async block
                    block()
                }.also {
                    // Note: mutex is held here
                    deferred = it
                }
            }

            // await the result, with our custom error handling
            currentDeferred.safeAwait()
        }
    }

    /**
     * Await for a deferred, with our custom error logic.
     *
     * If there is an exception, clear the `deferred` field if this is still the current stored
     * value.
     *
     * If the exception is cancellation, rethrow it without any changes.
     *
     * Otherwise, try to get a fallback value from onErrorFallback.
     *
     * This function is thread-safe _iff_ [deferred] is @Volatile and all reads and writes hold the
     * mutex.
     *
     * @param this the deferred to wait for.
     */
    private suspend fun Deferred<T>.safeAwait(): T {
        try {
            // Note: this call to await will always throw if this coroutine is cancelled
            return await()
        } catch (throwable: Throwable) {
            // If deferred still points to `this` instance of Deferred, clear it because we don't
            // want to cache errors
            mutex.withLock {
                if (deferred == this) {
                    deferred = null
                }
            }

            // never consume cancellation
            if (throwable is CancellationException) {
                throw throwable
            }

            // return fallback if provided
            onErrorFallback?.let { fallback -> return fallback() }

            // if we get here the error fallback didn't provide a fallback result, so throw the
            // exception to the caller
            throw throwable
        }
    }

}