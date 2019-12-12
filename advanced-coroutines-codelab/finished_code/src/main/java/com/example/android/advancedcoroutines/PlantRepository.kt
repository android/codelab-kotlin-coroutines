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
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.example.android.advancedcoroutines.util.CacheOnSuccess
import com.example.android.advancedcoroutines.utils.ComparablePair
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
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
    private var plantsListSortOrderCache = CacheOnSuccess(onErrorFallback = { listOf<String>() }) {
        plantService.customPlantSortOrder()
    }

    /**
     * Fetch a list of [Plant]s from the database and apply a custom sort order to the list.
     * Returns a LiveData-wrapped List of Plants.
     */
    val plants: LiveData<List<Plant>> = liveData<List<Plant>> {
        // Observe plants from the database (just like a normal LiveData + Room return)
        val plantsLiveData = plantDao.getPlants()

        // Fetch our custom sort from the network in a main-safe suspending call (cached)
        val customSortOrder = plantsListSortOrderCache.getOrAwait()

        // Map the LiveData, applying the sort criteria
        emitSource(plantsLiveData.map { plantList -> plantList.applySort(customSortOrder) })
    }

    /**
     * Fetch a list of [Plant]s from the database that matches a given [GrowZone] and apply a
     * custom sort order to the list. Returns a LiveData-wrapped List of Plants.
     *
     * This this similar to [plants], but uses *main-safe* transforms to avoid blocking the main
     * thread.
     */
    fun getPlantsWithGrowZone(growZone: GrowZone) =
        plantDao.getPlantsWithGrowZoneNumber(growZone.number)

            // Apply switchMap, which "switches" to a new liveData every time a new value is
            // received
            .switchMap { plantList ->

                // Use the liveData builder to construct a new coroutine-backed LiveData
                liveData {
                    val customSortOrder = plantsListSortOrderCache.getOrAwait()

                    // Emit the sorted list to the LiveData builder, which will be the new value
                    // sent to getPlantsWithGrowZoneNumber
                    emit(plantList.applyMainSafeSort(customSortOrder))
                }
            }
    /**
     * Create a flow that calls a single function
     */
    private val customSortFlow = plantsListSortOrderCache::getOrAwait.asFlow()

    /**
     * This is a version of [plants] (from above), and represent our observable database using
     * [flow], which has a similar interface to sequences in Kotlin. This allows us to do async or
     * suspending transforms of the data.
     */
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

    /**
     * This is a version of [getPlantsWithGrowZoneNumber] (from above), but using [Flow].
     * It differs from [plantsFlow] in that it only calls *main-safe* suspend functions in the
     * [map] operator, so it does not need to use [flowOn].
     */
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
                val sortOrderFromNetwork = plantsListSortOrderCache.getOrAwait()

                // The result will be the sorted list with custom sort order applied. Note that this
                // call is also main-safe due to using applyMainSafeSort.
                val nextValue = plantList.applyMainSafeSort(sortOrderFromNetwork)
                nextValue
            }
    }

    /**
     * A function that sorts the list of Plants in a given custom order.
     */
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

    /**
     * The same sorting function as [applySort], but as a suspend function that can run on any thread
     * (main-safe)
     */
    @AnyThread
    private suspend fun List<Plant>.applyMainSafeSort(customSortOrder: List<String>) =
        withContext(defaultDispatcher) {
            this@applyMainSafeSort.applySort(customSortOrder)
        }

    /**
     * Returns true if we should make a network request.
     */
    private suspend fun shouldUpdatePlantsCache(growZone: GrowZone): Boolean {
        // suspending function, so you can e.g. check the status of the database here
        return true
    }

    /**
     * Update the plants cache.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentPlantsCache() {
        if (shouldUpdatePlantsCache(NoGrowZone)) fetchRecentPlants()
    }

    /**
     * Update the plants cache for a specific grow zone.
     *
     * This function may decide to avoid making a network requests on every call based on a
     * cache-invalidation policy.
     */
    suspend fun tryUpdateRecentPlantsForGrowZoneCache(growZoneNumber: GrowZone) {
        if (shouldUpdatePlantsCache(growZoneNumber)) fetchPlantsForGrowZone(growZoneNumber)
    }

    /**
     * Fetch a new list of plants from the network, and append them to [plantDao]
     */
    private suspend fun fetchRecentPlants() {
        val plants = plantService.allPlants()
        plantDao.insertAll(plants)
    }

    /**
     * Fetch a list of plants for a grow zone from the network, and append them to [plantDao]
     */
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
