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

Kotlin/Native doesn't have full reflection capabilities, thus we instantiate the job classes in a custom factory class.

```kotlin
class TestFactory: TaskFactory {
    override fun <T : Task> create(type: KClass<T>, params: Map<String, Any>): Task = when(type) {
        TestTask::class -> TestTask(params)
        else -> throw Exception("Unknown job class!")
    }
}
```

Create a single instance of the scheduler on app start, and then start the queue to enqueue scheduled jobs.

```kotlin
val factory = TestFactory()
val scheduler = JobScheduler(factory)
scheduler.queue.start()
```

On job schedule you can add rules, define a store, and inject parameters.

````kotlin
val data = ...

scheduler.schedule<UploadTask> {
    rules {
        retry(RetryLimit.Limited(3), delay = 30.seconds)
        unique(data.id)
        timeout(60.seconds)
    }
    persist(Store.Preferences)
    params(
        "result" to data.value, 
        "timestamp" to data.date
    )
}
````

You can subscribe to life cycle events (e.g. for logging).

```kotlin
scheduler.onEvent.collect { event ->
    Logger.info(event)
}
```
