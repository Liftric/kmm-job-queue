package com.liftric.persisted.queue

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
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

    private val enquedJobs = atomic(mutableListOf<com.liftric.persisted.queue.Job>())
    private val _jobs = atomic(mutableListOf<com.liftric.persisted.queue.Job>())
    override val jobs: List<com.liftric.persisted.queue.Job>
        get() = enquedJobs.value.plus(_jobs.value)

    val onEvent: MutableSharedFlow<JobEvent> = MutableSharedFlow()

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
        _jobs.value = _jobs.value.plus(listOf(job)).sortedBy { it.startTime }.toMutableList()
    }

    suspend fun start() {
        withContext(Dispatchers.Default) {
            while (isActive) {
                delay(1000L)
                if (_jobs.value.isEmpty()) break
                if (_jobs.value.first().startTime <= Clock.System.now()) {
                    submit {
                        val job = _jobs.value.removeFirst()
                        enquedJobs.value.add(job)
                        withTimeout(job.timeout) {
                            job.run()
                        }
                        enquedJobs.value.remove(job)
                    }
                }
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }

    suspend fun cancel(id: UUID) {
        _jobs.value.firstOrNull { it.id == id }?.apply {
            _jobs.value.remove(this)
            onEvent.emit(JobEvent.DidCancel(this, "Cancelled before run"))
        } ?: run {
            enquedJobs.value.firstOrNull { it.id == id }?.apply {
                enquedJobs.value.remove(this)
                onEvent.emit(JobEvent.DidCancel(this, "Cancelled before run"))
            }?.cancel()
        }
    }

    private fun submit(
        context: CoroutineContext = EmptyCoroutineContext,
        block: suspend CoroutineScope.() -> Unit
    ) {
        val job = scope.launch(context, CoroutineStart.LAZY, block)
        queue.tryEmit(job)
    }
}
