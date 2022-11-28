package com.liftric.persisted.queue.rules

import com.liftric.persisted.queue.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PeriodicRule(val interval: Duration = 0.seconds): JobRule() {
    override suspend fun willRemove(task: Task, event: Event) {
        if (event is Event.DidEnd) {
            task.repeat(task.copy(id = UUID::class.instance(), startTime = Clock.System.now().plus(interval)))
        }
    }
}

fun RuleInfo.repeat(interval: Duration = 0.seconds): RuleInfo {
    val rule = PeriodicRule(interval)
    rules.add(rule)
    return this
}
