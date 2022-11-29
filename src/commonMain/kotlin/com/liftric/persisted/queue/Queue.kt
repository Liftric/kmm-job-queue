package com.liftric.persisted.queue

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

interface Queue {
    val scope: CoroutineScope
    val operations: List<Operation>

    data class Configuration(
        val scope: CoroutineScope
    )
}

class OperationsQueue(
    override val scope: CoroutineScope
): Queue {
    private val queue = MutableSharedFlow<Job>(extraBufferCapacity = Int.MAX_VALUE)
    private val _operations: MutableList<Operation> = mutableListOf()
    override val operations: List<Operation>
        get() = _operations

    constructor(configuration: Queue.Configuration?) : this(
        configuration?.scope ?: CoroutineScope(Dispatchers.Default)
    )

    init {
        queue.onEach { it.join() }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }

    fun add(operation: Operation) {
        _operations.add(operation)
        _operations.sortBy { it.startTime }
    }

    suspend fun start() {
        while (true) {
            delay(1000L)
            if (_operations.isEmpty()) break
            if ((_operations.first().startTime) <= Clock.System.now()) {
                submit {
                    withContext(Dispatchers.Default) {
                        _operations.removeFirst().run()
                    }
                }
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }

    private fun submit(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val job = scope.launch(context, CoroutineStart.LAZY, block)
        queue.tryEmit(job)
    }
}
