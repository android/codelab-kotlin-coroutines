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

import android.support.annotation.UiThread

/**
 * Value class for passing through LiveData. Values will only be consumed once, unless the consumer
 * explicitly calls [release].
 *
 * For background see this blog post:
 * https://medium.com/androiddevelopers/livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150
 */
class ConsumableValue<T>(private val data: T) {
    var consumed = false
        private set

    /**
     * Process this value, block will only be called once.
     */
    @UiThread
    fun consume(block: ConsumableValue<T>.(T) -> Unit) {
        val wasConsumed = consumed
        consumed = true
        if (!wasConsumed) {
            this.block(data)
        }
    }

    /**
     * Inside a handle lambda, you may call this if you discover that you cannot handle
     * the event right now. It will mark the event as available to be handled by another handler.
     */
    @UiThread
    fun ConsumableValue<T>.release() {
        consumed = false
    }
}
