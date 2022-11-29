package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class PeriodicRule(val interval: Duration = 0.seconds): JobRule() {
    override suspend fun willRemove(context: JobContext, result: Event) {
        if (result is Event.DidEnd) {
            context.repeat(startTime = Clock.System.now().plus(interval))
        }
    }
}

fun RuleInfo.repeat(interval: Duration = 0.seconds): RuleInfo {
    val rule = PeriodicRule(interval)
    rules.add(rule)
    return this
}
