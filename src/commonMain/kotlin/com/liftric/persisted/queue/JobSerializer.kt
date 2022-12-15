package com.liftric.persisted.queue

interface JobSerializer {
    val tag: String
    fun store(job: Job)
    fun retrieve(id: String): Job?
    fun retrieveAll(): List<Job>
}
