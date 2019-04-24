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

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.android.kotlincoroutines.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    var job: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonEmptyTask.setOnClickListener { startEmptyTask() }
        buttonTaskWithProgress.setOnClickListener { startTaskWithProgress() }
        buttonCancelableTask.setOnClickListener { startCancelableTask() }
        buttonCancelTask.setOnClickListener { cancelTask() }
    }

    private fun startEmptyTask() {
        println("start empty task")
        GlobalScope.launch(Dispatchers.Main) {
            delay(1000)
            showText("Hello from Kotlin Coroutines!")
        }
    }

    private fun startTaskWithProgress() {
        println("start Empty With Progress")
        GlobalScope.launch(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            delay(5000)
            showText("Hello from Kotlin Coroutines!")
            progressBar.visibility = View.GONE
        }
    }

    private fun startCancelableTask() {
        println("start cancelable Task")
        job = GlobalScope.launch(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            delay(5000)
            showText("Hello from Kotlin Coroutines!")
            progressBar.visibility = View.GONE
        }
        job?.invokeOnCompletion {
            if (it is CancellationException) { // if coroutine was cancelled
                showText("Cancelable Task was cancel")
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun showText(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun cancelTask() {
        job?.cancel()
    }
}