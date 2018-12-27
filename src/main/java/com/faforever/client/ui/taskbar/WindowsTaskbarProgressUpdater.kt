package com.faforever.client.ui.taskbar

import com.faforever.client.FafClientApplication
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.task.TaskService
import javafx.beans.Observable
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.concurrent.Worker
import javafx.scene.control.ProgressIndicator
import org.bridj.Pointer
import org.bridj.PointerIO
import org.bridj.cpp.com.COMRuntime
import org.bridj.cpp.com.shell.ITaskbarList3
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.util.concurrent.Executor
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import com.github.nocatch.NoCatch.noCatch

/**
 * Updates the progress in the Windows 7+ task bar, if available.
 */
@Component
@Profile(FafClientApplication.PROFILE_WINDOWS)
class WindowsTaskbarProgressUpdater(private val taskService: TaskService) : InitializingBean {
    private val threadPoolExecutor: Executor
    private val progressUpdateListener: ChangeListener<Number>

    private var taskBarList: ITaskbarList3? = null
    private var taskBarPointer: Pointer<Int>? = null

    init {
        this.threadPoolExecutor = ThreadPoolExecutor(0, 1, 10L, TimeUnit.SECONDS, LinkedBlockingQueue())
        progressUpdateListener = { observable1, oldValue, newValue -> updateTaskbarProgress(newValue.toDouble()) }
    }

    override fun afterPropertiesSet() {
        JavaFxUtil.addListener(taskService.activeWorkers) { observable: Observable -> onActiveTasksChanged() }
    }

    private fun onActiveTasksChanged() {
        val runningTasks = taskService.activeWorkers
        if (runningTasks.isEmpty()) {
            updateTaskbarProgress(null)
        } else {
            val task = runningTasks.iterator().next()
            JavaFxUtil.addListener(task.progressProperty(), WeakChangeListener(progressUpdateListener))
            updateTaskbarProgress(task.progress)
        }
    }

    fun initTaskBar() {
        try {
            threadPoolExecutor.execute { noCatch<ITaskbarList3> { taskBarList = COMRuntime.newInstance(ITaskbarList3::class.java) } }
            val hwndVal = com.sun.jna.Pointer.nativeValue(JavaFxUtil.nativeWindow)
            taskBarPointer = Pointer.pointerToAddress<Any>(hwndVal, null as PointerIO<*>?)
        } catch (e: NoClassDefFoundError) {
            taskBarPointer = null
        }

    }

    private fun updateTaskbarProgress(progress: Double?) {


        threadPoolExecutor.execute {
            if (taskBarPointer == null || taskBarList == null) {
                return@threadPoolExecutor.execute
            }

            if (progress == null) {
                taskBarList!!.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NOPROGRESS)
            } else if (progress == ProgressIndicator.INDETERMINATE_PROGRESS) {
                taskBarList!!.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_INDETERMINATE)
            } else {
                taskBarList!!.SetProgressState(taskBarPointer, ITaskbarList3.TbpFlag.TBPF_NORMAL)
                taskBarList!!.SetProgressValue(taskBarPointer, (progress * 100).toInt().toLong(), 100)
            }
        }
    }
}
