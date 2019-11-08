/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.util

import android.os.Handler
import android.os.Looper
import java.util.*
import java.util.concurrent.Executors

/**
 * This file contains a completely fake networking library that returns a random value after
 * a delay.
 *
 * The API is intended to look similar to Retrofit or Volley without requiring network to complete
 * the codelab.
 *
 * Callers will be given a [FakeNetworkCall] object and call [FakeNetworkCall.addOnResultListener]
 * to get results from the "network."
 *
 * Results are represented by sealed [FakeNetworkResult] with [FakeNetworkSuccess] and
 * [FakeNetworkError] subclasses.
 *
 * Retrofit: https://square.github.io/retrofit/
 * Volley: https://developer.android.com/training/volley/
 *
 */

private const val ONE_SECOND = 1_000L

private const val ERROR_RATE = 0.3

private val executor = Executors.newCachedThreadPool()

private val uiHandler = Handler(Looper.getMainLooper())

/**
 * A completely fake network library that returns from a given list of strings or an error.
 */
fun fakeNetworkLibrary(from: List<String>): FakeNetworkCall<String> {
    assert(from.isNotEmpty()) { "You must pass at least one result string" }
    val result = FakeNetworkCall<String>()

    // Launch the "network request" in a new thread to avoid blocking the calling thread
    executor.submit {
        Thread.sleep(ONE_SECOND) // pretend we actually made a network request by sleeping

        // pretend we got a result from the passed list, or randomly an error
        if (DefaultErrorDecisionStrategy.shouldError()) {
            result.onError(FakeNetworkException("Error contacting the network"))
        } else {
            result.onSuccess(from[Random().nextInt(from.size)])
        }
    }
    return result
}

/**
 * Error decision strategy is used to decide if an error should be returned by the fake request
 */
interface ErrorDecisionStrategy {
    fun shouldError(): Boolean
}

/**
 * Default error decision strategy allows us to override the behavior of a decision strategy in
 * tests
 */
object DefaultErrorDecisionStrategy : ErrorDecisionStrategy {
    var delegate: ErrorDecisionStrategy = RandomErrorStrategy

    override fun shouldError() = delegate.shouldError()
}

/**
 * Random error decision strategy uses random to return error randomly
 */
object RandomErrorStrategy : ErrorDecisionStrategy {
    override fun shouldError() = Random().nextFloat() < ERROR_RATE
}

/**
 * Fake Call for our network library used to observe results
 */
class FakeNetworkCall<T> {
    var result: FakeNetworkResult<T>? = null

    val listeners = mutableListOf<FakeNetworkListener<T>>()

    /**
     * Register a result listener to observe this callback.
     *
     * Errors will be passed to this callback as an instance of [FakeNetworkError] and successful
     * calls will be passed to this callback as an instance of [FakeNetworkSuccess].
     *
     * @param listener the callback to call when this request completes
     */
    fun addOnResultListener(listener: (FakeNetworkResult<T>) -> Unit) {
        trySendResult(listener)
        listeners += listener
    }

    /**
     * The library will call this when a result is available
     */
    fun onSuccess(data: T) {
        result = FakeNetworkSuccess(data)
        sendResultToAllListeners()
    }

    /**
     * The library will call this when an error happens
     */
    fun onError(throwable: Throwable) {
        result = FakeNetworkError(throwable)
        sendResultToAllListeners()
    }

    /**
     * Broadcast the current result (success or error) to all registered listeners.
     */
    private fun sendResultToAllListeners() = listeners.map { trySendResult(it) }

    /**
     * Send the current result to a specific listener.
     *
     * If no result is set (null), this method will do nothing.
     */
    private fun trySendResult(listener: FakeNetworkListener<T>) {
        val thisResult = result
        thisResult?.let {
            uiHandler.post {
                listener(thisResult)
            }
        }
    }
}

/**
 * Network result class that represents both success and errors
 */
sealed class FakeNetworkResult<T>

/**
 * Passed to listener when the network request was successful
 *
 * @param data the result
 */
class FakeNetworkSuccess<T>(val data: T) : FakeNetworkResult<T>()

/**
 * Passed to listener when the network failed
 *
 * @param error the exception that caused this error
 */
class FakeNetworkError<T>(val error: Throwable) : FakeNetworkResult<T>()

/**
 * Listener "type" for observing a [FakeNetworkCall]
 */
typealias FakeNetworkListener<T> = (FakeNetworkResult<T>) -> Unit

/**
 * Throwable to use in fake network errors.
 */
class FakeNetworkException(message: String) : Throwable(message)
