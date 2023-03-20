package com.liftric.job.queue

import com.liftric.job.queue.rules.NetworkState
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
        UUIDFactory.create(),
        info,
        task,
        Clock.System.now()
    )

    private var canRepeat: Boolean = true

    suspend fun run(currentNetworkState: NetworkState): JobEvent {
        val event = try {
            info.rules.forEach {
                it.willRun(
                    context = this@Job,
                    currentNetworkState = currentNetworkState
                )
            }
            task.body()
            JobEvent.DidSucceed(this@Job)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            canRepeat = task.onRepeat(e)
            JobEvent.DidFail(this@Job, e)
        }

        try {
            info.rules.forEach { it.willRemove(this@Job, event) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            JobEvent.DidFailOnRemove(this@Job, e)
        }
        return event
    }

    override suspend fun cancel() {
        delegate?.onEvent?.emit(JobEvent.DidCancel(this@Job))
    }

    override suspend fun repeat(id: UUID, info: JobInfo, task: Task, startTime: Instant) {
        if (canRepeat) {
            delegate?.onEvent?.emit(JobEvent.ShouldRepeat(Job(id, info, task, startTime)))
        }
    }
}
