package com.faforever.client.util

import javafx.concurrent.Service
import javafx.concurrent.Task
import javafx.concurrent.Worker
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.invoke.MethodHandles

object ConcurrentUtil {

    private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

    /**
     * Executes the given task in background and calls the specified callback when finished. The callback is always called
     * on the FX application thread.
     *
     * @return the [Service] the specified task has been started in.
     */
    // TODO this needs to be removed
    fun <T> executeInBackground(worker: Worker<T>): Service<T> {
        val service = object : Service<T>() {
            override fun createTask(): Task<T> {
                return worker as Task<T>
            }
        }
        service.setOnFailed { event -> logger.error("Task failed", event.source.exception) }
        service.start()

        return service
    }
}// Utility class
