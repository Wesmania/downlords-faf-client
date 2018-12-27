package com.faforever.client.fx

import com.faforever.client.chat.InitiatePrivateChatEvent
import com.faforever.client.chat.UrlPreviewResolver
import com.faforever.client.clan.ClanService
import com.faforever.client.clan.ClanTooltipController
import com.faforever.client.config.ClientProperties
import com.faforever.client.i18n.I18n
import com.faforever.client.main.event.ShowReplayEvent
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.Severity
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.replay.ReplayService
import com.faforever.client.theme.UiService
import com.faforever.client.ui.StageHolder
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.scene.control.ContentDisplay
import javafx.scene.control.Label
import javafx.scene.control.Tooltip
import javafx.stage.Popup
import javafx.stage.PopupWindow
import javafx.stage.PopupWindow.AnchorLocation
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Optional
import java.util.regex.Matcher
import java.util.regex.Pattern

import com.faforever.client.util.RatingUtil.getGlobalRating
import com.faforever.client.util.RatingUtil.getLeaderboardRating

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class BrowserCallback internal constructor(private val platformService: PlatformService, clientProperties: ClientProperties,
                                           private val urlPreviewResolver: UrlPreviewResolver, private val replayService: ReplayService, private val eventBus: EventBus,
                                           private val clanService: ClanService, private val uiService: UiService, private val playerService: PlayerService, private val i18n: I18n,
                                           private val notificationService: NotificationService) {
    private val replayUrlPattern: Pattern
    @VisibleForTesting
    internal var clanInfoPopup: Popup? = null
    private var linkPreviewTooltip: Tooltip? = null
    private var playerInfoPopup: Popup? = null
    private var lastMouseX: Double = 0.toDouble()
    private var lastMouseY: Double = 0.toDouble()

    init {

        val urlFormat = clientProperties.getVault().getReplayDownloadUrlFormat()
        val splitFormat = urlFormat.split("%s".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        replayUrlPattern = Pattern.compile(Pattern.quote(splitFormat[0]) + "(\\d+)" + Pattern.compile(if (splitFormat.size == 2) splitFormat[1] else ""))
    }

    /**
     * Called from JavaScript when user clicked a URL.
     */
    fun openUrl(url: String) {
        val replayUrlMatcher = replayUrlPattern.matcher(url)
        if (!replayUrlMatcher.matches()) {
            platformService.showDocument(url)
            return
        }

        val replayId = replayUrlMatcher.group(1)

        replayService.findById(Integer.parseInt(replayId))
                .thenAccept { replay ->
                    Platform.runLater {
                        if (replay.isPresent) {
                            eventBus.post(ShowReplayEvent(replay.get()))
                        } else {
                            notificationService.addNotification(ImmediateNotification(i18n.get("replay.notFoundTitle"), i18n.get("replay.replayNotFoundText", replayId), Severity.WARN))
                        }
                    }
                }
    }

    /**
     * Called from JavaScript when user clicks on user name in chat
     */
    fun openPrivateMessageTab(username: String) {
        eventBus.post(InitiatePrivateChatEvent(username))
    }

    /**
     * Called from JavaScript when user no longer hovers over an URL.
     */
    fun hideUrlPreview() {
        if (linkPreviewTooltip != null) {
            linkPreviewTooltip!!.hide()
            linkPreviewTooltip = null
        }
    }

    /**
     * Called from JavaScript when user hovers over an URL.
     */
    fun previewUrl(urlString: String) {
        urlPreviewResolver.resolvePreview(urlString).thenAccept { optionalPreview ->
            optionalPreview.ifPresent { preview ->
                linkPreviewTooltip = Tooltip(preview.description)
                linkPreviewTooltip!!.isAutoHide = true
                linkPreviewTooltip!!.anchorLocation = AnchorLocation.CONTENT_BOTTOM_LEFT
                linkPreviewTooltip!!.graphic = preview.node
                linkPreviewTooltip!!.contentDisplay = ContentDisplay.TOP
                Platform.runLater { linkPreviewTooltip!!.show(StageHolder.stage, lastMouseX + 20, lastMouseY) }
            }
        }
    }

    /**
     * Called from JavaScript when user hovers over a clan tag.
     */
    fun showClanInfo(clanTag: String) {
        clanService.getClanByTag(clanTag).thenAccept { clan ->
            Platform.runLater {
                if (!clan.isPresent || clanTag.isEmpty()) {
                    return@Platform.runLater
                }
                val clanTooltipController = uiService.loadFxml<ClanTooltipController>("theme/chat/clan_tooltip.fxml")
                clanTooltipController.setClan(clan.get())
                clanTooltipController.root!!.styleClass.add("tooltip")

                clanInfoPopup = Popup()
                clanInfoPopup!!.content.setAll(clanTooltipController.root)
                clanInfoPopup!!.anchorLocation = AnchorLocation.CONTENT_TOP_LEFT
                clanInfoPopup!!.isAutoHide = true
                clanInfoPopup!!.show(StageHolder.stage, lastMouseX, lastMouseY + 10)
            }
        }
    }

    /**
     * Called from JavaScript when user no longer hovers over a clan tag.
     */
    fun hideClanInfo() {
        if (clanInfoPopup == null) {
            return
        }
        Platform.runLater {
            clanInfoPopup!!.hide()
            clanInfoPopup = null
        }
    }

    /**
     * Called from JavaScript when user clicks on clan tag.
     */
    fun showClanWebsite(clanTag: String) {
        clanService.getClanByTag(clanTag).thenAccept { clan ->
            if (!clan.isPresent) {
                return@clanService.getClanByTag(clanTag).thenAccept
            }
            platformService.showDocument(clan.get().websiteUrl)
        }
    }

    /**
     * Called from JavaScript when user hovers over a user name.
     */
    fun showPlayerInfo(username: String) {
        val playerOptional = playerService.getPlayerForUsername(username)

        if (!playerOptional.isPresent) {
            return
        }

        val player = playerOptional.get()

        playerInfoPopup = Popup()
        val label = Label()
        label.styleClass.add("tooltip")
        playerInfoPopup!!.content.setAll(label)

        label.textProperty().bind(Bindings.createStringBinding(
                { i18n.get("userInfo.ratingFormat", getGlobalRating(player), getLeaderboardRating(player)) },
                player.leaderboardRatingMeanProperty(), player.leaderboardRatingDeviationProperty(),
                player.globalRatingMeanProperty(), player.globalRatingDeviationProperty()
        ))

        playerInfoPopup!!.anchorLocation = PopupWindow.AnchorLocation.CONTENT_BOTTOM_LEFT

        Platform.runLater { playerInfoPopup!!.show(StageHolder.stage, lastMouseX, lastMouseY - 10) }
    }

    /**
     * Called from JavaScript when user no longer hovers over a user name.
     */
    fun hidePlayerInfo() {
        if (playerInfoPopup == null) {
            return
        }
        Platform.runLater {
            playerInfoPopup!!.hide()
            playerInfoPopup = null
        }
    }

    internal fun setLastMouseX(screenX: Double) {
        lastMouseX = screenX
    }

    internal fun setLastMouseY(screenY: Double) {
        lastMouseY = screenY
    }
}
