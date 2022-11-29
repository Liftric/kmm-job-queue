package com.liftric.persisted.queue

sealed class Event {
    data class DidSchedule(val job: JobContext): Event()
    data class DidScheduleRepeat(val job: JobContext): Event()
    data class DidThrowRepeat(val error: Error): Event()
    data class DidThrowSchedule(val error: Error): Event()
    data class DidThrow(val job: JobContext, val error: Error): Event()
    data class DidEnd(val job: JobContext): Event()
    data class DidFail(val job: JobContext, val error: Error): Event()
    data class Rule(val tag: String, val message: String): Event() {
        constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
    }
}
