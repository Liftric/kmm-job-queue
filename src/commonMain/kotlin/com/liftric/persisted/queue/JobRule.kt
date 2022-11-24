package com.liftric.persisted.queue

abstract class JobRule {
    open suspend fun mapping(info: JobInfo): JobInfo = info
    @Throws(Exception::class)
    open suspend fun willSchedule(queue: Queue, operation: Task) {}
    open suspend fun willRun(operation: Task) {}
    open suspend fun willRemove(task: Task, event: JobDelegate.Event) {}
}
