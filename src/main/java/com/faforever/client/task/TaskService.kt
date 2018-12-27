package com.faforever.client.task

import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.concurrent.Worker
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.util.concurrent.Executor

/**
 * Enqueues and runs tasks in background. Services that need to run a task (tasks that finish, not long-running
 * background jobs) in background should always submit them to this service.
 *
 *
 * There are different queues for different kind of tasks. For every queue, only one task is executed at a time.
 */
@Lazy
@Service
class TaskService @Inject
constructor(private val executor: Executor) {
    private val activeTasks: ObservableList<Worker<*>>

    val activeWorkers: ObservableList<Worker<*>>

    init {

        activeTasks = FXCollections.synchronizedObservableList(FXCollections.observableArrayList())
        activeWorkers = FXCollections.unmodifiableObservableList(activeTasks)
    }

    /**
     * Submits a task for execution in background.
     * @param <T> the task's result type
     * @param task the task to execute
    </T> */
    fun <T : PrioritizedCompletableTask<*>> submitTask(task: T): T {
        task.future.whenComplete { o, throwable ->
            activeTasks.remove(task)
            if (throwable != null) {
                logger.warn("Task failed", throwable)
            }
        }

        activeTasks.add(task)
        executor.execute(task)

        return task
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
