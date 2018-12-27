package com.faforever.client.notification

import com.faforever.client.audio.AudioService
import com.faforever.client.fx.Controller
import com.faforever.client.theme.UiService
import javafx.application.Platform
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.HashMap

/**
 * Controller for pane that displays all persistent notifications.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PersistentNotificationsController @Inject
constructor(private val notificationService: NotificationService, private val audioService: AudioService, private val uiService: UiService) : Controller<Node> {

    private val notificationsToNode: MutableMap<PersistentNotification, Node>
    var noNotificationsLabel: Label? = null
    var persistentNotificationsRoot: Pane? = null

    override val root: Node?
        get() = persistentNotificationsRoot

    init {

        notificationsToNode = HashMap()
    }

    override fun initialize() {
        addNotifications(notificationService.persistentNotifications)
        notificationService.addPersistentNotificationListener { change ->
            if (change.wasAdded()) {
                val addedNotifications = change.elementAdded
                addNotification(addedNotifications)
            } else {
                removeNotification(change.elementRemoved)
            }
        }
    }

    private fun addNotifications(persistentNotifications: Set<PersistentNotification>) {
        persistentNotifications.forEach(Consumer<PersistentNotification> { this.addNotification(it) })
    }

    private fun addNotification(notification: PersistentNotification) {
        val controller = uiService.loadFxml<PersistentNotificationController>("theme/persistent_notification.fxml")
        controller.setNotification(notification)

        notificationsToNode[notification] = controller.root

        Platform.runLater {
            val children = persistentNotificationsRoot!!.children
            children.remove(noNotificationsLabel)
            children.add(controller.root)

            playNotificationSound(notification)
        }
    }

    private fun removeNotification(removedNotifications: PersistentNotification) {
        val children = persistentNotificationsRoot!!.children
        children.remove(notificationsToNode[removedNotifications])

        if (children.isEmpty()) {
            children.setAll(noNotificationsLabel)
        }
    }

    private fun playNotificationSound(notification: PersistentNotification) {
        when (notification.severity) {
            Severity.INFO -> audioService.playInfoNotificationSound()

            Severity.WARN -> audioService.playWarnNotificationSound()

            Severity.ERROR -> audioService.playErrorNotificationSound()
        }
    }

}
