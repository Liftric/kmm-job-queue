package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class DelayRule(val duration: Duration = 0.seconds): JobRule() {
    override suspend fun willRun(context: JobContext) {
        context.broadcast(RuleEvent.OnRun(this, "Delaying job=${context.id} by duration=$duration"))
        delay(duration)
    }
}

fun RuleInfo.delay(duration: Duration = 0.seconds): RuleInfo {
    val rule = DelayRule(duration)
    rules.add(rule)
    return this
}
