package com.faforever.client.notification

import com.faforever.client.fx.Controller
import com.jfoenix.controls.JFXButton
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.ArrayList

/**
 * Controller for a single persistent notification entry.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PersistentNotificationController @Inject
constructor(private val notificationService: NotificationService) : Controller<Node> {
    override var root: Node? = null
    var messageLabel: Label? = null
    var iconLabel: Label? = null
    var actionButtonsContainer: HBox? = null
    private var notification: PersistentNotification? = null

    /**
     * Sets the notification to display. Populates corresponding UI elements.
     */
    fun setNotification(notification: PersistentNotification) {
        this.notification = notification
        messageLabel!!.text = notification.text
        setImageBasedOnSeverity(notification.severity)
        setActions(notification.actions)
    }

    private fun setImageBasedOnSeverity(severity: Severity) {
        val styleClasses = iconLabel!!.styleClass
        styleClasses.removeAll(CSS_STYLE_INFO, CSS_STYLE_WARN, CSS_STYLE_ERROR)

        when (severity) {
            Severity.INFO -> {
                iconLabel!!.text = "\uE88F"
                styleClasses.add(CSS_STYLE_INFO)
            }
            Severity.WARN -> {
                iconLabel!!.text = "\uE002"
                styleClasses.add(CSS_STYLE_WARN)
            }
            Severity.ERROR -> {
                iconLabel!!.text = "\uE001"
                styleClasses.add(CSS_STYLE_ERROR)
            }
            else -> throw IllegalStateException("Unhandled severity: $severity")
        }
        iconLabel!!.labelFor = iconLabel
    }

    private fun setActions(actions: List<Action>?) {
        if (actions == null) {
            return
        }

        val actionButtons = ArrayList<Button>()
        for (action in actions) {
            val button = JFXButton(action.title)
            button.isFocusTraversable = false
            button.setOnAction { event ->
                action.call(event)
                if (action.type == Action.Type.OK_DONE) {
                    dismiss()
                }
            }

            actionButtons.add(button)
        }

        actionButtonsContainer!!.children.setAll(actionButtons)
    }

    private fun dismiss() {
        notificationService.removeNotification(notification)
    }

    fun onCloseButtonClicked() {
        dismiss()
    }

    companion object {

        private val CSS_STYLE_INFO = "info"
        private val CSS_STYLE_WARN = "warn"
        private val CSS_STYLE_ERROR = "error"
    }
}
