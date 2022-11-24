package com.liftric.persisted.queue

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class Queue {
    val dispatcher: CoroutineDispatcher = Dispatchers.Default
    val tasks: MutableList<Task> = mutableListOf()
}
