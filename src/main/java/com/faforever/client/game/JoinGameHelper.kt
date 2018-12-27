package com.faforever.client.game

import com.faforever.client.i18n.I18n
import com.faforever.client.notification.Action
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.ImmediateNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.Severity
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.ui.StageHolder
import com.faforever.client.ui.preferences.event.GameDirectoryChooseEvent
import com.faforever.client.util.RatingUtil
import com.google.common.eventbus.EventBus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.lang.invoke.MethodHandles
import java.nio.file.Path
import java.util.Optional
import java.util.concurrent.CompletableFuture

import java.util.Arrays.asList

@Component
@Scope
class JoinGameHelper(private val i18n: I18n, private val playerService: PlayerService, private val gameService: GameService, private val preferencesService: PreferencesService, private val notificationService: NotificationService, private val reportingService: ReportingService, private val uiService: UiService, private val eventBus: EventBus) {

    fun join(game: Game?) {
        this.join(game, null, false)
    }

    fun join(game: Game?, password: String?, ignoreRating: Boolean) {
        val currentPlayer = playerService.currentPlayer.orElseThrow { IllegalStateException("Player has not been set") }
        val playerRating = RatingUtil.getRoundedGlobalRating(currentPlayer)

        if (!preferencesService.isGamePathValid) {
            val gameDirectoryFuture = CompletableFuture<Path>()
            eventBus.post(GameDirectoryChooseEvent(gameDirectoryFuture))
            gameDirectoryFuture.thenAccept { path -> Optional.ofNullable(path).ifPresent { path1 -> join(game, password, ignoreRating) } }
            return
        }

        if (!ignoreRating && (playerRating < game!!.minRating || playerRating > game.maxRating)) {
            showRatingOutOfBoundsConfirmation(playerRating, game, password)
            return
        }

        if (game!!.passwordProtected && password == null) {
            val enterPasswordController = uiService.loadFxml<EnterPasswordController>("theme/enter_password.fxml")
            enterPasswordController.setOnPasswordEnteredListener(OnPasswordEnteredListener { game, password, ignoreRating -> this.join(game, password, ignoreRating) })
            enterPasswordController.setGame(game)
            enterPasswordController.setIgnoreRating(ignoreRating)
            enterPasswordController.showPasswordDialog(StageHolder.stage)
        } else {
            gameService.joinGame(game, password)
                    .exceptionally { throwable ->
                        logger.warn("Game could not be joined", throwable)
                        notificationService.addNotification(ImmediateErrorNotification(
                                i18n.get("errorTitle"),
                                i18n.get("games.couldNotJoin"),
                                throwable,
                                i18n, reportingService
                        ))
                        null
                    }
        }
    }

    private fun showRatingOutOfBoundsConfirmation(playerRating: Int, game: Game, password: String?) {
        notificationService.addNotification(ImmediateNotification(
                i18n.get("game.joinGameRatingConfirmation.title"),
                i18n.get("game.joinGameRatingConfirmation.text", game.minRating, game.maxRating, playerRating),
                Severity.INFO,
                asList(
                        Action(i18n.get("game.join")) { event -> this.join(game, password, true) },
                        Action(i18n.get("game.cancel"))
                )
        ))
    }

    fun join(gameId: Int) {
        join(gameService.getByUid(gameId))
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
