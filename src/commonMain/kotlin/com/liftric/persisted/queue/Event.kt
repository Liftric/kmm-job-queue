package com.liftric.persisted.queue

sealed class Event {
    object None: Event()
    data class DidSchedule(val task: Task): Event()
    data class DidRetry(val task: Task): Event()
    data class DidCancelSchedule(val error: Error): Event()
    data class DidTerminate(val task: Task): Event()
    data class DidEnd(val job: Job): Event()
    data class DidCancel(val job: Job, val error: Error): Event()
    data class DidFail(val job: Job, val error: Error): Event()
}
