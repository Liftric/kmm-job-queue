package com.liftric.persisted.queue

sealed class JobEvent {
    data class DidSchedule(val job: JobContext): JobEvent()
    data class DidScheduleRepeat(val job: JobContext): JobEvent()
    data class WillRun(val job: JobContext): JobEvent()
    data class DidThrowOnRepeat(val error: Error): JobEvent()
    data class DidThrowOnSchedule(val error: Error): JobEvent()
    data class DidEnd(val job: JobContext): JobEvent()
    data class DidFail(val job: JobContext, val error: Error): JobEvent()
    data class DidCancel(val job: JobContext, val message: String): JobEvent()
    data class DidFailOnRemove(val job: JobContext, val error: Error): JobEvent()
}

sealed class RuleEvent: JobEvent() {
    data class OnMutate(val rule: String, val message: String): RuleEvent() {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }
    data class OnSchedule(val rule: String, val message: String): RuleEvent() {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }
    data class OnRun(val rule: String, val message: String): RuleEvent() {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }
    data class OnRemove(val rule: String, val message: String): RuleEvent() {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }
}
