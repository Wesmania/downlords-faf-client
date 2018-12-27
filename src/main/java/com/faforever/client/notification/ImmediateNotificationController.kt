package com.faforever.client.notification

import com.faforever.client.fx.Controller
import com.faforever.client.fx.WebViewConfigurer
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXDialogLayout
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.web.WebView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.io.PrintWriter
import java.io.StringWriter
import java.util.Optional
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ImmediateNotificationController(private val webViewConfigurer: WebViewConfigurer) : Controller<Node> {
    var immediateNotificationRoot: Pane? = null
    var errorMessageView: WebView? = null
    var exceptionAreaTitleLabel: Label? = null
    var exceptionTextArea: TextArea? = null

    val jfxDialogLayout: JFXDialogLayout
    private var closeListener: Runnable? = null

    override val root: Region?
        get() = immediateNotificationRoot

    init {
        jfxDialogLayout = JFXDialogLayout()
    }

    override fun initialize() {
        exceptionAreaTitleLabel!!.managedProperty().bind(exceptionAreaTitleLabel!!.visibleProperty())
        exceptionAreaTitleLabel!!.visibleProperty().bind(exceptionTextArea!!.visibleProperty())
        exceptionTextArea!!.managedProperty().bind(exceptionTextArea!!.visibleProperty())
        webViewConfigurer.configureWebView(errorMessageView)

        jfxDialogLayout.setBody(immediateNotificationRoot)
    }

    fun setNotification(notification: ImmediateNotification): ImmediateNotificationController {
        val writer = StringWriter()
        val throwable = notification.throwable
        if (throwable != null) {
            throwable.printStackTrace(PrintWriter(writer))
            exceptionTextArea!!.isVisible = true
            exceptionTextArea!!.text = writer.toString()
        } else {
            exceptionTextArea!!.isVisible = false
        }

        jfxDialogLayout.setHeading(Label(notification.title))
        Platform.runLater { errorMessageView!!.engine.loadContent(notification.text) }

        Optional.ofNullable(notification.actions)
                .map { actions -> actions.stream().map<Button>(Function<Action, Button> { this.createButton(it) }).collect<List<Button>, Any>(Collectors.toList()) }
                .ifPresent { buttons -> jfxDialogLayout.setActions(buttons) }

        return this
    }

    private fun createButton(action: Action): Button {
        val button = JFXButton(action.title)
        button.setOnAction { event ->
            action.call(event)
            if (action.type == Action.Type.OK_DONE) {
                dismiss()
            }
        }

        when (action.type) {
            Action.Type.OK_DONE -> {
                button.styleClass.add("dialog-accept")
                ButtonBar.setButtonData(button, ButtonBar.ButtonData.OK_DONE)
            }
        }

        // Until implemented
        if (action is ReportAction) {
            button.isDisable = true
        }

        return button
    }

    private fun dismiss() {
        closeListener!!.run()
    }

    fun setCloseListener(closeListener: Runnable): ImmediateNotificationController {
        this.closeListener = closeListener
        return this
    }
}
