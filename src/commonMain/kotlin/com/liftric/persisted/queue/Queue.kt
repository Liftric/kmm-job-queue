package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex

interface Queue {
    val dispatcher: CoroutineDispatcher
    val maxConcurrency: Int
    val operations: StateFlow<List<Operation>>

    data class Configuration(
        val dispatcher: CoroutineDispatcher,
        val maxConcurrency: Int
    )
}

class OperationsQueue(
    override val dispatcher: CoroutineDispatcher,
    override val maxConcurrency: Int
): Queue {
    private val _operations: MutableStateFlow<MutableList<Operation>> = MutableStateFlow(mutableListOf())
    override val operations: StateFlow<List<Operation>> = _operations.asStateFlow()

    var isRunning: Mutex = Mutex(false)

    constructor(configuration: Queue.Configuration?) : this(
        configuration?.dispatcher ?: Dispatchers.Default,
        configuration?.maxConcurrency ?: 1
    )

    fun add(operation: Operation) {
        _operations.value.add(operation)
        _operations.value.sortBy { it.startTime }
    }

    fun removeFirst(): Operation {
        return _operations.value.removeFirst()
    }
}

