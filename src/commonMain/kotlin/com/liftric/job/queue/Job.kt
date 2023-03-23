package com.liftric.job.queue

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal class JobDelegate {
    val onEvent = MutableSharedFlow<JobEvent>(extraBufferCapacity = Int.MAX_VALUE)
}

@Serializable
data class Job(
    @Serializable(with = UUIDSerializer::class)
    override val id: UUID,
    override val info: JobInfo,
    override val task: Task,
    override val startTime: Instant
) : JobContext {
    @Transient
    internal var delegate: JobDelegate? = null

    constructor(task: Task, info: JobInfo) : this(
        id = UUIDFactory.create(),
        info = info,
        task = task,
        startTime = Clock.System.now()
    )

    private var canRepeat: Boolean = true

    suspend fun run(): JobEvent {
        val event = try {
            info.rules.forEach {
                it.willRun(jobContext = this@Job)
            }
            task.body()
            JobEvent.DidSucceed(job = this@Job)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            canRepeat = task.onRepeat(e)
            JobEvent.DidFail(job = this@Job, error = e)
        }

        try {
            info.rules.forEach { rule ->
                rule.willRemove(jobContext = this@Job, result = event)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            JobEvent.DidFailOnRemove(job = this@Job, error = e)
        }
        return event
    }

    override suspend fun cancel() {
        delegate?.onEvent?.emit(JobEvent.DidCancel(job = this@Job))
    }

    override suspend fun repeat(id: UUID, info: JobInfo, task: Task, startTime: Instant) {
        if (canRepeat) {
            delegate?.onEvent?.emit(
                JobEvent.ShouldRepeat(
                    Job(
                        id = id,
                        info = info,
                        task = task,
                        startTime = startTime
                    )
                )
            )
        }
    }
}
