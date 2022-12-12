# Persisted-queue

Coroutine job scheduler inspired by `Android Work Manager` and `android-priority-jobqueue` for Kotlin Multiplatform projects. Run & repeat tasks. Rebuild them from disk. Fine tune execution with rules.

## Rules

- [x] Delay
- [x] Timeout
- [x] Retry
- [x] Periodic
- [x] Unique
- [ ] Internet

## Capabilities

- [x] Cancellation (all, by id, by tag)

## Example

Define a task by implementing the interface. Define a body and limit in which cases the task can repeat.

```kotlin
@Serializable
class SyncTask: Task {
    override suspend fun body() { /* Do something */ }
    override suspend fun onRepeat(cause: Throwable): Boolean { cause is NetworkException }
}
```

Create a single instance of the scheduler on app start, and then start the queue to enqueue scheduled jobs.

```kotlin
val scheduler = JobScheduler()
scheduler.queue.start()
```

On job schedule you can add rules and add your own data store.

```kotlin
scheduler.schedule(::SyncTask) {
    rules {
        retry(RetryLimit.Limited(3), delay = 30.seconds)
        unique(data.id)
        timeout(60.seconds)
    }
}
```

You can subscribe to life cycle events (e.g. for logging).

```kotlin
scheduler.onEvent.collect { event ->
    Logger.info(event)
}
```
