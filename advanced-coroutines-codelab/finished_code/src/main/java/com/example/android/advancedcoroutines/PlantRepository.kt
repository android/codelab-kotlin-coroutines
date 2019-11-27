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

package com.example.android.advancedcoroutines

import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.example.android.advancedcoroutines.utils.ComparablePair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository module for handling data operations.
 *
 * The @ExperimentalCoroutinesApi and @FlowPreview indicate that experimental APIs are being used.
 */
@ExperimentalCoroutinesApi
@FlowPreview
class PlantRepository private constructor(
    private val plantDao: PlantDao,
    private val plantService: NetworkService,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    // Cache for storing the custom sort order
    private var sortOrderLoader: Deferred<List<String>>? = null

    /**
     * Fetch a list of [Plant]s from the database and apply a custom sort order to the list.
     * Returns a LiveData-wrapped List of Plants.
     */
    val plants = liveData<List<Plant>> {
        // Observe plants from the database (just like a normal LiveData + Room return)
        val plantsLiveData = plantDao.getPlants()

        // Fetch our custom sort from the network in a main-safe suspending call (cached)
        val customSortOrder = getOrRefreshCachedSortOrder()

        // Map the LiveData, applying the sort criteria
        emitSource(plantsLiveData.map { plantList -> plantList.applySort(customSortOrder) })
    }

    /**
     * Fetch a list of [Plant]s from the database that matches a given [GrowZone] and apply a
     * custom sort order to the list. Returns a LiveData-wrapped List of Plants.
     *
     * This this similar to [plants], but with a [switchMap] and a nested liveData builder.
     */
    fun getPlantsWithGrowZone(growZone: GrowZone) = liveData {
        // Observe plants from the database for a given grow zone
        val plantsGrowZoneLiveData = plantDao.getPlantsWithGrowZoneNumber(growZone.number)

        // Apply switchMap, which "switches" to a new liveData every time a new value is received
        emitSource(plantsGrowZoneLiveData.switchMap { plantList ->
            // Use the liveData builder to construct a new coroutine-backed LiveData
            liveData {
                // When we want to call suspend funs on each item you can nest a LiveData builder.
                // Note: each level of nesting introduces another rotation / configuration change
                // timeout.
                val customSortOrder = getOrRefreshCachedSortOrder()

                // Emit the sorted list to the inner LiveData builder, which will be the new value
                // sent to getPlantsWithGrowZoneNumber
                emit(plantList.applyMainSafeSort(customSortOrder))
            }
        })
    }

    // Create a flow that calls a single function
    private val customSortFlow = suspend { getOrRefreshCachedSortOrder() }.asFlow()

    // This is a version of `val plants` (from above), and represent our observable database using
    // flow, which has a similar interface to sequences in Kotlin. This allows us to do async or
    // suspending transforms of the data.
    val plantsFlow: Flow<List<Plant>>
        get() = plantDao.getPlantsFlow()

            // When the result of customSortFlow is available, this will combine it with the latest
            // value from the flow above.  Thus, as long as both `plants` and `sortOrder`
            // have an initial value (their flow has emitted at least one value), any change
            // to either `plants` or `sortOrder` will call `plants.applySort(sortOrder)`.
            .combine(customSortFlow) { plants, sortOrder ->
                plants.applySort(sortOrder)
            }

            // Flow allows you to switch the dispatcher the previous transforms run on.
            // Doing so introduces a buffer that the lines above this can write to, which we don't
            // need for this UI use-case that only cares about the latest value.
            //
            // This flowOn is needed to make the [background-thread only] applySort call above
            // run on a background thread.
            .flowOn(defaultDispatcher)

            // We can tell flow to make the buffer "conflated". It removes the buffer from flowOn
            // and only shares the last value, as our UI discards any intermediate values in the
            // buffer.
            .conflate()

    // This is a version of `getPlantsWithGrowZoneNumber` (from above), but using flow.
    // It differs from `val plantsFlow` in that it only calls main-safe suspend functions in the
    // .map operator, so it does not need to use flowOn.
    fun getPlantsWithGrowZoneFlow(growZone: GrowZone): Flow<List<Plant>> {
        // A Flow from Room will return each value, just like a LiveData.
        return plantDao.getPlantsWithGrowZoneNumberFlow(growZone.number)
            // When a new value is sent from the database, we can transform it using a
            // suspending map function. This allows us to call async code like here
            // where it potentially loads the sort order from network (if not cached)
            //
            // Since all calls in this map are main-safe, flowOn is not needed here.
            // Both Room and Retrofit will run the query on a different thread even though this
            // flow is using the main thread.
            .map { plantList ->

                // We can make a request for the cached sort order directly here, because map
                // takes a suspend lambda
                //
                // This may trigger a network request if it's not yet cached, but since the network
                // call is main safe, we won't block the main thread (even though this flow executes
                // on Dispatchers.Main).
                val sortOrderFromNetwork = getOrRefreshCachedSortOrder()

                // The result will be the sorted list with custom sort order applied. Note that this
                // call is also main-safe due to using applyMainSafeSort.
                val nextValue = plantList.applyMainSafeSort(sortOrderFromNetwork)
                nextValue
            }
    }

    // A function that sorts the list of Plants in a given custom order.
    // Only runs on a background thread.
    @WorkerThread
    private fun List<Plant>.applySort(customSortOrder: List<String>): List<Plant> {
        // Our product manager requested that these plants always be sorted first in this
        // order whenever they are present in the array
        return sortedBy { plant ->
            val positionForItem = customSortOrder.indexOf(plant.plantId).let { order ->
                if (order > -1) order else Int.MAX_VALUE
            }
            ComparablePair(positionForItem, plant.name)
        }
    }

    // The same sorting function as above, but as a suspend function that can run on any thread
    @AnyThread
    private suspend fun List<Plant>.applyMainSafeSort(customSortOrder: List<String>) =
        withContext(defaultDispatcher) {
            this@applyMainSafeSort.applySort(customSortOrder)
        }

    // Loads the plant sort order from an in-memory cache. If the cache is null,
    // fetch sort order from network.
    // This is synchronized because assigning the variable sortOrderLoader from Dispatchers.Default
    // needs to be guarded.
    private suspend fun getOrRefreshCachedSortOrder(): List<String> {
        // Load the value from the network once, then cache it after
        return coroutineScope {
            synchronized(this) {
                val loader: Deferred<List<String>> = sortOrderLoader ?: async(defaultDispatcher) {
                    try {
                        return@async plantService.customPlantSortOrder()
                    } catch (ex: Throwable) {
                        sortOrderLoader = null // don't cache this default value
                        return@async emptyList<String>()
                    }
                }
                sortOrderLoader = loader
                loader
            }.await()
        }
    }

    private fun shouldUpdatePlantsCache(): Boolean {
        // Define your own logic to decide when cache is stale
        return true
    }

    suspend fun tryUpdateRecentPlantsCache() {
        if (shouldUpdatePlantsCache()) fetchRecentPlants()
    }

    suspend fun tryUpdateRecentPlantsForGrowZoneCache(growZoneNumber: GrowZone) {
        if (shouldUpdatePlantsCache()) fetchPlantsForGrowZone(growZoneNumber)
    }

    private suspend fun fetchRecentPlants(): List<Plant> {
        val plants = plantService.allPlants()
        plantDao.insertAll(plants)
        return plants
    }

    private suspend fun fetchPlantsForGrowZone(growZoneNumber: GrowZone): List<Plant> {
        val plants = plantService.plantsByGrowZone(growZoneNumber)
        plantDao.insertAll(plants)
        return plants
    }

    companion object {

        // For Singleton instantiation
        @Volatile private var instance: PlantRepository? = null

        fun getInstance(plantDao: PlantDao, plantService: NetworkService) =
            instance ?: synchronized(this) {
                instance ?: PlantRepository(plantDao, plantService).also { instance = it }
            }
    }
}
