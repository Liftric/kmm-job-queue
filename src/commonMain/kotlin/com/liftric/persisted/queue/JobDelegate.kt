package com.liftric.persisted.queue

class JobDelegate {
    var onExit: (suspend () -> Unit)? = null
    var onRepeat: (suspend (Job) -> Unit)? = null
    var onEvent: (suspend (Event) -> Unit)? = null

    suspend fun broadcast(event: Event) {
        onEvent?.invoke(event)
    }

    suspend fun exit() {
        onExit?.invoke()
    }

    suspend fun repeat(job: Job) {
        onRepeat?.invoke(job)
    }
}
