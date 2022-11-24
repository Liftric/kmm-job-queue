package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RetryRule(val limit: RetryLimit, val delay: Duration = 0.seconds): JobRule() {
    private var count = 0

    override suspend fun willSchedule(queue: Queue, operation: Task) {
        if (limit is RetryLimit.Limited) count = limit.count
    }

    override suspend fun willRemove(task: Task, event: JobDelegate.Event) {
        if (event is JobDelegate.Event.DidFail) {
            when (limit) {
                is RetryLimit.Unlimited ->  {
                    delay(delay)
                    println("Retry task id=${task.job.id}")
                    task.run()
                }
                is RetryLimit.Limited -> {
                    if (count > 0) {
                        delay(delay)
                        println("Retry task id=${task.job.id}, tries=$count")
                        count -= 1
                        task.run()
                    } else {
                        task.terminate()
                    }
                }
            }
        }
    }
}

sealed class RetryLimit {
    data class Limited(val count: Int): RetryLimit()
    object Unlimited: RetryLimit()
}

fun RuleInfos.retry(limit: RetryLimit, delay: Duration = 0.seconds): RuleInfos {
    val rule = RetryRule(limit, delay)
    rules.add(rule)
    return this
}
