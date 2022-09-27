package com.liftric.persisted.queue

abstract class JobRule {
    open suspend fun mapping(operation: Operation): Operation { return operation }
    @Throws(Exception::class)
    open suspend fun willSchedule(queue: Queue, operation: Operation) {}
    open suspend fun willRun(queue: Queue, operation: Operation) {}
    open suspend fun willRemove(operation: Operation) {}
}
