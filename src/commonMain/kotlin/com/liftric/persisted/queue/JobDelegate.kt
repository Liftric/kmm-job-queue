package com.liftric.persisted.queue

import kotlinx.coroutines.flow.MutableSharedFlow

class JobDelegate {
    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    suspend fun broadcast(event: JobEvent) {
        onEvent.emit(event)
    }

    suspend fun cancel(job: Job) {
        onEvent.emit(JobEvent.DidCancel(job))
    }

    suspend fun exit(job: Job) {
        onEvent.emit(JobEvent.DidExit(job))
    }

    suspend fun repeat(job: Job) {
        onEvent.emit(JobEvent.ShouldRepeat(job))
    }
}

