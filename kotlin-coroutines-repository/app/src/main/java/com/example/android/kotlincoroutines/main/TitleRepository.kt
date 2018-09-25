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

package com.example.android.kotlincoroutines.main

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import com.example.android.kotlincoroutines.main.TitleRepository.RefreshState.Success
import com.example.android.kotlincoroutines.main.TitleRepository.RefreshState.Error
import com.example.android.kotlincoroutines.main.TitleRepository.RefreshState.Loading
import com.example.android.kotlincoroutines.util.BACKGROUND
import com.example.android.kotlincoroutines.util.FakeNetworkError
import com.example.android.kotlincoroutines.util.FakeNetworkSuccess
import kotlin.LazyThreadSafetyMode.NONE

/**
 * TitleRepository provides an interface to fetch a title or request a new one be generated.
 *
 * Repository modules handle data operations. They provide a clean API so that the rest of the app
 * can retrieve this data easily. They know where to get the data from and what API calls to make
 * when data is updated. You can consider repositories to be mediators between different data
 * sources, in our case it mediates between a network API and an offline database cache.
 */
class TitleRepository(private val network: MainNetwork, private val titleDao: TitleDao) {

    /**
     * [LiveData] to load title.
     *
     * This is the main interface for loading a title. The title will be loaded from the offline
     * cache.
     *
     * Observing this will not cause the title to be refreshed, use [TitleRepository.refreshTitle]
     * to refresh the title.
     *
     * Because this is defined as `by lazy` it won't be instantiated until the property is
     * used for the first time.
     */
    val title: LiveData<String> by lazy<LiveData<String>>(NONE) {
        Transformations.map(titleDao.loadTitle()) { it?.title }
    }

    /**
     * Refresh the current title and save the results to the offline cache.
     *
     * This method does not return the new title. Use [TitleRepository.title] to observe
     * the current tile.
     *
     * @param onStateChanged callback called when state changes to Loading, Success, or Error
     */
    // TODO: Reimplement with coroutines and remove state listener
    fun refreshTitle(onStateChanged: TitleStateListener) {
        onStateChanged(Loading)
        val call = network.fetchNewWelcome()
        call.addOnResultListener { result ->
            when (result) {
                is FakeNetworkSuccess<String> -> {
                    BACKGROUND.submit {
                        // run insertTitle on a background thread
                        titleDao.insertTitle(Title(result.data))
                    }
                    onStateChanged(Success)
                }
                is FakeNetworkError -> {
                    onStateChanged(Error(TitleRefreshError(result.error)))
                }
            }
        }
    }

    /**
     * Class that represents the state of a refresh request.
     *
     * Sealed classes can only be extended from inside this file.
     */
    // TODO: Remove this class after rewriting refreshTitle
    sealed class RefreshState {
        /**
         * The request is currently loading.
         *
         * An object is a singleton that cannot have more than one instance.
         */
        object Loading : RefreshState()

        /**
         * The request has completed successfully.
         *
         * An object is a singleton that cannot have more than one instance.
         */
        object Success : RefreshState()

        /**
         * The request has completed with an error
         *
         * @param error error message ready to be displayed to user
         */
        class Error(val error: Throwable) : RefreshState()
    }
}

/**
 * Listener for [RefreshState] changes.
 *
 * A typealias introduces a shorthand way to say a complex type. It does not create a new type.
 */
// TODO: Remove this typealias after rewriting refreshTitle
typealias TitleStateListener = (TitleRepository.RefreshState) -> Unit

/**
 * Thrown when there was a error fetching a new title
 *
 * @property message user ready error message
 * @property cause the original cause of this exception
 */
class TitleRefreshError(cause: Throwable) : Throwable(cause.message, cause)

/**
 * Suspend function to use callback-based [FakeNetworkCall] in coroutines
 *
 * @return network result after completion
 * @throws Throwable original exception from library if network request fails
 */
// TODO: Implement FakeNetworkCall<T>.await() here
