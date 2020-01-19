/*
 * Copyright (C) 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.kotlincoroutines.fakes

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.android.kotlincoroutines.main.MainNetwork
import com.example.android.kotlincoroutines.main.Title
import com.example.android.kotlincoroutines.main.TitleDao
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Fake [TitleDao] for use in tests.
 */
class TitleDaoFake(initialTitle: String) : TitleDao {
    /**
     * A channel is a Coroutines based implementation of a blocking queue.
     *
     * We're using it here as a buffer of inserted elements.
     *
     * This uses a channel instead of a list to allow multiple threads to call insertTitle and
     * synchronize the results with the test thread.
     */
    private val insertedForNext = Channel<Title>(capacity = Channel.BUFFERED)

    override suspend fun insertTitle(title: Title) {
        insertedForNext.send(title)
        _titleLiveData.value = title
    }

    private val _titleLiveData = MutableLiveData<Title?>(Title(initialTitle))

    override val titleLiveData: LiveData<Title?>
        get() = _titleLiveData

    /**
     * Assertion that the next element inserted has a title of expected
     *
     * If the element was previously inserted and is currently the most recent element
     * this assertion will also match. This allows tests to avoid synchronizing calls to insert
     * with calls to assertNextInsert.
     *
     * If multiple items were inserted, this will always match the first item that was not
     * previously matched.
     *
     * @param expected the value to match
     * @param timeout duration to wait (this is provided for instrumentation tests that may run on
     *                multiple threads)
     * @param unit timeunit
     * @return the next value that was inserted into this dao, or null if none found
     */
    fun nextInsertedOrNull(timeout: Long = 2_000): String? {
        var result: String? = null
        runBlocking {
            // wait for the next insertion to complete
            try {
                withTimeout(timeout) {
                    result = insertedForNext.receive().title
                }
            } catch (ex: TimeoutCancellationException) {
                // ignore
            }
        }
        return result
    }
}

/**
 * Testing Fake implementation of MainNetwork
 */
class MainNetworkFake(var result: String) : MainNetwork {
    override suspend fun fetchNextTitle() = result
}

/**
 * Testing Fake for MainNetwork that lets you complete or error all current requests
 */
class MainNetworkCompletableFake() : MainNetwork {
    private var completable = CompletableDeferred<String>()

    override suspend fun fetchNextTitle(): String = completable.await()

    fun sendCompletionToAllCurrentRequests(result: String) {
        completable.complete(result)
        completable = CompletableDeferred()
    }

    fun sendErrorToCurrentRequests(throwable: Throwable) {
        completable.completeExceptionally(throwable)
        completable = CompletableDeferred()
    }

}