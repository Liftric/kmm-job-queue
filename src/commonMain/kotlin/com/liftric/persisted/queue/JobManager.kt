package com.liftric.persisted.queue

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JobManager(val factory: JobFactory) {
    val queue = Queue()
    private val delegate = TaskDelegate()

    @PublishedApi internal val _onEvent: MutableStateFlow<Event> = MutableStateFlow(Event.None)
    val onEvent: StateFlow<Event> = _onEvent.asStateFlow()

    init {
        delegate.onEvent = { event ->
            when (event) {
                is Event.DidTerminate -> {
                    next()
                }
                else -> Unit
            }
            println(event)
            _onEvent.emit(event)
        }
    }

    suspend inline fun <reified T: Job> schedule(init: TaskInfo.() -> TaskInfo) {
        try {
            val info = init(TaskInfo()).apply {
                rules.forEach { it.mutating(this) }
            }

            val job = factory.create(T::class, info.params)

            val task = Task(job, info)

            task.rules.forEach { it.willSchedule(queue, task) }

            _onEvent.emit(Event.DidSchedule(task))

            queue.tasks.add(task)
        } catch (error: Error) {
            _onEvent.emit(Event.DidCancelSchedule(error))
        }
    }

    suspend fun start() = next()

    private suspend fun next() {
        if (queue.tasks.isEmpty()) return
        withContext(queue.dispatcher) {
            launch {
                val task = queue.tasks.removeFirst()
                task.delegate = delegate
                task.run()
            }
        }
    }
}
