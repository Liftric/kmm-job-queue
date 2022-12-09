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
    data class NotAllowedToRepeat(val job: JobContext): JobEvent()
}

sealed class RuleEvent(open val rule: String, open val message: String): JobEvent() {
    data class OnMutate(override val rule: String, override val message: String): RuleEvent(rule, message) {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }

    data class OnSchedule(override val rule: String, override val message: String): RuleEvent(rule, message) {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }

    data class OnRun(override val rule: String, override val message: String): RuleEvent(rule, message) {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }

    data class OnRemove(override val rule: String, override val message: String): RuleEvent(rule, message) {
        constructor(rule: JobRule, message: String) : this(rule::class.simpleName!!, message)
    }
}
