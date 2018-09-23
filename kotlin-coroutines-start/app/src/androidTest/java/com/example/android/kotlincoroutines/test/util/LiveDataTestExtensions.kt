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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Helper extension that asserts that a LiveData gets a ConsumableEvent with expected in the next 2
 * seconds.
 *
 * LiveData<ConsumableEvent<T>> defines an extension function only for LiveData of ConsumableEvent.
 * You can call this as if it were a method of LiveData.
 *
 * @param expected the value to find
 */
fun <T> LiveData<ConsumableValue<T>>.assertSendsEventWith(
    expected: T,
    timeout: Long = 2,
    unit: TimeUnit = SECONDS
) {
    // the last value that this liveData sent, or null if none
    var found: T? = null
    // latch to wait until the value is found
    val latch = CountDownLatch(1)

    /**
     * An observer will be called every time a LiveData updates it's value. In this case we're
     * going to store data from the ConsumableEvent until we find expected.
     */
    val observer = Observer<ConsumableValue<T>> { actual ->
        actual?.consume { data ->
            release() // don't consume the value
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
    val completed = latch.await(timeout, unit)
    // always remove the observer
    removeObserver(observer)

    Truth.assertThat(found).isEqualTo(expected)
    // and ensure that the observer actually processed an event (in case we're checking null)
    Truth.assertThat(completed).isTrue()
}

/**
 * Captor class to capture all values sent to a LiveData.
 */
class LiveDataCaptor<T> : Observer<T> {
    private val _values = mutableListOf<T?>()
    val values: List<T?>
        get() = _values

    override fun onChanged(t: T?) {
    }
}

/**
 * Represents a list of capture values from a LiveData.
 */
data class LiveDataValueCapture<T>(val values: List<T?>)

/**
 * Verify *all* capture values match expected.
 *
 * For example, `LiveDataValueCapture(listOf(true, true, false)).verify(true, true, false)` will
 * match, but `.verify(true, false)` will fail even though it matches part of the capture values.
 *
 * @param expected values to assert in the order they must have been posted
 */
fun <T> LiveDataValueCapture<T>.verify(vararg expected: T?) {
    Truth.assertThat(values).isEqualTo(expected.asList())
}

/**
 * Extension function to capture all values that are emitted to a LiveData<T> during the execution of
 * `captureBlock`.
 *
 * @param captureBlock a lambda that will
 */
inline fun <T> LiveData<T>.captureValues(captureBlock: () -> Unit): LiveDataValueCapture<T> {
    val values = mutableListOf<T?>()
    val observer = Observer<T> {
        values += it
    }
    observeForever(observer)
    captureBlock()
    removeObserver(observer)
    return LiveDataValueCapture(values)
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
