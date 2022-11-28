package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex

data class Queue(
    val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    val tasks: MutableStateFlow<MutableList<Task>> = MutableStateFlow(mutableListOf()),
    val maxConcurrency: Int = 1,
    var isRunning: Mutex = Mutex(false)
)
