package com.liftric.persisted.queue

import com.liftric.persisted.queue.rules.*
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.datetime.serializers.InstantIso8601Serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic

expect class JobScheduler: AbstractJobScheduler
abstract class AbstractJobScheduler(
    serializers: SerializersModule,
    configuration: Queue.Configuration?,
    settings: Settings
) {
    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)

    private val module = SerializersModule {
        contextual(InstantIso8601Serializer)
        polymorphic(JobRule::class) {
            subclass(DelayRule::class, DelayRule.serializer())
            subclass(PeriodicRule::class, PeriodicRule.serializer())
            subclass(RetryRule::class, RetryRule.serializer())
            subclass(TimeoutRule::class, TimeoutRule.serializer())
            subclass(UniqueRule::class, UniqueRule.serializer())
            subclass(PersistenceRule::class, PersistenceRule.serializer())
        }
    }
    private val format = Json { serializersModule = module + serializers }

    @PublishedApi
    internal val delegate = JobDelegate()

    @PublishedApi
    internal val queue = JobQueue(
        settings,
        format,
        configuration ?: Queue.Configuration(CoroutineScope(Dispatchers.Default), 1)
    )

    init {
        delegate.onExit = { settings.remove(it.id.toString()) }
        delegate.onRepeat = { repeat(it) }
        delegate.onEvent = { onEvent.emit(it) }
    }

    suspend fun schedule(task: () -> DataTask<Unit>, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(), configure)
    }

    suspend inline fun <reified Data> schedule(data: Data, task: (Data) -> DataTask<Data>, configure: JobInfo.() -> JobInfo = { JobInfo() }) {
        schedule(task(data), configure)
    }

    suspend inline fun <reified Data> schedule(task: DataTask<Data>, configure: JobInfo.() -> JobInfo = { JobInfo() }) = try {
        val info = configure(JobInfo()).apply {
            rules.forEach { it.mutating(this) }
        }

        val job = Job(task, info)
        job.delegate = delegate

        job.info.rules.forEach {
            it.willSchedule(queue, job)
        }

        queue.add(job).apply {
            onEvent.emit(JobEvent.DidSchedule(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnSchedule(error))
    }

    private suspend fun repeat(job: Job<*>) = try {
        job.delegate = delegate

        job.info.rules.forEach {
            it.willSchedule(queue, job)
        }

        queue.add(job).apply {
            onEvent.emit(JobEvent.DidScheduleRepeat(job))
        }
    } catch (error: Error) {
        onEvent.emit(JobEvent.DidThrowOnRepeat(error))
    }
}
