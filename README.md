# Persisted-queue

Coroutine job scheduler inspired by `Android Work Manager` and `android-priority-jobqueue` for Kotlin Multiplatform projects. Run & repeat tasks sequentially (for now). Rebuild them from disk. Fine tune execution with rules.

## Rules

- [x] Delay
- [x] Timeout
- [x] Retry
- [x] Periodic
- [x] Unique
- [ ] Internet

## Example

```kotlin
val factory = TaskFactory()
val scheduler = JobScheduler(factory)
val data = Data()

scheduler.schedule<UploadTask> {
    rules {
        retry(RetryLimit.Limited(3), delay = 30.seconds)
        unique(data.id)
        timeout(60.seconds)
    }
    params(
        "result" to data.value, 
        "timestamp" to data.date
    )
}

scheduler.queue.start()
```
