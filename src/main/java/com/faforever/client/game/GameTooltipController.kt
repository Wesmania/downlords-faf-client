package com.faforever.client.game


import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.player.PlayerService
import com.faforever.client.theme.UiService
import com.google.common.base.Joiner
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.collections.ObservableMap
import javafx.collections.WeakMapChangeListener
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.TitledPane
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
class GameTooltipController @Inject
constructor(private val uiService: UiService, private val playerService: PlayerService) : Controller<Node> {

    var modsPane: TitledPane? = null
    var teamsPane: Pane? = null
    var modsLabel: Label? = null
    var gameTooltipRoot: VBox? = null
    private var lastTeams: ObservableMap<String, List<String>>? = null
    private var lastSimMods: ObservableMap<String, String>? = null
    private var teamChangedListener: InvalidationListener? = null
    private var simModsChangedListener: InvalidationListener? = null
    private var weakTeamChangeListener: WeakInvalidationListener? = null
    private var weakModChangeListener: WeakInvalidationListener? = null

    override val root: Node?
        get() = gameTooltipRoot

    override fun initialize() {
        modsPane!!.managedProperty().bind(modsPane!!.visibleProperty())
    }

    fun setGame(game: Game) {
        teamChangedListener = { change -> createTeams(game.teams) }
        simModsChangedListener = { change -> createModsList(game.simMods) }

        if (lastTeams != null && weakTeamChangeListener != null) {
            lastTeams!!.removeListener(weakTeamChangeListener)
        }

        if (lastSimMods != null && weakModChangeListener != null) {
            game.simMods.removeListener(weakModChangeListener)
        }

        lastSimMods = game.simMods
        lastTeams = game.teams
        createTeams(game.teams)
        createModsList(game.simMods)
        weakTeamChangeListener = WeakInvalidationListener(teamChangedListener!!)
        JavaFxUtil.addListener(game.teams, weakTeamChangeListener)
        weakModChangeListener = WeakInvalidationListener(simModsChangedListener!!)
        JavaFxUtil.addListener(game.simMods, weakModChangeListener)
    }

    private fun createTeams(teamsList: ObservableMap<out String, out List<String>>) {
        Platform.runLater {
            synchronized(teamsList) {
                teamsPane!!.children.clear()
                TeamCardController.createAndAdd(teamsList, playerService, uiService, teamsPane)
            }
        }
    }

    private fun createModsList(simMods: ObservableMap<out String, out String>) {
        val stringSimMods = Joiner.on(System.getProperty("line.separator")).join(simMods.values)
        Platform.runLater {
            if (simMods.isEmpty()) {
                modsPane!!.isVisible = false
                return@Platform.runLater
            }

            modsLabel!!.text = stringSimMods
            modsPane!!.isVisible = true
        }
    }
}
