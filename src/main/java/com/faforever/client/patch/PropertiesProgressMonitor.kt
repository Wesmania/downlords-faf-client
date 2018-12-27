package com.faforever.client.patch

import javafx.beans.property.BooleanProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import org.eclipse.jgit.lib.ProgressMonitor

class PropertiesProgressMonitor : ProgressMonitor {
    private val totalTasks: IntegerProperty
    private val completed: BooleanProperty
    private val totalWork: IntegerProperty
    private val cancelled: BooleanProperty
    private val title: StringProperty
    private val workUnitsDone: IntegerProperty
    private val tasksDone: IntegerProperty

    val isCompleted: Boolean
        get() = completed.get()

    init {
        totalTasks = SimpleIntegerProperty()
        completed = SimpleBooleanProperty()
        totalWork = SimpleIntegerProperty()
        cancelled = SimpleBooleanProperty()
        title = SimpleStringProperty()
        workUnitsDone = SimpleIntegerProperty()
        tasksDone = SimpleIntegerProperty()
    }

    fun getTotalTasks(): Int {
        return totalTasks.get()
    }

    fun totalTasksProperty(): IntegerProperty {
        return totalTasks
    }

    fun completedProperty(): BooleanProperty {
        return completed
    }

    fun getTotalWork(): Int {
        return totalWork.get()
    }

    fun totalWorkProperty(): IntegerProperty {
        return totalWork
    }

    fun getCancelled(): Boolean {
        return cancelled.get()
    }

    fun cancelledProperty(): BooleanProperty {
        return cancelled
    }

    fun getTitle(): String {
        return title.get()
    }

    fun titleProperty(): StringProperty {
        return title
    }

    fun getWorkUnitsDone(): Int {
        return workUnitsDone.get()
    }

    fun workUnitsDoneProperty(): IntegerProperty {
        return workUnitsDone
    }

    override fun start(totalTasks: Int) {
        this.totalTasks.value = totalTasks
        this.tasksDone.value = 0
        workUnitsDone.value = 0
    }

    override fun beginTask(title: String, totalWork: Int) {
        workUnitsDone.value = 0
        this.totalWork.value = totalWork
        this.title.value = title
    }

    override fun update(completed: Int) {
        this.workUnitsDone.value = Math.min(totalWork.get(), workUnitsDone.value!! + completed)
    }

    override fun endTask() {
        tasksDone.value = Math.min(totalTasks.get(), tasksDone.value!! + 1)
    }

    override fun isCancelled(): Boolean {
        return cancelled.get()
    }

    fun setCancelled(cancelled: Boolean) {
        this.cancelled.set(cancelled)
    }

    fun getTasksDone(): Int {
        return tasksDone.get()
    }

    fun tasksDoneProperty(): IntegerProperty {
        return tasksDone
    }
}
