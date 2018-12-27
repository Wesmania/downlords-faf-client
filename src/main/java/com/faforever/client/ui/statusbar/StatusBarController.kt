package com.faforever.client.ui.statusbar

import com.faforever.client.chat.ChatService
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.remote.FafService
import com.faforever.client.task.TaskService
import com.faforever.client.update.Version
import com.google.common.base.Strings
import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.concurrent.Worker
import javafx.css.PseudoClass
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.ProgressBar
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

import javafx.application.Platform.runLater

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class StatusBarController @Inject
constructor(private val fafService: FafService, private val i18n: I18n, private val chatService: ChatService, private val taskService: TaskService) : Controller<Node> {

    var chatConnectionStatusIcon: Label? = null
    var fafConnectionStatusIcon: Label? = null
    var fafConnectionButton: MenuButton? = null
    var chatConnectionButton: MenuButton? = null
    var taskProgressBar: ProgressBar? = null
    var taskPane: Pane? = null
    var taskProgressLabel: Label? = null
    var versionLabel: Label? = null

    override val root: Node?
        get() = null

    override fun initialize() {
        setCurrentWorkerInStatusBar(null)
        versionLabel!!.text = Version.VERSION

        JavaFxUtil.addListener<ConnectionState>(fafService.connectionStateProperty()) { observable, oldValue, newValue ->
            runLater {
                when (newValue) {
                    ConnectionState.DISCONNECTED -> {
                        fafConnectionButton!!.text = i18n.get("statusBar.fafDisconnected")
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false)
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true)
                    }
                    ConnectionState.CONNECTING -> {
                        fafConnectionButton!!.text = i18n.get("statusBar.fafConnecting")
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false)
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false)
                    }
                    ConnectionState.CONNECTED -> {
                        fafConnectionButton!!.text = i18n.get("statusBar.fafConnected")
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true)
                        fafConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false)
                    }
                }
            }
        }

        JavaFxUtil.addListener<ConnectionState>(chatService.connectionStateProperty()) { observable, oldValue, newValue ->
            runLater {
                chatConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, false)
                chatConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, false)
                when (newValue) {
                    ConnectionState.DISCONNECTED -> {
                        chatConnectionButton!!.text = i18n.get("statusBar.chatDisconnected")
                        chatConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS, true)
                    }
                    ConnectionState.CONNECTING -> chatConnectionButton!!.text = i18n.get("statusBar.chatConnecting")
                    ConnectionState.CONNECTED -> {
                        chatConnectionButton!!.text = i18n.get("statusBar.chatConnected")
                        chatConnectionStatusIcon!!.pseudoClassStateChanged(CONNECTIVITY_CONNECTED_PSEUDO_CLASS, true)
                    }
                }
            }
        }

        JavaFxUtil.addListener(taskService.activeWorkers) { observable: Observable ->
            val runningWorkers = taskService.activeWorkers
            if (runningWorkers.isEmpty()) {
                setCurrentWorkerInStatusBar(null)
            } else {
                setCurrentWorkerInStatusBar(runningWorkers.iterator().next())
            }
        }
    }

    /**
     * @param worker the task to set, `null` to unset
     */
    private fun setCurrentWorkerInStatusBar(worker: Worker<*>?) {
        runLater {
            if (worker == null) {
                taskPane!!.isVisible = false
                taskProgressBar!!.progressProperty().unbind()
                taskProgressLabel!!.textProperty().unbind()
                return@runLater
            }

            taskPane!!.isVisible = true
            taskProgressBar!!.progressProperty().bind(worker.progressProperty())
            taskProgressLabel!!.textProperty().bind(Bindings.createStringBinding(
                    {
                        val message = worker.message
                        val title = worker.title
                        if (Strings.isNullOrEmpty(message)) {
                            return@Bindings.createStringBinding i18n . get "statusBar.taskWithoutMessage.format", title)
                        }
                        i18n.get("statusBar.taskWithMessage.format", title, message)
                    },
                    worker.titleProperty(), worker.messageProperty()
            ))
        }
    }

    fun onFafReconnectClicked() {
        fafService.reconnect()
    }

    fun onChatReconnectClicked() {
        chatService.reconnect()
    }

    companion object {
        private val CONNECTIVITY_CONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("connected")
        private val CONNECTIVITY_DISCONNECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("disconnected")
    }
}
