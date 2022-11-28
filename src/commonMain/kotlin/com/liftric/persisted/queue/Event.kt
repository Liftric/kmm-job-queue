package com.liftric.persisted.queue

sealed class Event {
    object None: Event()
    data class DidSchedule(val task: Task): Event()
    data class WillRetry(val task: Task): Event()
    data class Error(val error: kotlin.Error): Event()
    data class DidEnd(val job: Job): Event()
    data class DidFail(val job: Job, val error: kotlin.Error): Event()
}
