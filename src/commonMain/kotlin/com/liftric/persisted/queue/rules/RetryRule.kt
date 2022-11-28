package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class RetryRule(val limit: RetryLimit, val delay: Duration = 0.seconds): JobRule() {
    override suspend fun willRemove(task: Task, event: Event) {
        if (event is Event.DidFail) {
            when (limit) {
                is RetryLimit.Unlimited ->  {
                    task.repeat(task.copy(startTime = Clock.System.now()))
                }
                is RetryLimit.Limited -> {
                    if (limit.count > 0) {
                        val rules = task.rules.minus(this).plus(RetryRule(RetryLimit.Limited((limit.count + 1) - 2), delay))
                        task.broadcast(Event.Rule(this, "Attempting to retry task=$task"))
                        task.repeat(Task(UUID::class.instance(), task.job, task.tag, rules, Clock.System.now().plus(delay)))
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

fun RuleInfo.retry(limit: RetryLimit, delay: Duration = 0.seconds): RuleInfo {
    val rule = RetryRule(limit, delay)
    rules.add(rule)
    return this
}
