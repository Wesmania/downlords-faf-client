package com.faforever.client.game


import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.replay.Replay.PlayerStats
import com.faforever.client.theme.UiService
import com.faforever.client.util.Rating
import com.faforever.client.util.RatingUtil
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.HashMap
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class TeamCardController(private val uiService: UiService, private val i18n: I18n) : Controller<Node> {
    var teamPaneRoot: Pane? = null
    var teamPane: VBox? = null
    var teamNameLabel: Label? = null
    private val ratingChangeControllersByPlayerId: MutableMap<Int, RatingChangeLabelController>

    override val root: Node?
        get() = teamPaneRoot

    init {
        ratingChangeControllersByPlayerId = HashMap()
    }

    fun setPlayersInTeam(team: String, playerList: List<Player>, ratingProvider: Function<Player, Rating>, ratingType: RatingType) {
        var totalRating = 0
        for (player in playerList) {
            // If the server wasn't bugged, this would never be the case.
            if (player == null) {
                continue
            }
            val playerCardTooltipController = uiService.loadFxml<PlayerCardTooltipController>("theme/player_card_tooltip.fxml")
            var playerRating = RatingUtil.getRating(ratingProvider.apply(player))
            totalRating += playerRating

            if (ratingType == RatingType.ROUNDED) {
                playerRating = RatingUtil.getRoundedGlobalRating(player)
            }
            playerCardTooltipController.setPlayer(player, playerRating)

            val ratingChangeLabelController = uiService.loadFxml<RatingChangeLabelController>("theme/rating_change_label.fxml")
            ratingChangeControllersByPlayerId[player.id] = ratingChangeLabelController
            val container = HBox(playerCardTooltipController.root, ratingChangeLabelController.root)
            teamPane!!.children.add(container)
        }

        val teamTitle: String
        if ("1" == team || "-1" == team) {
            teamTitle = i18n.get("game.tooltip.teamTitleNoTeam")
        } else if ("null" == team) {
            teamTitle = i18n.get("game.tooltip.observers")
        } else {
            teamTitle = i18n.get("game.tooltip.teamTitle", Integer.valueOf(team)!! - 1, totalRating)
        }
        teamNameLabel!!.text = teamTitle
    }

    fun showRatingChange(teams: Map<String, List<PlayerStats>>) {
        teams.values.stream()
                .flatMap { it.stream() }
                .filter { playerStats -> ratingChangeControllersByPlayerId.containsKey(playerStats.getPlayerId()) }
                .forEach { playerStats -> ratingChangeControllersByPlayerId[playerStats.getPlayerId()].setRatingChange(playerStats) }
    }

    companion object {

        /**
         * Creates a new [TeamCardController] and adds its root to the specified `teamsPane`.
         *
         * @param teamsList a mapping of team name (e.g. "2") to a list of player names that are in that team
         * @param playerService the service to use to look up players by name
         */
        internal fun createAndAdd(teamsList: ObservableMap<out String, out List<String>>, playerService: PlayerService, uiService: UiService, teamsPane: Pane) {
            for ((key, value) in teamsList) {
                val players = entry.value.stream()
                        .map({ playerService.getPlayerForUsername(it) })
                        .filter(Predicate<Optional<Player>> { it.isPresent() })
                        .map({ it.get() })
                        .collect(Collectors.toList())

                val teamCardController = uiService.loadFxml<TeamCardController>("theme/team_card.fxml")
                teamCardController.setPlayersInTeam(entry.key, players,
                        { player -> Rating(player.getGlobalRatingMean(), player.getGlobalRatingDeviation()) }, RatingType.ROUNDED)
                teamsPane.children.add(teamCardController.root)
            }
        }
    }
}
