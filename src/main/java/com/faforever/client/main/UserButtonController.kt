package com.faforever.client.main

import com.faforever.client.chat.UserInfoWindowController
import com.faforever.client.fx.Controller
import com.faforever.client.player.PlayerService
import com.faforever.client.theme.UiService
import com.faforever.client.user.event.LogOutRequestEvent
import com.faforever.client.user.event.LoginSuccessEvent
import com.faforever.client.util.IdenticonUtil
import com.google.common.eventbus.EventBus
import com.google.common.eventbus.Subscribe
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.MenuButton
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class UserButtonController @Inject
constructor(private val eventBus: EventBus, private val playerService: PlayerService, private val uiService: UiService) : Controller<Node> {
    var userButtonRoot: MenuButton? = null
    var userImageView: ImageView? = null

    override val root: Node?
        get() = userButtonRoot

    override fun initialize() {
        eventBus.register(this)
    }

    @Subscribe
    fun onLoginSuccessEvent(event: LoginSuccessEvent) {
        Platform.runLater {
            userButtonRoot!!.text = event.username
            userImageView!!.image = IdenticonUtil.createIdenticon(event.userId)
        }
    }

    fun onShowProfile(event: ActionEvent) {
        val userInfoWindowController = uiService.loadFxml<UserInfoWindowController>("theme/user_info_window.fxml")
        userInfoWindowController.setPlayer(playerService.currentPlayer.orElseThrow { IllegalStateException("Player has not been set") })
        userInfoWindowController.setOwnerWindow(userButtonRoot!!.scene.window)
        userInfoWindowController.show()
    }

    fun onLogOut(actionEvent: ActionEvent) {
        eventBus.post(LogOutRequestEvent())
    }
}
