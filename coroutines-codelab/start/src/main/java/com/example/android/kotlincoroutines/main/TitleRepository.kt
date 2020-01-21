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

package com.example.android.kotlincoroutines.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.android.kotlincoroutines.util.BACKGROUND

/**
 * TitleRepository provides an interface to fetch a title or request a new one be generated.
 *
 * Repository modules handle data operations. They provide a clean API so that the rest of the app
 * can retrieve this data easily. They know where to get the data from and what API calls to make
 * when data is updated. You can consider repositories to be mediators between different data
 * sources, in our case it mediates between a network API and an offline database cache.
 */
class TitleRepository(val network: MainNetwork, val titleDao: TitleDao) {

    /**
     * [LiveData] to load title.
     *
     * This is the main interface for loading a title. The title will be loaded from the offline
     * cache.
     *
     * Observing this will not cause the title to be refreshed, use [TitleRepository.refreshTitleWithCallbacks]
     * to refresh the title.
     */
    val title: LiveData<String?> = titleDao.titleLiveData.map { it?.title }


    // TODO: Add coroutines-based `fun refreshTitle` here

    /**
     * Refresh the current title and save the results to the offline cache.
     *
     * This method does not return the new title. Use [TitleRepository.title] to observe
     * the current tile.
     */
    fun refreshTitleWithCallbacks(titleRefreshCallback: TitleRefreshCallback) {
        // This request will be run on a background thread by retrofit
        BACKGROUND.submit {
            try {
                // Make network request using a blocking call
                val result = network.fetchNextTitle().execute()
                if (result.isSuccessful) {
                    // Save it to database
                    titleDao.insertTitle(Title(result.body()!!))
                    // Inform the caller the refresh is completed
                    titleRefreshCallback.onCompleted()
                } else {
                    // If it's not successful, inform the callback of the error
                    titleRefreshCallback.onError(
                            TitleRefreshError("Unable to refresh title", null))
                }
            } catch (cause: Throwable) {
                // If anything throws an exception, inform the caller
                titleRefreshCallback.onError(
                        TitleRefreshError("Unable to refresh title", cause))
            }
        }
    }
}

/**
 * Thrown when there was a error fetching a new title
 *
 * @property message user ready error message
 * @property cause the original cause of this exception
 */
class TitleRefreshError(message: String, cause: Throwable?) : Throwable(message, cause)

interface TitleRefreshCallback {
    fun onCompleted()
    fun onError(cause: Throwable)
}
