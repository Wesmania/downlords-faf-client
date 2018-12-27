package com.faforever.client.coop

import com.faforever.client.api.dto.CoopResult
import com.faforever.client.api.dto.GamePlayerStats
import com.faforever.client.api.dto.Player
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.NodeTableCell
import com.faforever.client.fx.StringCell
import com.faforever.client.fx.StringListCell
import com.faforever.client.fx.WebViewConfigurer
import com.faforever.client.game.Game
import com.faforever.client.game.GameService
import com.faforever.client.game.GamesTableController
import com.faforever.client.game.NewGameInfo
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.mod.ModService
import com.faforever.client.notification.ImmediateErrorNotification
import com.faforever.client.notification.NotificationService
import com.faforever.client.remote.domain.GameStatus
import com.faforever.client.replay.ReplayService
import com.faforever.client.reporting.ReportingService
import com.faforever.client.theme.UiService
import com.faforever.client.util.TimeService
import com.google.common.base.Strings
import javafx.application.Platform
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.PasswordField
import javafx.scene.control.SingleSelectionModel
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.text.Text
import javafx.scene.web.WebView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Predicate
import java.util.stream.Collectors

import com.faforever.client.game.KnownFeaturedMod.COOP
import java.util.Collections.emptySet
import javafx.collections.FXCollections.observableList

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class CoopController @Inject
constructor(private val replayService: ReplayService, private val gameService: GameService, private val coopService: CoopService,
            private val notificationService: NotificationService, private val i18n: I18n, private val reportingService: ReportingService,
            private val mapService: MapService, private val uiService: UiService, private val timeService: TimeService,
            private val webViewConfigurer: WebViewConfigurer, private val modService: ModService) : AbstractViewController<Node>() {

    override var root: Node? = null
    var missionComboBox: ComboBox<CoopMission>? = null
    var mapImageView: ImageView? = null
    var descriptionWebView: WebView? = null
    var gameViewContainer: Pane? = null
    var titleTextField: TextField? = null
    var playButton: Button? = null
    var passwordTextField: PasswordField? = null
    var leaderboardTable: TableView<CoopResult>? = null
    var numberOfPlayersComboBox: ComboBox<Int>? = null
    var rankColumn: TableColumn<CoopResult, Int>? = null
    var playerCountColumn: TableColumn<CoopResult, Int>? = null
    var playerNamesColumn: TableColumn<CoopResult, String>? = null
    var secondaryObjectivesColumn: TableColumn<CoopResult, Boolean>? = null
    var timeColumn: TableColumn<CoopResult, Duration>? = null
    var replayColumn: TableColumn<CoopResult, String>? = null

    private var selectedMission: CoopMission
        get() = missionComboBox!!.selectionModel.selectedItem
        set(mission) {
            Platform.runLater {
                descriptionWebView!!.engine.loadContent(mission.description)
                mapImageView!!.image = mapService.loadPreview(mission.mapFolderName, PreviewSize.SMALL)
            }

            loadLeaderboard()
        }

    override fun initialize() {
        missionComboBox!!.setCellFactory { param -> missionListCell() }
        missionComboBox!!.setButtonCell(missionListCell())
        missionComboBox!!.selectionModel.selectedItemProperty().addListener { observable, oldValue, newValue -> selectedMission = newValue }
        playButton!!.disableProperty().bind(titleTextField!!.textProperty().isEmpty)

        numberOfPlayersComboBox!!.setButtonCell(numberOfPlayersCell())
        numberOfPlayersComboBox!!.setCellFactory { param -> numberOfPlayersCell() }
        numberOfPlayersComboBox!!.selectionModel.select(2)
        numberOfPlayersComboBox!!.selectionModel.selectedItemProperty().addListener { observable -> loadLeaderboard() }

        // TODO don't use API object but bean instead
        rankColumn!!.setCellValueFactory { param -> SimpleObjectProperty<T>(param.value.getRanking()) }
        rankColumn!!.setCellFactory { param -> StringCell(Function<Int, String> { it.toString() }) }

        playerCountColumn!!.setCellValueFactory { param -> SimpleObjectProperty<T>(param.value.getPlayerCount()) }
        playerCountColumn!!.setCellFactory { param -> StringCell(Function<Int, String> { it.toString() }) }

        playerNamesColumn!!.setCellValueFactory { param -> SimpleStringProperty(commaDelimitedPlayerList(param.value)) }

        playerNamesColumn!!.setCellFactory { param -> StringCell(Function<String, String> { it.toString() }) }

        secondaryObjectivesColumn!!.setCellValueFactory { param -> SimpleBooleanProperty(param.value.isSecondaryObjectives()) }
        secondaryObjectivesColumn!!.setCellFactory { param -> StringCell { aBoolean -> if (aBoolean) i18n.get("yes") else i18n.get("no") } }

        timeColumn!!.setCellValueFactory { param -> SimpleObjectProperty<T>(param.value.getDuration()) }
        timeColumn!!.setCellFactory { param -> StringCell(Function<Duration, String> { timeService.shortDuration(it) }) }

        replayColumn!!.setCellValueFactory { param -> SimpleStringProperty(param.value.getId()) }
        replayColumn!!.setCellFactory { param ->
            NodeTableCell { replayId ->
                val button = uiService.loadFxml<ReplayButtonController>("theme/play/coop/replay_button.fxml")
                button.replayId = replayId
                button.setOnClickedAction(Consumer<ReplayButtonController> { this.onReplayButtonClicked(it) })
                button.root
            }
        }

        // Without this and no coop missions, the WebView is empty and the transparent background can't be applied to <html>
        descriptionWebView!!.engine.loadContent("<html></html>")
        webViewConfigurer.configureWebView(descriptionWebView)

        val games = gameService.games

        val filteredItems = FilteredList(games)
        filteredItems.setPredicate(OPEN_COOP_GAMES_PREDICATE)

        val gamesTableController = uiService.loadFxml<GamesTableController>("theme/play/games_table.fxml")
        gamesTableController.initializeGameTable(filteredItems)

        val root = gamesTableController.root
        populateContainer(root)

        coopService.missions.thenAccept { coopMaps ->
            Platform.runLater { missionComboBox!!.setItems(observableList(coopMaps)) }

            val selectionModel = missionComboBox!!.selectionModel
            if (selectionModel.isEmpty) {
                Platform.runLater { selectionModel.selectFirst() }
            }
        }.exceptionally { throwable ->
            notificationService.addPersistentErrorNotification(throwable, "coop.couldNotLoad", throwable.localizedMessage)
            null
        }
    }

    private fun commaDelimitedPlayerList(coopResult: CoopResult): String {
        return coopResult.getGame().getPlayerStats().stream()
                .map(???({ getPlayer() }))
        .map(???({ getLogin() }))
        .collect(Collectors.joining(i18n.get("textSeparator")))
    }

    private fun onReplayButtonClicked(button: ReplayButtonController) {
        val replayId = button.replayId
        replayService.runReplay(Integer.valueOf(replayId!!))
    }

    private fun numberOfPlayersCell(): ListCell<Int> {
        return StringListCell { numberOfPlayers ->
            if (numberOfPlayers == 0) {
                return i18n.get("coop.leaderboard.allPlayers")
            }
            if (numberOfPlayers == 1) {
                return i18n.get("coop.leaderboard.singlePlayer")
            }
            i18n.get("coop.leaderboard.numberOfPlayersFormat", numberOfPlayers)
        }
    }

    private fun missionListCell(): ListCell<CoopMission> {
        return StringListCell(Function<CoopMission, String> { it.getName() },
                { mission ->
                    val text = Text()
                    text.styleClass.add(UiService.CSS_CLASS_ICON)
                    when (mission.getCategory()) {
                        CoopCategory.AEON -> text.text = "\uE900"
                        CoopCategory.CYBRAN -> text.text = "\uE902"
                        CoopCategory.UEF -> text.text = "\uE904"
                        else -> return null
                    }
                    text
                }, Pos.CENTER_LEFT, "coop-mission")
    }

    private fun loadLeaderboard() {
        coopService.getLeaderboard(selectedMission, numberOfPlayersComboBox!!.selectionModel.selectedItem)
                .thenAccept { coopLeaderboardEntries ->
                    val ranking = AtomicInteger()
                    coopLeaderboardEntries.forEach { coopResult -> coopResult.setRanking(ranking.incrementAndGet()) }
                    Platform.runLater { leaderboardTable!!.setItems(observableList(coopLeaderboardEntries)) }
                }
                .exceptionally { throwable ->
                    notificationService.addNotification(ImmediateErrorNotification(
                            i18n.get("errorTitle"), i18n.get("coop.leaderboard.couldNotLoad"), throwable, i18n, reportingService
                    ))
                    null
                }
    }

    private fun populateContainer(root: Node?) {
        gameViewContainer!!.children.setAll(root)
        JavaFxUtil.setAnchors(root, 0.0)
    }

    fun onPlayButtonClicked() {
        modService.getFeaturedMod(COOP.technicalName)
                .thenAccept { featuredModBean ->
                    gameService.hostGame(NewGameInfo(titleTextField!!.text,
                            Strings.emptyToNull(passwordTextField!!.text), featuredModBean, selectedMission.mapFolderName,
                            emptySet()))
                }
    }

    companion object {

        private val OPEN_COOP_GAMES_PREDICATE = { gameInfoBean -> gameInfoBean.getStatus() == GameStatus.OPEN && COOP.technicalName == gameInfoBean.getFeaturedMod() }
    }
}
