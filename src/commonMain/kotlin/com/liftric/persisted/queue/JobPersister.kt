package com.liftric.persisted.queue

interface JobPersister {
    val tag: String
    fun store(job: Job)
    fun retrieve(id: String): Job?
    fun retrieveAll(): List<Job>
}
