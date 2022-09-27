package com.liftric.persisted.queue

class UniqueRule(private val id: String): JobRule() {
    override suspend fun mapping(operation: Operation): Operation {
        return operation.apply { tag = id }
    }

    @Throws(Exception::class)
    override suspend fun willSchedule(queue: Queue, operation: Operation) {
        for (item in queue.operations) {
            if (item.tag == id) {
                throw Exception("Should be unique")
            }
        }
    }
}
