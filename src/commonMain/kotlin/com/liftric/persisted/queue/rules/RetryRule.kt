package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class RetryRule(val limit: RetryLimit, val delay: Duration = 0.seconds): JobRule() {
    override suspend fun willRemove(context: JobContext, result: JobEvent) {
        if (result is JobEvent.DidFail) {
            when (limit) {
                is RetryLimit.Unlimited ->  {
                    context.repeat(startTime = Clock.System.now())
                }
                is RetryLimit.Limited -> {
                    if (limit.count > 0) {
                        val rules = context.info.rules.minus(this).plus(RetryRule(RetryLimit.Limited((limit.count + 1) - 2), delay))
                        context.broadcast(RuleEvent.OnRemove(this, "Attempting to retry task=$context"))
                        context.repeat(info = context.info.copy(rules = rules.toMutableList()), startTime = Clock.System.now().plus(delay))
                    }
                }
            }
        }
    }
}

@Serializable
sealed class RetryLimit {
    data class Limited(val count: Int): RetryLimit()
    object Unlimited: RetryLimit()
}

fun JobInfo.retry(limit: RetryLimit, delay: Duration = 0.seconds): JobInfo {
    val rule = RetryRule(limit, delay)
    rules.add(rule)
    return this
}
