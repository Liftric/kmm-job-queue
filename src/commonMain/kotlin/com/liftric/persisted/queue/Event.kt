package com.liftric.persisted.queue

sealed class Event {
    data class DidSchedule(val job: JobContext): Event()
    data class DidScheduleRepeat(val job: JobContext): Event()
    data class DidThrowOnRepeat(val error: Error): Event()
    data class DidThrowOnSchedule(val error: Error): Event()
    data class DidEnd(val job: JobContext): Event()
    data class DidFail(val job: JobContext, val error: Error): Event()
    data class DidFailOnRemove(val job: JobContext, val error: Error): Event()

    sealed class Rule: Event() {
        data class OnMutate(val tag: String, val message: String): Event() {
            constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
        }
        data class WillSchedule(val tag: String, val message: String): Event() {
            constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
        }
        data class WillRun(val tag: String, val message: String): Event() {
            constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
        }
        data class WillRemove(val tag: String, val message: String): Event() {
            constructor(tag: JobRule, message: String) : this(tag::class.simpleName!!, message)
        }
    }
}
