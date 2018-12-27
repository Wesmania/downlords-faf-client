package com.faforever.client.notification

import com.faforever.client.fx.Controller
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.ToastPosition
import com.faforever.client.theme.UiService
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TransientNotificationsController @Inject
constructor(private val uiService: UiService, private val preferencesService: PreferencesService) : Controller<Node> {
    var transientNotificationsRoot: VBox? = null

    override val root: Pane?
        get() = transientNotificationsRoot

    override fun initialize() {
        val toastPosition = preferencesService.preferences!!.notification.toastPosition

        when (toastPosition) {
            ToastPosition.TOP_RIGHT -> transientNotificationsRoot!!.alignment = Pos.TOP_RIGHT
            ToastPosition.BOTTOM_RIGHT -> transientNotificationsRoot!!.alignment = Pos.BOTTOM_RIGHT
            ToastPosition.BOTTOM_LEFT -> transientNotificationsRoot!!.alignment = Pos.BOTTOM_LEFT
            ToastPosition.TOP_LEFT -> transientNotificationsRoot!!.alignment = Pos.TOP_LEFT
            else -> throw AssertionError("Uncovered position: $toastPosition")
        }
    }

    fun addNotification(notification: TransientNotification) {
        val controller = uiService.loadFxml<TransientNotificationController>("theme/transient_notification.fxml")
        controller.setNotification(notification)
        val controllerRoot = controller.root
        transientNotificationsRoot!!.children.add(0, controllerRoot)
    }
}
