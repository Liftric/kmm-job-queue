# kmm-job-queue

Coroutine job scheduler for Kotlin Multiplatform projects. Run & repeat tasks. Rebuild them from disk. Fine tune execution with rules.

The library depends on `kotlinx-serialization` for the persistence of the jobs.

⚠️ The project is still work in progress and shouldn't be used in a production project.

## Rules

- [x] Delay
- [x] Timeout
- [x] Retry
- [x] Periodic
- [x] Unique
- [ ] Network

## Capabilities

- [x] Cancellation (all, by id)
- [x] Start & stop scheduling
- [x] Restore from disk (after start)

## Example

Define a `Task` (or `DataTask<T>`), customize its body and limit when it should repeat.

⚠️ Make sure the data you pass into the task is serializable.

```kotlin
@Serializable
data class UploadData(val id: String)

class UploadTask(data: UploadData): DataTask<UploadData>(data) {
    override suspend fun body() { /* Do something */ }
    override suspend fun onRepeat(cause: Throwable): Boolean { cause is NetworkException } // Won't retry if false
}
```

Create a single instance of the job queue on app start. To start enqueuing jobs run `jobQueue.start()`.

⚠️ You have to provide the polymorphic serializer of your custom task **if you want to persist it**.

You can pass a custom `Queue.Configuration` or `JsonStorage` to the job queue.

```kotlin
val jobQueue = JobQueue(serializers = SerializersModule {
    polymorphic(Task::class) {
        subclass(UploadTask::class, UploadTask.serializer())
    }
})
jobQueue.start()
```

You can customize the jobs life cycle during schedule by defining rules.

```kotlin
val data = UploadData(id = "123456")
        
jobQueue.schedule(UploadTask(data)) {
    unique(data.id)
    retry(RetryLimit.Limited(3), delay = 30.seconds)
    persist()
}
```

You can subscribe to life cycle events (e.g. for logging).

```kotlin
jobQueue.listener.collect { event ->
    when (event) {
        is JobEvent.DidFail -> Logger.error(event.error)
        else -> Logger.info(event)
    }
}
```
