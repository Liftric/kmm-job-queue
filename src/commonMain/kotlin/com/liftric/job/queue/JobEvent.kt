package com.liftric.job.queue

sealed class JobEvent {
    data class DidSchedule(val job: JobContext): JobEvent()
    data class DidScheduleRepeat(val job: JobContext): JobEvent()
    data class WillRun(val job: JobContext): JobEvent()
    data class DidThrowOnSchedule(val error: Throwable): JobEvent()
    data class DidSucceed(val job: JobContext): JobEvent()
    data class DidFail(val job: JobContext, val error: Throwable): JobEvent()
    data class ShouldRepeat(val job: Job): JobEvent()
    data class DidCancel(val job: JobContext): JobEvent()
    data class DidFailOnRemove(val job: JobContext, val error: Throwable): JobEvent()
    data class NetworkRuleSatisfied(val job: JobContext): JobEvent()
    data class NetworkRuleTimeout(val job: JobContext): JobEvent()
    data class JobTimeout(val job: JobContext): JobEvent()
}
