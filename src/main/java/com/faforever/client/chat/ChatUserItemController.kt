package com.faforever.client.chat

import com.faforever.client.chat.avatar.AvatarService
import com.faforever.client.clan.Clan
import com.faforever.client.clan.ClanService
import com.faforever.client.clan.ClanTooltipController
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.PlatformService
import com.faforever.client.game.PlayerStatus
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.ChatPrefs
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.theme.UiService
import com.faforever.client.util.TimeService
import com.google.common.annotations.VisibleForTesting
import com.google.common.base.Strings
import com.google.common.eventbus.EventBus
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.WeakChangeListener
import javafx.collections.MapChangeListener
import javafx.collections.WeakMapChangeListener
import javafx.css.PseudoClass
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.Tooltip
import javafx.scene.image.ImageView
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.PopupWindow
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils

import java.time.Duration
import java.util.Optional

import com.faforever.client.chat.ChatColorMode.CUSTOM
import com.faforever.client.game.PlayerStatus.IDLE
import com.faforever.client.player.SocialStatus.SELF
import com.faforever.client.util.RatingUtil.getGlobalRating
import com.faforever.client.util.RatingUtil.getLeaderboardRating
import java.time.Instant.now
import java.util.Locale.US

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
// TODO null safety for "player"
class ChatUserItemController// TODO reduce dependencies, rely on eventBus instead
(private val preferencesService: PreferencesService, private val avatarService: AvatarService,
 private val countryFlagService: CountryFlagService,
 private val i18n: I18n, private val uiService: UiService, private val eventBus: EventBus,
 private val clanService: ClanService, private val playerService: PlayerService,
 private val platformService: PlatformService, private val timeService: TimeService) : Controller<Node> {
    private val colorChangeListener: InvalidationListener
    private val formatChangeListener: InvalidationListener
    private val colorPerUserMapChangeListener: MapChangeListener<String, Color>
    private val avatarChangeListener: ChangeListener<String>
    private val clanChangeListener: ChangeListener<String>
    private val countryChangeListener: ChangeListener<String>
    private val gameStatusChangeListener: ChangeListener<PlayerStatus>
    private val playerChangeListener: ChangeListener<Player>
    private val userActivityListener: InvalidationListener
    private val usernameInvalidationListener: InvalidationListener
    private val weakPlayerChangeListener: WeakChangeListener<Player>
    private val weakUsernameInvalidationListener: WeakInvalidationListener
    private val weakColorInvalidationListener: WeakInvalidationListener
    private val weakFormatInvalidationListener: WeakInvalidationListener
    private val weakUserActivityListener: WeakInvalidationListener
    private val weakGameStatusListener: WeakChangeListener<PlayerStatus>
    private val weakAvatarChangeListener: WeakChangeListener<String>
    private val weakClanChangeListener: WeakChangeListener<String>
    private val weakCountryChangeListener: WeakChangeListener<String>

    override var root: Pane? = null
    var countryImageView: ImageView? = null
    var avatarImageView: ImageView? = null
    var usernameLabel: Label? = null
    var clanMenu: MenuButton? = null
    var statusLabel: Label? = null
    var presenceStatusIndicator: Text? = null
    var chatUser: ChatChannelUser? = null
        set(chatUser) {
            if (this.chatUser === chatUser) {
                return
            }

            if (this.chatUser != null) {
                removeListeners(this.chatUser!!)
            }

            field = chatUser
            if (this.chatUser != null) {
                addListeners(this.chatUser!!)
            }
        }
    private var randomColorsAllowed: Boolean = false
    @VisibleForTesting
    var countryTooltip: Tooltip
    @VisibleForTesting
    var avatarTooltip: Tooltip
    @VisibleForTesting
    var userTooltip: Tooltip? = null

    init {

        val chatPrefs = preferencesService.preferences!!.chat
        colorChangeListener = { observable -> updateColor() }
        formatChangeListener = { observable -> updateFormat() }
        weakColorInvalidationListener = WeakInvalidationListener(colorChangeListener)
        weakFormatInvalidationListener = WeakInvalidationListener(formatChangeListener)

        JavaFxUtil.addListener(chatPrefs.chatColorModeProperty(), weakColorInvalidationListener)
        JavaFxUtil.addListener(chatPrefs.chatFormatProperty(), weakFormatInvalidationListener)

        colorPerUserMapChangeListener = { change ->
            val lowerUsername = this.chatUser!!.username.toLowerCase(US)
            if (lowerUsername.equals(change.getKey(), ignoreCase = true)) {
                val newColor = chatPrefs.userToColor[lowerUsername]
                assignColor(newColor)
            }
        }
        userActivityListener = { observable -> JavaFxUtil.runLater { this.onUserActivity() } }
        gameStatusChangeListener = { observable, oldValue, newValue -> JavaFxUtil.runLater { this.updateGameStatus() } }
        avatarChangeListener = { observable, oldValue, newValue -> JavaFxUtil.runLater { setAvatarUrl(newValue) } }
        clanChangeListener = { observable, oldValue, newValue -> JavaFxUtil.runLater { setClanTag(newValue) } }
        countryChangeListener = { observable, oldValue, newValue -> JavaFxUtil.runLater { setCountry(newValue) } }

        weakUserActivityListener = WeakInvalidationListener(userActivityListener)
        weakGameStatusListener = WeakChangeListener(gameStatusChangeListener)
        weakAvatarChangeListener = WeakChangeListener(avatarChangeListener)
        weakClanChangeListener = WeakChangeListener(clanChangeListener)
        weakCountryChangeListener = WeakChangeListener(countryChangeListener)

        playerChangeListener = { observable, oldValue, newValue ->
            JavaFxUtil.runLater {
                if (oldValue != null) {
                    JavaFxUtil.removeListener(oldValue!!.idleSinceProperty(), weakUserActivityListener)
                    JavaFxUtil.removeListener(oldValue!!.statusProperty(), weakUserActivityListener)
                    JavaFxUtil.removeListener(oldValue!!.statusProperty(), weakGameStatusListener)
                    JavaFxUtil.removeListener(oldValue!!.avatarUrlProperty(), weakAvatarChangeListener)
                    JavaFxUtil.removeListener(oldValue!!.clanProperty(), weakClanChangeListener)
                    JavaFxUtil.removeListener(oldValue!!.countryProperty(), weakCountryChangeListener)

                    weakGameStatusListener.changed(oldValue!!.statusProperty(), oldValue!!.getStatus(), null)
                    weakAvatarChangeListener.changed(oldValue!!.avatarUrlProperty(), oldValue!!.getAvatarUrl(), null)
                    weakClanChangeListener.changed(oldValue!!.clanProperty(), oldValue!!.getClan(), null)
                    weakCountryChangeListener.changed(oldValue!!.countryProperty(), oldValue!!.getCountry(), null)
                }

                if (newValue != null) {
                    JavaFxUtil.addListener(newValue!!.idleSinceProperty(), weakUserActivityListener)
                    JavaFxUtil.addListener(newValue!!.statusProperty(), weakUserActivityListener)
                    JavaFxUtil.addListener(newValue!!.statusProperty(), weakGameStatusListener)
                    JavaFxUtil.addListener(newValue!!.avatarUrlProperty(), weakAvatarChangeListener)
                    JavaFxUtil.addListener(newValue!!.clanProperty(), weakClanChangeListener)
                    JavaFxUtil.addListener(newValue!!.countryProperty(), weakCountryChangeListener)

                    weakUserActivityListener.invalidated(newValue!!.idleSinceProperty())
                    weakGameStatusListener.changed(newValue!!.statusProperty(), null, newValue!!.getStatus())
                    weakAvatarChangeListener.changed(newValue!!.avatarUrlProperty(), null, newValue!!.getAvatarUrl())
                    weakClanChangeListener.changed(newValue!!.clanProperty(), null, newValue!!.getClan())
                    weakCountryChangeListener.changed(newValue!!.countryProperty(), null, newValue!!.getCountry())
                }

                if (this.chatUser != null) {
                    userActivityListener.invalidated(null)
                }
            }
        }
        weakPlayerChangeListener = WeakChangeListener(playerChangeListener)

        usernameInvalidationListener = { observable ->
            updateNameLabelTooltip()
            if (this.chatUser == null) {
                usernameLabel!!.text = ""
            } else {
                usernameLabel!!.text = this.chatUser!!.username
            }
        }
        weakUsernameInvalidationListener = WeakInvalidationListener(usernameInvalidationListener)
    }

    private fun updateFormat() {
        root!!.pseudoClassStateChanged(
                COMPACT,
                preferencesService.preferences!!.chat.chatFormat == ChatFormat.COMPACT
        )
    }

    override fun initialize() {
        // TODO until server side support is available, the presence status is initially set to "unknown" until the user
        // does something
        presenceStatusIndicator!!.text = "\uF10C"
        setIdle(false)

        root!!.userData = this
        countryImageView!!.managedProperty().bind(countryImageView!!.visibleProperty())
        countryImageView!!.isVisible = false
        statusLabel!!.managedProperty().bind(statusLabel!!.visibleProperty())
        statusLabel!!.visibleProperty().bind(statusLabel!!.textProperty().isNotEmpty)
        clanMenu!!.managedProperty().bind(clanMenu!!.visibleProperty())
        clanMenu!!.isVisible = false

        val chatPrefs = preferencesService.preferences!!.chat
        weakColorInvalidationListener.invalidated(chatPrefs.chatColorModeProperty())
        weakFormatInvalidationListener.invalidated(chatPrefs.chatFormatProperty())
    }

    fun onContextMenuRequested(event: ContextMenuEvent) {
        val contextMenuController = uiService.loadFxml<ChatUserContextMenuController>("theme/chat/chat_user_context_menu.fxml")
        contextMenuController.setChatUser(this.chatUser)
        contextMenuController.contextMenu!!.show(root, event.screenX, event.screenY)
    }

    fun onItemClicked(mouseEvent: MouseEvent) {
        if (mouseEvent.button == MouseButton.PRIMARY && mouseEvent.clickCount == 2) {
            eventBus.post(InitiatePrivateChatEvent(this.chatUser!!.username))
        }
    }

    private fun updateColor() {
        if (this.chatUser == null) {
            return
        }
        val chatPrefs = preferencesService.preferences!!.chat

        this.chatUser!!.player.ifPresent { player ->
            if (player.socialStatus == SELF) {
                usernameLabel!!.styleClass.add(SELF.cssClass)
                clanMenu!!.styleClass.add(SELF.cssClass)
            }
        }

        var color: Color? = null
        val lowerUsername = this.chatUser!!.username.toLowerCase(US)

        if (chatPrefs.chatColorMode == CUSTOM) {
            if (chatPrefs.userToColor.containsKey(lowerUsername)) {
                color = chatPrefs.userToColor[lowerUsername]
            }

            JavaFxUtil.addListener(chatPrefs.userToColor, WeakMapChangeListener(colorPerUserMapChangeListener))
        } else if (chatPrefs.chatColorMode == ChatColorMode.RANDOM && randomColorsAllowed) {
            color = ColorGeneratorUtil.generateRandomColor(this.chatUser!!.username.hashCode().toLong())
        }

        this.chatUser!!.color = color
        assignColor(color)
    }

    private fun assignColor(color: Color?) {
        if (color != null) {
            usernameLabel!!.style = String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color))
            clanMenu!!.style = String.format("-fx-text-fill: %s", JavaFxUtil.toRgbCode(color))
        } else {
            usernameLabel!!.style = ""
            clanMenu!!.style = ""
        }
    }

    private fun setAvatarUrl(avatarUrl: String?) {
        updateAvatarTooltip()
        if (Strings.isNullOrEmpty(avatarUrl)) {
            avatarImageView!!.isVisible = false
        } else {
            // Loading the avatar image asynchronously would be better but asynchronous list cell updates don't work well
            avatarImageView!!.image = avatarService.loadAvatar(avatarUrl)
            avatarImageView!!.isVisible = true
        }
    }

    private fun setClanTag(clanTag: String) {
        if (!this.chatUser!!.player.isPresent || Strings.isNullOrEmpty(clanTag)) {
            clanMenu!!.isVisible = false
            return
        }
        clanMenu!!.text = String.format("[%s]", clanTag)
        clanMenu!!.isVisible = true
        updateClanMenu()
    }

    private fun updateGameStatus() {
        if (this.chatUser == null) {
            return
        }
        val playerOptional = this.chatUser!!.player
        if (!playerOptional.isPresent) {
            statusLabel!!.text = ""
            return
        }

        val player = playerOptional.get()
        when (player.status) {
            IDLE -> statusLabel!!.text = ""
            PlayerStatus.HOSTING -> statusLabel!!.text = i18n.get("user.status.hosting", player.game.title)
            PlayerStatus.LOBBYING -> statusLabel!!.text = i18n.get("user.status.waiting", player.game.title)
            PlayerStatus.PLAYING -> statusLabel!!.text = i18n.get("user.status.playing", player.game.title)
        }
    }

    private fun updateCountryTooltip() {
        Optional.ofNullable(countryTooltip).ifPresent { imageView -> Tooltip.uninstall(countryImageView, countryTooltip) }

        this.chatUser!!.player.ifPresent { player ->
            countryTooltip = Tooltip(player.country)
            countryTooltip.text = player.country
            Tooltip.install(countryImageView, countryTooltip)
        }
    }

    private fun updateClanMenu() {
        this.chatUser!!.player.ifPresent(Consumer<Player> { this.updateClanMenu(it) })
    }

    private fun updateNameLabelTooltip() {
        Optional.ofNullable(usernameLabel!!.tooltip).ifPresent { tooltip -> usernameLabel!!.tooltip = null }

        if (this.chatUser == null || !this.chatUser!!.player.isPresent) {
            return
        }

        this.chatUser!!.player.ifPresent { player ->
            userTooltip = Tooltip()
            usernameLabel!!.tooltip = userTooltip
            updateNameLabelText(player)
        }
    }

    private fun updateNameLabelText(player: Player) {
        userTooltip!!.text = String.format("%s\n%s",
                i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)),
                i18n.get("userInfo.idleTimeFormat", timeService.timeAgo(player.idleSince)))
    }

    private fun addListeners(chatUser: ChatChannelUser) {
        JavaFxUtil.addListener(chatUser.usernameProperty(), weakUsernameInvalidationListener)
        JavaFxUtil.addListener(chatUser.colorProperty(), weakColorInvalidationListener)
        JavaFxUtil.addListener(chatUser.playerProperty(), weakPlayerChangeListener)

        weakUsernameInvalidationListener.invalidated(chatUser.usernameProperty())
        weakColorInvalidationListener.invalidated(chatUser.colorProperty())
        weakPlayerChangeListener.changed(chatUser.playerProperty(), null, chatUser.player.orElse(null))
    }

    private fun removeListeners(chatUser: ChatChannelUser) {
        JavaFxUtil.removeListener(chatUser.usernameProperty(), weakUsernameInvalidationListener)
        JavaFxUtil.removeListener(chatUser.colorProperty(), weakColorInvalidationListener)
        JavaFxUtil.removeListener(chatUser.playerProperty(), weakPlayerChangeListener)

        weakPlayerChangeListener.changed(chatUser.playerProperty(), chatUser.player.orElse(null), null)
    }

    private fun setCountry(country: String) {
        if (StringUtils.isEmpty(country)) {
            countryImageView!!.isVisible = false
        } else {
            countryFlagService.loadCountryFlag(country).ifPresent { image ->
                countryImageView!!.image = image
                countryImageView!!.isVisible = true
                updateCountryTooltip()
            }
        }
    }

    //TODO: see where this should be called
    internal fun setRandomColorAllowed(randomColorsAllowed: Boolean) {
        this.randomColorsAllowed = randomColorsAllowed
        updateColor()
    }

    fun setVisible(visible: Boolean) {
        root!!.isVisible = visible
        root!!.isManaged = visible
    }

    /**
     * Updates the displayed idle indicator (online/idle). This is called from outside in order to only have one timer per
     * channel, instead of one timer per chat user.
     */
    internal fun updatePresenceStatusIndicator() {
        JavaFxUtil.assertApplicationThread()

        if (this.chatUser == null) {
            setIdle(false)
            return
        }

        this.chatUser!!.player.ifPresent { player ->
            if (player.status != IDLE) {
                setIdle(false)
            }

            val idleThreshold = preferencesService.preferences!!.chat.idleThreshold
            setIdle(player.idleSince.isBefore(now().minus(Duration.ofMinutes(idleThreshold.toLong()))))
        }
    }

    private fun setIdle(idle: Boolean) {
        presenceStatusIndicator!!.pseudoClassStateChanged(PRESENCE_STATUS_ONLINE, !idle)
        presenceStatusIndicator!!.pseudoClassStateChanged(PRESENCE_STATUS_IDLE, idle)
        if (idle) {
            // TODO only until server-side support
            presenceStatusIndicator!!.text = "\uF111"
        }
    }

    private fun onUserActivity() {
        // TODO only until server-side support
        presenceStatusIndicator!!.text = "\uF111"
        updatePresenceStatusIndicator()
        updateGameStatus()
        if (this.chatUser!!.player.isPresent && userTooltip != null) {
            updateNameLabelText(this.chatUser!!.player.get())
        }
    }

    private fun updateClanMenu(player: Player) {
        clanService.getClanByTag(player.clan)
                .thenAccept { optionalClan -> JavaFxUtil.runLater { updateClanMenu(optionalClan) } }
                .exceptionally { throwable ->
                    log.warn("Clan was not updated", throwable)
                    null
                }
    }

    private fun updateClanMenu(optionalClan: Optional<Clan>) {
        clanMenu!!.items.clear()
        if (!optionalClan.isPresent) {
            return
        }

        val clan = optionalClan.get()
        if (playerService.isOnline(clan.leader.id)) {
            val messageLeaderItem = MenuItem(i18n.get("clan.messageLeader"))
            messageLeaderItem.setOnAction { event -> eventBus.post(InitiatePrivateChatEvent(clan.leader.username)) }
            clanMenu!!.items.add(messageLeaderItem)
        }

        val visitClanPageAction = MenuItem(i18n.get("clan.visitPage"))
        visitClanPageAction.setOnAction { event ->
            platformService.showDocument(clan.websiteUrl)
            // TODO: Could be viewed in clan section (if implemented)
        }
        clanMenu!!.items.add(visitClanPageAction)

        Optional.ofNullable(clanMenu!!.tooltip).ifPresent { tooltip -> clanMenu!!.tooltip = null }

        val clanTooltipController = uiService.loadFxml<ClanTooltipController>("theme/chat/clan_tooltip.fxml")
        clanTooltipController.setClan(clan)

        val clanTooltip = Tooltip()
        clanTooltip.maxHeight = clanTooltipController.root!!.height
        clanTooltip.graphic = clanTooltipController.root

        Tooltip.install(clanMenu, clanTooltip)
    }

    fun updateAvatarTooltip() {
        Optional.ofNullable(avatarTooltip).ifPresent { tooltip -> Tooltip.uninstall(avatarImageView, tooltip) }

        this.chatUser!!.player.ifPresent { player ->
            avatarTooltip = Tooltip(player.avatarTooltip)
            avatarTooltip.textProperty().bind(player.avatarTooltipProperty())
            avatarTooltip.anchorLocation = PopupWindow.AnchorLocation.CONTENT_TOP_LEFT

            Tooltip.install(avatarImageView, avatarTooltip)
        }
    }

    fun onMouseEnteredUserNameLabel() {
        this.chatUser!!.player.ifPresent(Consumer<Player> { this.updateNameLabelText(it) })
    }

    companion object {

        private val PRESENCE_STATUS_ONLINE = PseudoClass.getPseudoClass("online")
        private val PRESENCE_STATUS_IDLE = PseudoClass.getPseudoClass("idle")
        private val COMPACT = PseudoClass.getPseudoClass("compact")
    }
}
