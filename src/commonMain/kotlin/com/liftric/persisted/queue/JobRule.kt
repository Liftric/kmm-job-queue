package com.liftric.persisted.queue

import kotlinx.serialization.Serializable

@Serializable
abstract class JobRule {
    open suspend fun mutating(info: TaskInfo): TaskInfo = info
    @Throws(Throwable::class)
    open suspend fun willSchedule(queue: Queue, task: Task) {}
    open suspend fun willRun(task: Task) {}
    open suspend fun willRemove(task: Task, event: Event) {}
}
