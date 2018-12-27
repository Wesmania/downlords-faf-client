package com.faforever.client.task

import javafx.concurrent.Worker

import java.util.concurrent.CompletableFuture
import java.util.concurrent.RunnableFuture

interface PrioritizedCompletableTask<V> : Comparable<CompletableTask<*>>, Worker<V>, RunnableFuture<V> {

    val future: CompletableFuture<V>

    override fun getValue(): V

    override fun getException(): Throwable

    override fun getTitle(): String

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean
}
