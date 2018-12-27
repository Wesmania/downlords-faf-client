package com.faforever.client.chat

import com.faforever.client.chat.avatar.AvatarBean
import com.faforever.client.chat.avatar.AvatarService
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.StringListCell
import com.faforever.client.game.JoinGameHelper
import com.faforever.client.game.PlayerStatus
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.Severity
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.replay.ReplayService
import com.faforever.client.theme.UiService
import com.google.common.eventbus.EventBus
import com.jfoenix.controls.JFXButton
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.control.ColorPicker
import javafx.scene.control.ComboBox
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.lang.invoke.MethodHandles
import java.net.URL
import java.util.Objects

import com.faforever.client.chat.ChatColorMode.CUSTOM
import com.faforever.client.player.SocialStatus.FOE
import com.faforever.client.player.SocialStatus.FRIEND
import com.faforever.client.player.SocialStatus.SELF
import java.util.Locale.US

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class ChatUserContextMenuController(private val preferencesService: PreferencesService,
                                    private val playerService: PlayerService, private val replayService: ReplayService,
                                    private val notificationService: NotificationService, private val i18n: I18n, private val eventBus: EventBus,
                                    private val joinGameHelper: JoinGameHelper, private val avatarService: AvatarService, private val uiService: UiService) : Controller<ContextMenu> {
    var avatarComboBox: ComboBox<AvatarBean>? = null
    var avatarPickerMenuItem: CustomMenuItem? = null
    var sendPrivateMessageItem: MenuItem? = null
    var socialSeparator: SeparatorMenuItem? = null
    var colorPickerMenuItem: CustomMenuItem? = null
    var colorPicker: ColorPicker? = null
    var joinGameItem: MenuItem? = null
    var addFriendItem: MenuItem? = null
    var removeFriendItem: MenuItem? = null
    var addFoeItem: MenuItem? = null
    var removeFoeItem: MenuItem? = null
    var watchGameItem: MenuItem? = null
    var viewReplaysItem: MenuItem? = null
    var inviteItem: MenuItem? = null
    var moderatorActionSeparator: SeparatorMenuItem? = null
    var kickItem: MenuItem? = null
    var banItem: MenuItem? = null
    internal var contextMenu: ContextMenu? = null
        set
    var showUserInfo: MenuItem? = null
    var removeCustomColorButton: JFXButton? = null
    private var chatUser: ChatChannelUser? = null

    private var playerChangeListener: ChangeListener<Player>? = null

    private val player: Player
        get() = chatUser!!.player.orElseThrow { IllegalStateException("No player for chat user:" + chatUser!!) }

    override fun initialize() {
        avatarComboBox!!.setCellFactory { param -> avatarCell() }
        avatarComboBox!!.setButtonCell(avatarCell())
        removeCustomColorButton!!.managedProperty().bind(removeCustomColorButton!!.visibleProperty())

        avatarPickerMenuItem!!.visibleProperty().bind(Bindings.createBooleanBinding({ !avatarComboBox!!.items.isEmpty() }, avatarComboBox!!.items))

        // Workaround for the issue that the popup gets closed when the "custom color" button is clicked, causing an NPE
        // in the custom color popup window.
        colorPicker!!.focusedProperty().addListener { observable, oldValue, newValue -> contextMenu!!.isAutoHide = !newValue }
    }

    private fun avatarCell(): StringListCell<AvatarBean> {
        return StringListCell(
                Function<AvatarBean, String> { it.getDescription() },
                { avatarBean ->
                    val url = avatarBean.url
                    if (url == null) {
                        return null
                    }
                    ImageView(avatarService.loadAvatar(url!!.toString()))
                })
    }

    fun setChatUser(chatUser: ChatChannelUser) {
        this.chatUser = chatUser
        showUserInfo!!.visibleProperty().bind(chatUser.playerProperty().isNotNull)

        val chatPrefs = preferencesService.preferences!!.chat

        val lowerCaseUsername = chatUser.username.toLowerCase(US)
        colorPicker!!.value = (chatPrefs.userToColor as java.util.Map<String, Color>).getOrDefault(lowerCaseUsername, null)

        colorPicker!!.valueProperty().addListener { observable, oldValue, newValue ->
            val lowerUsername = chatUser.username.toLowerCase(US)
            if (newValue == null) {
                chatPrefs.userToColor.remove(lowerUsername)
            } else {
                chatPrefs.userToColor[lowerUsername] = newValue
            }
            chatUser.color = newValue
            contextMenu!!.hide()
        }

        removeCustomColorButton!!.visibleProperty().bind(chatPrefs.chatColorModeProperty().isEqualTo(CUSTOM)
                .and(colorPicker!!.valueProperty().isNotNull))
        colorPickerMenuItem!!.visibleProperty().bind(chatPrefs.chatColorModeProperty()
                .isEqualTo(CUSTOM))


        playerChangeListener = { observable, oldValue, newValue ->
            if (newValue == null) {
                return
            }

            if (newValue!!.getSocialStatus() == SELF) {
                loadAvailableAvatars(newValue)
            }

            kickItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF))
            banItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF))
            moderatorActionSeparator!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF))
            sendPrivateMessageItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF))
            addFriendItem!!.visibleProperty().bind(
                    newValue!!.socialStatusProperty().isNotEqualTo(FRIEND).and(newValue!!.socialStatusProperty().isNotEqualTo(SELF))
            )
            removeFriendItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isEqualTo(FRIEND))
            addFoeItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(FOE).and(newValue!!.socialStatusProperty().isNotEqualTo(SELF)))
            removeFoeItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isEqualTo(FOE))

            joinGameItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF)
                    .and(newValue!!.statusProperty().isEqualTo(PlayerStatus.LOBBYING)
                            .or(newValue!!.statusProperty().isEqualTo(PlayerStatus.HOSTING))))
            watchGameItem!!.visibleProperty().bind(newValue!!.statusProperty().isEqualTo(PlayerStatus.PLAYING))
            inviteItem!!.visibleProperty().bind(newValue!!.socialStatusProperty().isNotEqualTo(SELF)
                    .and(newValue!!.statusProperty().isNotEqualTo(PlayerStatus.PLAYING)))
        }
        JavaFxUtil.addListener(chatUser.playerProperty(), WeakChangeListener(playerChangeListener!!))
        playerChangeListener!!.changed(chatUser.playerProperty(), null, chatUser.player.orElse(null))

        socialSeparator!!.visibleProperty().bind(addFriendItem!!.visibleProperty().or(
                removeFriendItem!!.visibleProperty().or(
                        addFoeItem!!.visibleProperty().or(
                                removeFoeItem!!.visibleProperty()))))
    }

    private fun loadAvailableAvatars(player: Player?) {
        avatarService.availableAvatars.thenAccept { avatars ->
            val items = FXCollections.observableArrayList(avatars)
            items.add(0, AvatarBean(null, i18n.get("chat.userContext.noAvatar")))

            val currentAvatarUrl = player!!.avatarUrl
            Platform.runLater {
                avatarComboBox!!.items.setAll(items)
                avatarComboBox!!.selectionModel.select(items.stream()
                        .filter { avatarBean -> Objects.toString(avatarBean.url, null) == currentAvatarUrl }
                        .findFirst()
                        .orElse(null))

                // Only after the box has been populated and we selected the current value, we add the listener.
                // Otherwise the code above already triggers a changeAvatar()
                avatarComboBox!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue ->
                    player.avatarTooltip = newValue?.description
                    player.avatarUrl = if (newValue == null) null else Objects.toString(newValue.url, null)
                    avatarService.changeAvatar(newValue)
                }
            }
        }
    }

    fun onShowUserInfoSelected() {
        val userInfoWindowController = uiService.loadFxml<UserInfoWindowController>("theme/user_info_window.fxml")
        userInfoWindowController.setPlayer(chatUser!!.player.orElseThrow { IllegalStateException("No player for chat user: " + chatUser!!) })
        userInfoWindowController.setOwnerWindow(contextMenu!!.ownerWindow)
        userInfoWindowController.show()
    }

    fun onSendPrivateMessageSelected() {
        eventBus.post(InitiatePrivateChatEvent(chatUser!!.username))
    }

    fun onCopyUsernameSelected() {
        val clipboardContent = ClipboardContent()
        clipboardContent.putString(chatUser!!.username)
        Clipboard.getSystemClipboard().setContent(clipboardContent)
    }

    fun onAddFriendSelected() {
        val player = player
        if (player.socialStatus == FOE) {
            playerService.removeFoe(player)
        }
        playerService.addFriend(player)
    }

    fun onRemoveFriendSelected() {
        val player = player
        playerService.removeFriend(player)
    }

    fun onAddFoeSelected() {
        val player = player
        if (player.socialStatus == FRIEND) {
            playerService.removeFriend(player)
        }
        playerService.addFoe(player)
    }

    fun onRemoveFoeSelected() {
        val player = player
        playerService.removeFoe(player)
    }

    fun onWatchGameSelected() {
        val player = player
        try {
            replayService.runLiveReplay(player.game.id, player.id)
        } catch (e: Exception) {
            logger.error("Cannot display live replay {}", e.cause)
            val title = i18n.get("replays.live.loadFailure.title")
            val message = i18n.get("replays.live.loadFailure.message")
            notificationService.addNotification(ImmediateNotification(title, message, Severity.ERROR))
        }

    }

    fun onViewReplaysSelected() {
        // FIXME implement
    }

    fun onInviteToGameSelected() {
        //FIXME implement
    }

    fun onKickSelected() {
        // FIXME implement
    }

    fun onBanSelected() {
        // FIXME implement
    }

    fun onJoinGameSelected() {
        val player = player
        joinGameHelper.join(player.game)
    }

    fun onRemoveCustomColor() {
        colorPicker!!.value = null
    }

    override fun getRoot(): ContextMenu? {
        return contextMenu
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
