package com.faforever.client.login

import com.faforever.client.config.ClientProperties
import com.faforever.client.config.ClientProperties.Replay
import com.faforever.client.config.ClientProperties.Server
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.preferences.LoginPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.update.ClientConfiguration.Endpoints
import com.faforever.client.user.UserService
import com.google.common.base.Strings
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import lombok.extern.slf4j.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.util.concurrent.CancellationException

import com.google.common.base.Strings.isNullOrEmpty

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class LoginController(
        private val userService: UserService,
        private val preferencesService: PreferencesService,
        private val platformService: PlatformService,
        private val clientProperties: ClientProperties
) : Controller<Node> {

    var loginFormPane: Pane? = null
    var loginProgressPane: Pane? = null
    var autoLoginCheckBox: CheckBox? = null
    var usernameInput: TextField? = null
    var passwordInput: TextField? = null
    var loginButton: Button? = null
    var loginErrorLabel: Label? = null
    override var root: Pane? = null
    var serverConfigPane: GridPane? = null
    var serverHostField: TextField? = null
    var serverPortField: TextField? = null
    var replayServerHostField: TextField? = null
    var replayServerPortField: TextField? = null
    var apiBaseUrlField: TextField? = null

    override fun initialize() {
        loginErrorLabel!!.managedProperty().bind(loginErrorLabel!!.visibleProperty())
        loginErrorLabel!!.isVisible = false

        loginFormPane!!.managedProperty().bind(loginFormPane!!.visibleProperty())

        loginProgressPane!!.managedProperty().bind(loginProgressPane!!.visibleProperty())
        loginProgressPane!!.isVisible = false

        serverConfigPane!!.managedProperty().bind(serverConfigPane!!.visibleProperty())
        serverConfigPane!!.isVisible = false

        populateEndpointFields(
                clientProperties.getServer().getHost(),
                clientProperties.getServer().getPort(),
                clientProperties.getReplay().getRemoteHost(),
                clientProperties.getReplay().getRemotePort(),
                clientProperties.getApi().getBaseUrl()
        )

        preferencesService.remotePreferences.thenAccept { clientConfiguration ->
            val defaultEndpoint = clientConfiguration.getEndpoints().get(0)
            populateEndpointFields(
                    defaultEndpoint.getLobby().getHost(),
                    clientConfiguration.getEndpoints().get(0).getLobby().getPort(),
                    clientConfiguration.getEndpoints().get(0).getLiveReplay().getHost(),
                    clientConfiguration.getEndpoints().get(0).getLiveReplay().getPort(),
                    clientConfiguration.getEndpoints().get(0).getApi().getUrl()
            )
        }.exceptionally { throwable ->
            log.warn("Could not read remote preferences")
            null
        }
    }

    private fun populateEndpointFields(
            serverHost: String,
            serverPort: Int,
            replayServerHost: String,
            replayServerPort: Int,
            apiBaseUrl: String
    ) {
        JavaFxUtil.runLater {
            serverHostField!!.text = serverHost
            serverPortField!!.text = serverPort.toString()
            replayServerHostField!!.text = replayServerHost
            replayServerPortField!!.text = replayServerPort.toString()
            apiBaseUrlField!!.text = apiBaseUrl
        }
    }

    fun display() {
        setShowLoginProgress(false)

        val loginPrefs = preferencesService.preferences!!.login
        val username = loginPrefs.username
        val password = loginPrefs.password
        val isAutoLogin = loginPrefs.autoLogin

        // Fill the form even if autoLogin is true, since user may cancel the login
        usernameInput!!.text = Strings.nullToEmpty(username)
        autoLoginCheckBox!!.isSelected = isAutoLogin

        if (loginPrefs.autoLogin && !isNullOrEmpty(username) && !isNullOrEmpty(password)) {
            login(username, password, true)
        } else if (isNullOrEmpty(username)) {
            usernameInput!!.requestFocus()
        } else {
            passwordInput!!.requestFocus()
        }
    }

    private fun setShowLoginProgress(show: Boolean) {
        loginFormPane!!.isVisible = !show
        loginProgressPane!!.isVisible = show
        loginButton!!.isDisable = show
    }

    private fun login(username: String, password: String, autoLogin: Boolean) {
        setShowLoginProgress(true)

        userService.login(username, password, autoLogin)!!
                .exceptionally { throwable ->
                    onLoginFailed(throwable)
                    null
                }
    }

    private fun onLoginFailed(e: Throwable) {
        logger.warn("Login failed", e)
        Platform.runLater {
            if (e is CancellationException) {
                loginErrorLabel!!.isVisible = false
            } else {
                if (e is LoginFailedException) {
                    loginErrorLabel!!.text = e.message
                } else {
                    loginErrorLabel!!.text = e.cause.getLocalizedMessage()
                }
                loginErrorLabel!!.isVisible = true
            }

            setShowLoginProgress(false)
        }
    }

    fun onLoginButtonClicked() {
        val username = usernameInput!!.text
        val password = passwordInput!!.text

        val autoLogin = autoLoginCheckBox!!.isSelected

        val server = clientProperties.getServer()
        server.setHost(serverHostField!!.text)
        server.setPort(Integer.parseInt(serverPortField!!.text))

        val replay = clientProperties.getReplay()
        replay.setRemoteHost(replayServerHostField!!.text)
        replay.setRemotePort(Integer.parseInt(replayServerPortField!!.text))

        clientProperties.getApi().setBaseUrl(apiBaseUrlField!!.text)

        login(username, password, autoLogin)
    }

    fun onCancelLoginButtonClicked() {
        userService.cancelLogin()
        setShowLoginProgress(false)
    }

    fun forgotLoginClicked() {
        platformService.showDocument(clientProperties.getWebsite().getForgotPasswordUrl())
    }

    fun createNewAccountClicked() {
        platformService.showDocument(clientProperties.getWebsite().getCreateAccountUrl())
    }

    fun onMouseClicked(event: MouseEvent) {
        if (event.button == MouseButton.PRIMARY && event.clickCount == 2) {
            serverConfigPane!!.isVisible = true
        }
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
