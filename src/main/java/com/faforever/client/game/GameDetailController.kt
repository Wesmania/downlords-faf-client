package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.mod.ModService
import com.faforever.client.player.PlayerService
import com.faforever.client.theme.UiService
import com.faforever.client.util.ProgrammingError
import com.faforever.client.vault.replay.WatchButtonController
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.ObservableMap
import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import lombok.extern.slf4j.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.util.Optional

import javafx.beans.binding.Bindings.createObjectBinding
import javafx.beans.binding.Bindings.createStringBinding

@Component
@Slf4j
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class GameDetailController(private val i18n: I18n, private val mapService: MapService, private val modService: ModService, private val playerService: PlayerService,
                           private val uiService: UiService, private val joinGameHelper: JoinGameHelper) : Controller<Pane> {

    override var root: Pane? = null
    var gameTypeLabel: Label? = null
    var mapLabel: Label? = null
    var numberOfPlayersLabel: Label? = null
    var hostLabel: Label? = null
    var teamListPane: VBox? = null
    var mapImageView: ImageView? = null
    var gameTitleLabel: Label? = null
    var joinButton: Node? = null
    var watchButtonController: WatchButtonController? = null
    private val game: ObjectProperty<Game>
    private val teamsInvalidationListener: InvalidationListener
    private val gameStatusInvalidationListener: InvalidationListener
    private val weakTeamListener: WeakInvalidationListener
    private val weakGameStatusListener: WeakInvalidationListener
    private var watchButton: Node? = null

    private var featuredModInvalidationListener: InvalidationListener? = null

    init {

        game = SimpleObjectProperty()

        gameStatusInvalidationListener = { observable -> onGameStatusChanged() }
        teamsInvalidationListener = { observable -> createTeams() }
        weakTeamListener = WeakInvalidationListener(teamsInvalidationListener)
        weakGameStatusListener = WeakInvalidationListener(gameStatusInvalidationListener)
    }

    override fun initialize() {
        watchButton = watchButtonController!!.root

        joinButton!!.managedProperty().bind(joinButton!!.visibleProperty())
        watchButton!!.managedProperty().bind(watchButton!!.visibleProperty())
        gameTitleLabel!!.managedProperty().bind(gameTitleLabel!!.visibleProperty())
        hostLabel!!.managedProperty().bind(hostLabel!!.visibleProperty())
        mapLabel!!.managedProperty().bind(mapLabel!!.visibleProperty())
        numberOfPlayersLabel!!.managedProperty().bind(numberOfPlayersLabel!!.visibleProperty())
        mapImageView!!.managedProperty().bind(mapImageView!!.visibleProperty())
        gameTypeLabel!!.managedProperty().bind(gameTypeLabel!!.visibleProperty())

        gameTitleLabel!!.visibleProperty().bind(game.isNotNull)
        hostLabel!!.visibleProperty().bind(game.isNotNull)
        mapLabel!!.visibleProperty().bind(game.isNotNull)
        numberOfPlayersLabel!!.visibleProperty().bind(game.isNotNull)
        mapImageView!!.visibleProperty().bind(game.isNotNull)
        gameTypeLabel!!.visibleProperty().bind(game.isNotNull)

        setGame(null)
    }

    private fun onGameStatusChanged() {
        val game = this.game.get()
        when (game.status) {
            GameStatus.PLAYING -> {
                joinButton!!.isVisible = false
                watchButton!!.isVisible = true
                watchButtonController!!.setGame(game)
            }
            GameStatus.OPEN -> {
                joinButton!!.isVisible = true
                watchButton!!.isVisible = false
            }
            GameStatus.UNKNOWN, GameStatus.CLOSED -> {
                joinButton!!.isVisible = false
                watchButton!!.isVisible = false
            }
            else -> throw ProgrammingError("Uncovered status: " + game.status)
        }
    }

    fun setGame(game: Game?) {
        Optional.ofNullable(this.game.get()).ifPresent { oldGame ->
            Optional.ofNullable(weakTeamListener).ifPresent { listener -> oldGame.teams.removeListener(listener) }
            Optional.ofNullable(weakGameStatusListener).ifPresent { listener -> oldGame.statusProperty().removeListener(listener) }
        }

        this.game.set(game)
        if (game == null) {
            return
        }

        gameTitleLabel!!.textProperty().bind(game.titleProperty())
        hostLabel!!.textProperty().bind(game.hostProperty())
        mapLabel!!.textProperty().bind(game.mapFolderNameProperty())
        numberOfPlayersLabel!!.textProperty().bind(createStringBinding(
                { i18n.get("game.detail.players.format", game.numPlayers, game.maxPlayers) },
                game.numPlayersProperty(),
                game.maxPlayersProperty()
        ))
        mapImageView!!.imageProperty().bind(createObjectBinding<Image>(
                { mapService.loadPreview(game.mapFolderName, PreviewSize.LARGE) },
                game.mapFolderNameProperty()
        ))

        featuredModInvalidationListener = { observable ->
            modService.getFeaturedMod(game.featuredMod)
                    .thenAccept { featuredMod ->
                        Platform.runLater {
                            gameTypeLabel!!.text = i18n.get("loading")
                            val fullName = featuredMod?.displayName
                            gameTypeLabel!!.text = StringUtils.defaultString(fullName)
                        }
                    }
        }
        game.featuredModProperty().addListener(WeakInvalidationListener(featuredModInvalidationListener!!))
        featuredModInvalidationListener!!.invalidated(game.featuredModProperty())

        JavaFxUtil.addListener(game.teams, weakTeamListener)
        teamsInvalidationListener.invalidated(game.teams)

        JavaFxUtil.addListener(game.statusProperty(), weakGameStatusListener)
        gameStatusInvalidationListener.invalidated(game.statusProperty())
    }

    private fun createTeams() {
        teamListPane!!.children.clear()
        val teams = this.game.get().teams
        synchronized(teams) {
            TeamCardController.createAndAdd(teams, playerService, uiService, teamListPane)
        }
    }

    fun onJoinButtonClicked(event: ActionEvent) {
        joinGameHelper.join(game.get())
    }
}
