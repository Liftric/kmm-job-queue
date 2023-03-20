package com.liftric.job.queue

import com.liftric.job.queue.rules.NetworkException
import com.liftric.job.queue.rules.NetworkState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withTimeout
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
        return withTimeout(info.timeout) {
            val event = try {
                info.rules.forEach { it.willRun(this@Job) }
                if (info.minRequiredNetworkState <= currentNetworkState) {
                    println("NETWORK: satisfied")
                    task.body()
                    JobEvent.DidSucceed(this@Job)
                } else {
                    println("NETWORK: unsatisfied")
                    throw NetworkException("Network requirement not satisfied!")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                canRepeat = task.onRepeat(e)
                JobEvent.DidFail(this@Job, e)
            }

            try {
                info.rules.forEach { it.willRemove(this@Job, event) }

                event
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                JobEvent.DidFailOnRemove(this@Job, e)
            }
        }
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
