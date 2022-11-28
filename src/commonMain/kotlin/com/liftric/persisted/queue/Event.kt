package com.liftric.persisted.queue

sealed class Event {
    data class DidSchedule(val task: Task): Event()
    data class DidRepeat(val task: Task): Event()
    data class Error(val error: kotlin.Error): Event()
    data class DidEnd(val job: Job): Event()
    data class DidFail(val job: Job, val error: kotlin.Error): Event()
    data class Rule(val tag: String, val message: String): Event() {
        constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
    }
}
