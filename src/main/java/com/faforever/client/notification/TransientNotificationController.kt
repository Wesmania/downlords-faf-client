package com.faforever.client.notification

import com.faforever.client.fx.Controller
import com.faforever.client.notification.Action.ActionCallback
import com.faforever.client.preferences.PreferencesService
import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.beans.value.ChangeListener
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.shape.Rectangle
import javafx.util.Duration
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.util.Objects

import javafx.util.Duration.millis

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TransientNotificationController @Inject
constructor(private val preferencesService: PreferencesService) : Controller<Node> {
    var transientNotificationRoot: Pane? = null
    var messageLabel: Label? = null
    var titleLabel: Label? = null
    var imageView: ImageView? = null
    private var animationListener: ChangeListener<Number>? = null
    private var action: ActionCallback? = null
    private var timeline: Timeline? = null
    private var toastDisplayTime: Int = 0

    override val root: Region?
        get() = transientNotificationRoot

    override fun initialize() {
        val rectangle = Rectangle()
        rectangle.widthProperty().bind(transientNotificationRoot!!.widthProperty())
        rectangle.heightProperty().bind(transientNotificationRoot!!.heightProperty())

        // Wait until the height is known, then animate it
        this.animationListener = { observable, oldValue, newValue ->
            if (newValue != null) {
                observable.removeListener(animationListener)
                transientNotificationRoot!!.maxHeight = 0.0
                transientNotificationRoot!!.isVisible = true
                this@TransientNotificationController.animate(newValue)
            }
        }

        transientNotificationRoot!!.isVisible = false
        transientNotificationRoot!!.clip = rectangle
        transientNotificationRoot!!.heightProperty().addListener(animationListener)

        // Divided by two because it's used in a cycle
        toastDisplayTime = preferencesService.preferences!!.notification.toastDisplayTime / 2

        transientNotificationRoot!!.setOnMouseEntered { event -> timeline!!.pause() }
        transientNotificationRoot!!.setOnMouseExited { event -> timeline!!.playFrom(Duration.millis((300 + toastDisplayTime).toDouble())) }
    }

    private fun animate(height: Number?) {
        timeline = Timeline()
        timeline!!.isAutoReverse = true
        timeline!!.cycleCount = 2
        timeline!!.keyFrames.addAll(
                KeyFrame(millis(300.0), KeyValue(transientNotificationRoot!!.maxHeightProperty(), height, Interpolator.LINEAR)),
                KeyFrame(millis((300 + toastDisplayTime).toDouble()), KeyValue(transientNotificationRoot!!.maxHeightProperty(), height))
        )
        timeline!!.setOnFinished { event -> dismiss() }
        timeline!!.playFromStart()
    }

    private fun dismiss() {
        Objects.requireNonNull<Timeline>(timeline, "timeline has not been set")
        timeline!!.stop()
        val parent = transientNotificationRoot!!.parent as Pane ?: return
        parent.children.remove(transientNotificationRoot)
    }

    fun setNotification(notification: TransientNotification) {
        titleLabel!!.text = notification.title
        messageLabel!!.text = notification.text
        imageView!!.image = notification.image
        action = notification.actionCallback
    }

    fun onCloseButtonClicked() {
        dismiss()
    }

    fun onClicked(event: MouseEvent) {
        if (event.button == MouseButton.SECONDARY) {
            dismiss()
        } else {
            action!!.call(event)
        }
    }
}
