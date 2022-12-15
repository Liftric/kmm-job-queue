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

Define a `DataTask<*>` or a `Task` (`DataTask<Unit>`), customize its body and limit when it should repeat.

⚠️ Make sure the data you pass into the task is serializable.

```kotlin
@Serializable
data class UploadData(val id: String)

class UploadTask(data: UploadData): DataTask<UploadData>(data) {
    override suspend fun body() { /* Do something */ }
    override suspend fun onRepeat(cause: Throwable): Boolean { cause is NetworkException }
}
```

Create a single instance of the scheduler on app start. To start enqueuing jobs run `queue.start()`.

You can pass a `Queue.Configuration` or a custom `JobSerializer` to the scheduler.

```kotlin
val scheduler = JobScheduler()
scheduler.queue.start()
```

You can customize the jobs life cycle during schedule by defining rules.

```kotlin
val data = UploadData(id = ...)
        
scheduler.schedule(UploadTask(data)) {
    unique(data.id)
    retry(RetryLimit.Limited(3), delay = 30.seconds)
    persist()
}
```

You can subscribe to life cycle events (e.g. for logging).

```kotlin
scheduler.onEvent.collect { event ->
    Logger.info(event)
}
```
