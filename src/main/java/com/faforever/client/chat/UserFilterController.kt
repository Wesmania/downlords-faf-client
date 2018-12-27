package com.faforever.client.chat

import com.faforever.client.fx.Controller
import com.faforever.client.game.PlayerStatus
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.util.ProgrammingError
import com.faforever.client.util.RatingUtil
import com.google.common.annotations.VisibleForTesting
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.scene.Node
import javafx.scene.control.MenuButton
import javafx.scene.control.TextField
import javafx.scene.control.ToggleGroup
import javafx.scene.layout.GridPane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.Optional

import com.faforever.client.game.PlayerStatus.HOSTING
import com.faforever.client.game.PlayerStatus.IDLE
import com.faforever.client.game.PlayerStatus.LOBBYING
import com.faforever.client.game.PlayerStatus.PLAYING

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class UserFilterController(private val i18n: I18n) : Controller<Node> {
    var gameStatusMenu: MenuButton? = null
    var filterUserRoot: GridPane? = null
    var clanFilterField: TextField? = null
    var minRatingFilterField: TextField? = null
    var maxRatingFilterField: TextField? = null
    var gameStatusToggleGroup: ToggleGroup? = null
    private val filterApplied: BooleanProperty
    @VisibleForTesting
    internal var channelTabController: ChannelTabController
    @VisibleForTesting
    internal var playerStatusFilter: PlayerStatus? = null

    val isFilterApplied: Boolean
        get() = filterApplied.get()

    override val root: Node?
        get() = filterUserRoot

    init {
        this.filterApplied = SimpleBooleanProperty(false)
    }

    internal fun setChannelController(channelTabController: ChannelTabController) {
        this.channelTabController = channelTabController
    }

    override fun initialize() {
        clanFilterField!!.textProperty().addListener { observable, oldValue, newValue -> filterUsers() }
        minRatingFilterField!!.textProperty().addListener { observable, oldValue, newValue -> filterUsers() }
        maxRatingFilterField!!.textProperty().addListener { observable, oldValue, newValue -> filterUsers() }
    }

    private fun filterUsers() {
        channelTabController.setUserFilter(Predicate<CategoryOrChatUserListItem> { this.filterUser(it) })
        filterApplied.set(
                !maxRatingFilterField!!.text.isEmpty()
                        || !minRatingFilterField!!.text.isEmpty()
                        || !clanFilterField!!.text.isEmpty()
                        || playerStatusFilter != null
        )
    }

    private fun filterUser(userListItem: CategoryOrChatUserListItem): Boolean {
        if (userListItem.getUser() == null) {
            return false
        }
        val user = userListItem.getUser()
        return (channelTabController.isUsernameMatch(user)
                && isInClan(user)
                && isBoundByRating(user)
                && isGameStatusMatch(user))
    }

    fun filterAppliedProperty(): BooleanProperty {
        return filterApplied
    }

    @VisibleForTesting
    internal fun isInClan(chatUser: ChatChannelUser): Boolean {
        if (clanFilterField!!.text.isEmpty()) {
            return true
        }

        val playerOptional = chatUser.player

        if (!playerOptional.isPresent) {
            return false
        }

        val player = playerOptional.get()
        val clan = player.clan ?: return false

        val lowerCaseSearchString = clan.toLowerCase()
        return lowerCaseSearchString.contains(clanFilterField!!.text.toLowerCase())
    }

    @VisibleForTesting
    internal fun isBoundByRating(chatUser: ChatChannelUser): Boolean {
        if (minRatingFilterField!!.text.isEmpty() && maxRatingFilterField!!.text.isEmpty()) {
            return true
        }

        val optionalPlayer = chatUser.player

        if (!optionalPlayer.isPresent) {
            return false
        }

        val player = optionalPlayer.get()

        val globalRating = RatingUtil.getGlobalRating(player)
        var minRating: Int
        var maxRating: Int

        try {
            minRating = Integer.parseInt(minRatingFilterField!!.text)
        } catch (e: NumberFormatException) {
            minRating = Integer.MIN_VALUE
        }

        try {
            maxRating = Integer.parseInt(maxRatingFilterField!!.text)
        } catch (e: NumberFormatException) {
            maxRating = Integer.MAX_VALUE
        }

        return globalRating >= minRating && globalRating <= maxRating
    }

    @VisibleForTesting
    internal fun isGameStatusMatch(chatUser: ChatChannelUser): Boolean {
        if (playerStatusFilter == null) {
            return true
        }

        val playerOptional = chatUser.player

        if (!playerOptional.isPresent) {
            return false
        }

        val player = playerOptional.get()
        val playerStatus = player.status
        return if (playerStatusFilter == LOBBYING) {
            LOBBYING == playerStatus || HOSTING == playerStatus
        } else {
            playerStatusFilter == playerStatus
        }
    }

    fun onGameStatusPlaying() {
        updateGameStatusMenuText(if (playerStatusFilter == PLAYING) null else PLAYING)
        filterUsers()
    }

    fun onGameStatusLobby() {
        updateGameStatusMenuText(if (playerStatusFilter == LOBBYING) null else LOBBYING)
        filterUsers()
    }

    fun onGameStatusNone() {
        updateGameStatusMenuText(if (playerStatusFilter == IDLE) null else IDLE)
        filterUsers()
    }

    private fun updateGameStatusMenuText(status: PlayerStatus?) {
        playerStatusFilter = status
        if (status == null) {
            gameStatusMenu!!.text = i18n.get("game.gameStatus")
            gameStatusToggleGroup!!.selectToggle(null)
            return
        }

        when (status) {
            PLAYING -> gameStatusMenu!!.text = i18n.get("game.gameStatus.playing")
            LOBBYING -> gameStatusMenu!!.text = i18n.get("game.gameStatus.lobby")
            IDLE -> gameStatusMenu!!.text = i18n.get("game.gameStatus.none")
            else -> throw ProgrammingError("Uncovered player status: $status")
        }
    }
}
