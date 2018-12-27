package com.faforever.client.chat

import com.faforever.client.audio.AudioService
import com.faforever.client.chat.event.UnreadPrivateMessageEvent
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.uploader.ImageUploadService
import com.faforever.client.user.UserService
import com.faforever.client.util.TimeService
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.scene.control.ScrollPane
import javafx.scene.control.Tab
import javafx.scene.control.TextInputControl
import javafx.scene.web.WebView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.time.Instant
import java.util.Optional

import com.faforever.client.player.SocialStatus.FOE

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class PrivateChatTabController @Inject
constructor(userService: UserService,
            preferencesService: PreferencesService,
            playerService: PlayerService,
            timeService: TimeService,
            i18n: I18n,
            imageUploadService: ImageUploadService,
            notificationService: NotificationService,
            reportingService: ReportingService,
            uiService: UiService,
            autoCompletionHelper: AutoCompletionHelper,
            eventBus: EventBus,
            audioService: AudioService,
            chatService: ChatService,
            webViewConfigurer: WebViewConfigurer,
            countryFlagService: CountryFlagService)// TODO cut dependencies
    : AbstractChatTabController(webViewConfigurer, userService, chatService, preferencesService, playerService, audioService, timeService, i18n, imageUploadService, notificationService, reportingService, uiService, autoCompletionHelper, eventBus, countryFlagService) {

    override var root: Tab? = null
    protected override var messagesWebView: WebView? = null
        set
    var messageTextField: TextInputControl? = null
    var privateUserInfoController: PrivateUserInfoController? = null
    var gameDetailScrollPane: ScrollPane? = null

    internal var isUserOffline: Boolean = false
        private set

    override var receiver: String
        get() = super.receiver
        set(username) {
            super.receiver = username
            root!!.id = username
            root!!.text = username

            val chatUser = chatService.getOrCreateChatUser(username, username, false)
            privateUserInfoController!!.setChatUser(chatUser)
        }

    override fun initialize() {
        super.initialize()
        JavaFxUtil.fixScrollSpeed(gameDetailScrollPane!!)
        isUserOffline = false
        chatService.addChatUsersByNameListener { change ->
            if (change.wasRemoved()) {
                onPlayerDisconnected(change.key)
            }
            if (change.wasAdded()) {
                onPlayerConnected(change.key)
            }
        }
    }

    override fun messageTextField(): TextInputControl? {
        return messageTextField
    }

    public override fun onChatMessage(chatMessage: ChatMessage) {
        val playerOptional = playerService.getPlayerForUsername(chatMessage.getUsername())
        val chatPrefs = preferencesService.preferences!!.chat

        if (playerOptional.isPresent && playerOptional.get().socialStatus == FOE && chatPrefs.hideFoeMessages) {
            return
        }

        super.onChatMessage(chatMessage)

        if (!hasFocus()) {
            audioService.playPrivateMessageSound()
            showNotificationIfNecessary(chatMessage)
            setUnread(true)
            incrementUnreadMessagesCount(1)
            eventBus.post(UnreadPrivateMessageEvent(chatMessage))
        }
    }

    @VisibleForTesting
    internal fun onPlayerDisconnected(userName: String) {
        if (userName != receiver) {
            return
        }
        isUserOffline = true
        Platform.runLater { onChatMessage(ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerLeft", userName), true)) }
    }

    @VisibleForTesting
    internal fun onPlayerConnected(userName: String) {
        if (!isUserOffline || userName != receiver) {
            return
        }
        isUserOffline = false
        Platform.runLater { onChatMessage(ChatMessage(userName, Instant.now(), i18n.get("chat.operator") + ":", i18n.get("chat.privateMessage.playerReconnect", userName), true)) }
    }
}
