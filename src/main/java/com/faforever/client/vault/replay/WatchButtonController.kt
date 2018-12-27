package com.faforever.client.vault.replay

import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.Controller
import com.faforever.client.game.Game
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.replay.ReplayService
import com.faforever.client.util.TimeService
import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.scene.Node
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.util.Duration
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.time.Instant
import java.util.Optional
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class WatchButtonController(private val replayService: ReplayService, private val playerService: PlayerService, private val clientProperties: ClientProperties, private val timeService: TimeService, private val i18n: I18n) : Controller<Node> {

    var watchButton: MenuButton? = null
    private var game: Game? = null
    private var delayTimeline: Timeline? = null

    override val root: Node?
        get() = watchButton

    override fun initialize() {
        delayTimeline = Timeline(
                KeyFrame(Duration.ZERO, { event -> updateWatchButtonTimer() }),
                KeyFrame(Duration.seconds(1.0))
        )
        delayTimeline!!.cycleCount = Timeline.INDEFINITE

        watchButton!!.isDisable = true
    }

    fun setGame(game: Game) {
        this.game = game
        Assert.notNull(game.startTime, "The game's start must not be null: $game")

        val menuItems = game.teams.values.stream()
                .flatMap<String>(Function<List<String>, Stream<out String>> { it.stream() })
                .map<Optional<Player>>(Function<String, Optional<Player>> { playerService.getPlayerForUsername(it) })
                .filter(Predicate<Optional<Player>> { it.isPresent() })
                .map<Player>(Function<Optional<Player>, Player> { it.get() })
                .map { player -> createMenuItem(game, player) }
                .collect<List<MenuItem>, Any>(Collectors.toList())

        watchButton!!.items.setAll(menuItems)
        delayTimeline!!.play()
    }

    private fun createMenuItem(game: Game, player: Player): MenuItem {
        val menuItem = MenuItem(player.username)
        menuItem.userData = player.id
        menuItem.setOnAction { event -> replayService.runLiveReplay(game.id, player.id) }
        return menuItem
    }

    private fun updateWatchButtonTimer() {
        Assert.notNull(game, "Game must not be null")
        Assert.notNull(game!!.startTime,
                "Game's start time is null, in which case it shouldn't even be listed: " + game!!)

        val watchDelay = java.time.Duration.between(
                Instant.now(),
                game!!.startTime.plusSeconds(clientProperties.getReplay().getWatchDelaySeconds())
        )
        if (watchDelay.isZero || watchDelay.isNegative) {
            delayTimeline!!.stop()
            delayTimeline = null
            watchButton!!.text = i18n.get("game.watch")
            watchButton!!.isDisable = false
        } else {
            watchButton!!.text = i18n.get("game.watchDelayedFormat", timeService.shortDuration(watchDelay))
        }
    }
}
