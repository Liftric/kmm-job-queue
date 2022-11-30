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
    val jobs: List<com.liftric.persisted.queue.Job>

    data class Configuration(
        val scope: CoroutineScope
    )
}

class JobQueue(
    override val scope: CoroutineScope
): Queue {
    private val queue = MutableSharedFlow<Job>(extraBufferCapacity = Int.MAX_VALUE)
    private val _jobs: MutableList<com.liftric.persisted.queue.Job> = mutableListOf()
    override val jobs: List<com.liftric.persisted.queue.Job>
        get() = _jobs

    constructor(configuration: Queue.Configuration?) : this(
        configuration?.scope ?: CoroutineScope(Dispatchers.Default)
    )

    init {
        queue.onEach { it.join() }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }

    @PublishedApi
    internal fun add(job: com.liftric.persisted.queue.Job) {
        _jobs.add(job)
        _jobs.sortBy { it.startTime }
    }

    suspend fun start() {
        withContext(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                if (_jobs.isEmpty()) break
                if (_jobs.first().startTime <= Clock.System.now()) {
                    submit {
                        val job = _jobs.removeFirst()
                        withTimeout(job.timeout) {
                            job.run()
                        }
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
