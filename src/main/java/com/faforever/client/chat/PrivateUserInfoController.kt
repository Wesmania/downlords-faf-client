package com.faforever.client.chat

import com.faforever.client.achievements.AchievementService
import com.faforever.client.api.dto.AchievementState
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.game.Game
import com.faforever.client.game.GameDetailController
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.util.IdenticonUtil
import com.faforever.client.util.RatingUtil
import com.neovisionaries.i18n.CountryCode
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.concurrent.CompletableFuture

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class PrivateUserInfoController(private val countryFlagService: CountryFlagService, private val i18n: I18n, private val achievementService: AchievementService) : Controller<Node> {

    var userImageView: ImageView? = null
    var usernameLabel: Label? = null
    var countryImageView: ImageView? = null
    var countryLabel: Label? = null
    var globalRatingLabel: Label? = null
    var leaderboardRatingLabel: Label? = null
    var gamesPlayedLabel: Label? = null
    var gameDetailController: GameDetailController? = null
    var gameDetailWrapper: Pane? = null
    var unlockedAchievementsLabel: Label? = null
    override var root: Node? = null
    var globalRatingLabelLabel: Label? = null
    var leaderboardRatingLabelLabel: Label? = null
    var gamesPlayedLabelLabel: Label? = null
    var unlockedAchievementsLabelLabel: Label? = null

    private var globalRatingInvalidationListener: InvalidationListener? = null
    private var leaderboardRatingInvalidationListener: InvalidationListener? = null
    private var gameInvalidationListener: InvalidationListener? = null

    override fun initialize() {
        JavaFxUtil.bindManagedToVisible(
                gameDetailWrapper,
                countryLabel,
                gamesPlayedLabel,
                unlockedAchievementsLabel,
                globalRatingLabel,
                leaderboardRatingLabel,
                globalRatingLabelLabel,
                leaderboardRatingLabelLabel,
                gamesPlayedLabelLabel,
                unlockedAchievementsLabelLabel
        )
        onPlayerGameChanged(null)
    }

    fun setChatUser(chatUser: ChatChannelUser) {
        chatUser.player.ifPresentOrElse(???({ this.displayPlayerInfo(it) }), {
            chatUser.playerProperty().addListener { observable, oldValue, newValue ->
                if (newValue != null) {
                    displayPlayerInfo(newValue)
                } else {
                    displayChatUserInfo(chatUser)
                }
            }
            displayChatUserInfo(chatUser)
        })
    }

    private fun displayChatUserInfo(chatUser: ChatChannelUser) {
        usernameLabel!!.textProperty().bind(chatUser.usernameProperty())
        onPlayerGameChanged(null)
        setPlayerInfoVisible(false)
    }

    private fun setPlayerInfoVisible(visible: Boolean) {
        userImageView!!.isVisible = visible
        countryLabel!!.isVisible = visible
        globalRatingLabel!!.isVisible = visible
        globalRatingLabelLabel!!.isVisible = visible
        leaderboardRatingLabel!!.isVisible = visible
        leaderboardRatingLabelLabel!!.isVisible = visible
        gamesPlayedLabel!!.isVisible = visible
        gamesPlayedLabelLabel!!.isVisible = visible
        unlockedAchievementsLabel!!.isVisible = visible
        unlockedAchievementsLabelLabel!!.isVisible = visible
    }

    private fun displayPlayerInfo(player: Player) {
        setPlayerInfoVisible(true)
        val countryCode = CountryCode.getByCode(player.country)

        usernameLabel!!.textProperty().bind(player.usernameProperty())

        userImageView!!.image = IdenticonUtil.createIdenticon(player.id)
        userImageView!!.isVisible = true

        countryFlagService.loadCountryFlag(player.country).ifPresent { image -> countryImageView!!.image = image }
        countryLabel!!.text = if (countryCode == null) player.country else countryCode.getName()
        countryLabel!!.isVisible = true

        globalRatingInvalidationListener = { observable -> loadReceiverGlobalRatingInformation(player) }
        JavaFxUtil.addListener(player.globalRatingMeanProperty(), WeakInvalidationListener(globalRatingInvalidationListener!!))
        JavaFxUtil.addListener(player.globalRatingDeviationProperty(), WeakInvalidationListener(globalRatingInvalidationListener!!))
        loadReceiverGlobalRatingInformation(player)

        leaderboardRatingInvalidationListener = { observable -> loadReceiverLadderRatingInformation(player) }
        JavaFxUtil.addListener(player.leaderboardRatingMeanProperty(), WeakInvalidationListener(leaderboardRatingInvalidationListener!!))
        JavaFxUtil.addListener(player.leaderboardRatingDeviationProperty(), WeakInvalidationListener(leaderboardRatingInvalidationListener!!))
        loadReceiverLadderRatingInformation(player)

        gameInvalidationListener = { observable -> onPlayerGameChanged(player.game) }
        JavaFxUtil.addListener(player.gameProperty(), WeakInvalidationListener(gameInvalidationListener!!))
        onPlayerGameChanged(player.game)

        JavaFxUtil.bind(gamesPlayedLabel!!.textProperty(), player.numberOfGamesProperty().asString())

        populateUnlockedAchievementsLabel(player)
    }

    private fun populateUnlockedAchievementsLabel(player: Player): CompletableFuture<CompletableFuture<Void>> {
        return achievementService.achievementDefinitions
                .thenApply { achievementDefinitions ->
                    val totalAchievements = achievementDefinitions.size
                    achievementService.getPlayerAchievements(player.id)
                            .thenAccept { playerAchievements ->
                                val unlockedAchievements = playerAchievements.stream()
                                        .filter { playerAchievement -> playerAchievement.getState() === AchievementState.UNLOCKED }
                                        .count()

                                Platform.runLater {
                                    unlockedAchievementsLabel!!.text = i18n.get("chat.privateMessage.achievements.unlockedFormat", unlockedAchievements, totalAchievements)
                                }
                            }
                            .exceptionally { throwable ->
                                log.warn("Could not load achievements for player '" + player.id, throwable)
                                null
                            }
                }
    }

    private fun onPlayerGameChanged(newGame: Game?) {
        gameDetailController!!.setGame(newGame)
        gameDetailWrapper!!.isVisible = newGame != null
    }

    private fun loadReceiverGlobalRatingInformation(player: Player) {
        Platform.runLater {
            globalRatingLabel!!.text = i18n.get("chat.privateMessage.ratingFormat",
                    RatingUtil.getRating(player.globalRatingMean.toDouble(), player.globalRatingDeviation.toDouble()))
        }
    }

    private fun loadReceiverLadderRatingInformation(player: Player) {
        Platform.runLater {
            leaderboardRatingLabel!!.text = i18n.get("chat.privateMessage.ratingFormat",
                    RatingUtil.getRating(player.leaderboardRatingMean.toDouble(), player.leaderboardRatingDeviation.toDouble()))
        }
    }
}
