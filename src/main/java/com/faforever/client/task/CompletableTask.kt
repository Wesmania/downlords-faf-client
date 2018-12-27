package com.faforever.client.task

import javafx.concurrent.Task

import java.util.concurrent.CompletableFuture

abstract class CompletableTask<V>(private var priority: Priority?) : Task<V>(), PrioritizedCompletableTask<V> {

    override val future: CompletableFuture<V>

    init {
        this.future = CompletableFuture()
        setOnCancelled { event -> future.cancel(true) }
        setOnFailed { event -> future.completeExceptionally(exception) }
        setOnSucceeded { event -> future.complete(value) }
    }

    override fun compareTo(other: CompletableTask<*>): Int {
        return priority!!.compareTo(other.priority!!)
    }

    fun setPriority(priority: Priority) {
        if (this.priority != null) {
            throw IllegalStateException("Priority has already been set")
        }
        this.priority = priority
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        future.cancel(mayInterruptIfRunning)
        return super.cancel(mayInterruptIfRunning)
    }

    enum class Priority {
        LOW,
        MEDIUM,
        HIGH
    }
}
