package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.Serializable

@Serializable
data class DelayRule(val duration: Duration = 0.seconds): JobRule() {
    override suspend fun willRun(context: JobContext) {
        context.broadcast(RuleEvent.OnRun(this, "Delaying job=${context.id} by duration=$duration"))
        delay(duration)
    }
}

fun JobInfo.delay(duration: Duration = 0.seconds): JobInfo {
    val rule = DelayRule(duration)
    rules.add(rule)
    return this
}
