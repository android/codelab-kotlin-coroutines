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

package com.example.android.kotlincoroutines.test.util

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.example.android.kotlincoroutines.util.ConsumableValue
import com.google.common.truth.Truth
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Helper extension that asserts that a LiveData gets a ConsumableValue with expected in the next 2
 * seconds.
 *
 * LiveData<ConsumableValue<T>> defines an extension function only for LiveData of ConsumableValue.
 * You can call this as if it were a method of LiveData.
 *
 * @param expected the value to find
 */
fun <T> LiveData<ConsumableValue<T>>.assertSendsEventWith(expected: T) {
    // the last value that this liveData sent, or null if none
    var found: T? = null
    // latch to wait until the value is found
    val latch = CountDownLatch(1)

    /**
     * An observer will be called every time a LiveData updates it's value. In this case we're
     * going to store data from the ConsumableValue until we find expected.
     */
    val observer = Observer<ConsumableValue<T>> { actual ->
        actual?.handle { data ->
            markUnhandled() // don't consume the event
            found = data

            if (data == expected) {
                // the expected value was sent, so we can run our assertion right away
                // let assertSendsEventWith know to exit await()
                latch.countDown()
            }
        }
    }

    // observeForever will watch for changes, including any sent before now
    observeForever(observer)
    // wait up to two seconds for the observer to let us know it found the value
    val completed = latch.await(2, SECONDS)
    // always remove the observer
    removeObserver(observer)

    Truth.assertThat(found).isEqualTo(expected)
    // and ensure that the observer actually processed an event (in case we're checking null)
    Truth.assertThat(completed).isTrue()
}

/**
 * Represents a list of capture values from a LiveData.
 *
 * This class is not threadsafe and must be used from the main thread.
 */
class LiveDataValueCapture<T> {

    private val _values = mutableListOf<T?>()
    val values: List<T?>
        get() = _values

    val channel = Channel<T?>(Channel.UNLIMITED)

    fun addValue(value: T?) {
        _values += value
        channel.offer(value)
    }

    suspend fun assertSendsValues(timeout: Long, unit: TimeUnit, vararg expected: T) {
        val expectedList = expected.asList()
        if (values == expectedList) {
            return
        }
        try {
            withTimeout(timeout, unit) {
                for (value in channel) {
                    if (values == expectedList) {
                        return@withTimeout
                    }
                }
            }
        } catch (ex: TimeoutCancellationException) {
            Truth.assertThat(values).isEqualTo(expectedList)
        }
    }
}

/**
 * Extension function to capture all values that are emitted to a LiveData<T> during the execution of
 * `captureBlock`.
 *
 * @param captureBlock a lambda that will
 */
inline fun <T> LiveData<T>.captureValues(block: LiveDataValueCapture<T>.() -> Unit) {
    val capture = LiveDataValueCapture<T>()
    val observer = Observer<T> {
        capture.addValue(it)
    }
    observeForever(observer)
    capture.block()
    removeObserver(observer)
}

/**
 * Get the current value from a LiveData without needing to register an observer.
 */
fun <T> LiveData<T>.getValueForTest(): T? {
    var value: T? = null
    var observer = Observer<T> {
        value = it
    }
    observeForever(observer)
    removeObserver(observer)
    return value
}
