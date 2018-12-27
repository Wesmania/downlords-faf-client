package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.theme.UiService
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class EnterPasswordController @Inject
constructor(private val uiService: UiService) : Controller<Node> {
    var loginErrorLabel: Label? = null
    var titleLabel: Label? = null
    var passwordField: TextField? = null
    var buttonBar: ButtonBar? = null
    override var root: Parent? = null
    var joinButton: Button? = null
    var cancelButton: Button? = null
    private var listener: OnPasswordEnteredListener? = null
    private var game: Game? = null
    private var ignoreRating: Boolean = false

    override fun initialize() {
        loginErrorLabel!!.isVisible = false // ToDo: manage negative logins
        loginErrorLabel!!.managedProperty().bind(loginErrorLabel!!.visibleProperty())
        joinButton!!.disableProperty().bind(passwordField!!.textProperty().isEmpty)
    }

    internal fun setOnPasswordEnteredListener(listener: OnPasswordEnteredListener) {
        if (this.listener != null) {
            throw IllegalStateException("Listener has already been set")
        }
        this.listener = listener
    }

    fun onJoinButtonClicked() {
        if (listener == null) {
            throw IllegalStateException("No listener has been set")
        }
        listener!!.onPasswordEntered(game, passwordField!!.text, ignoreRating)
        root!!.scene.window.hide()
    }

    fun onCancelButtonClicked() {
        root!!.scene.window.hide()
    }

    fun setGame(game: Game) {
        this.game = game
    }

    fun setIgnoreRating(ignoreRating: Boolean) {
        this.ignoreRating = ignoreRating
    }

    fun showPasswordDialog(owner: Window) {
        val userInfoWindow = Stage(StageStyle.TRANSPARENT)
        userInfoWindow.initModality(Modality.NONE)
        userInfoWindow.initOwner(owner)

        val scene = uiService.createScene(userInfoWindow, root)
        userInfoWindow.scene = scene
        userInfoWindow.show()
    }

    internal interface OnPasswordEnteredListener {

        fun onPasswordEntered(game: Game?, password: String, ignoreRating: Boolean)
    }
}
