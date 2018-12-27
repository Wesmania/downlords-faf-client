package com.faforever.client.chat

import ch.micheljung.fxborderlessscene.borderless.BorderlessScene
import com.faforever.client.achievements.AchievementItemController
import com.faforever.client.achievements.AchievementService
import com.faforever.client.achievements.AchievementService.AchievementState
import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.api.dto.PlayerEvent
import com.faforever.client.domain.RatingHistoryDataPoint
import com.faforever.client.events.EventService
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.OffsetDateTimeCell
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.i18n.I18n
import com.faforever.client.leaderboard.LeaderboardService
import com.faforever.client.notification.NotificationService
import com.faforever.client.player.NameRecord
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.stats.StatisticsService
import com.faforever.client.theme.UiService
import com.faforever.client.util.Assert
import com.faforever.client.util.IdenticonUtil
import com.faforever.client.util.RatingUtil
import com.faforever.client.util.TimeService
import com.neovisionaries.i18n.CountryCode
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.binding.Bindings
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.PieChart
import javafx.scene.chart.StackedBarChart
import javafx.scene.chart.XYChart
import javafx.scene.chart.XYChart.Data
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.ToggleButton
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.Window
import javafx.util.StringConverter
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.HashMap
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

import com.faforever.client.achievements.AchievementService.AchievementState.UNLOCKED
import com.faforever.client.events.EventService.EVENT_AEON_PLAYS
import com.faforever.client.events.EventService.EVENT_AEON_WINS
import com.faforever.client.events.EventService.EVENT_BUILT_AIR_UNITS
import com.faforever.client.events.EventService.EVENT_BUILT_LAND_UNITS
import com.faforever.client.events.EventService.EVENT_BUILT_NAVAL_UNITS
import com.faforever.client.events.EventService.EVENT_BUILT_TECH_1_UNITS
import com.faforever.client.events.EventService.EVENT_BUILT_TECH_2_UNITS
import com.faforever.client.events.EventService.EVENT_BUILT_TECH_3_UNITS
import com.faforever.client.events.EventService.EVENT_CYBRAN_PLAYS
import com.faforever.client.events.EventService.EVENT_CYBRAN_WINS
import com.faforever.client.events.EventService.EVENT_SERAPHIM_PLAYS
import com.faforever.client.events.EventService.EVENT_SERAPHIM_WINS
import com.faforever.client.events.EventService.EVENT_UEF_PLAYS
import com.faforever.client.events.EventService.EVENT_UEF_WINS
import javafx.collections.FXCollections.observableList

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
@Slf4j
class UserInfoWindowController @Inject
constructor(private val statisticsService: StatisticsService, private val countryFlagService: CountryFlagService,
            private val achievementService: AchievementService, private val eventService: EventService, private val i18n: I18n,
            private val uiService: UiService, private val timeService: TimeService,
            private val notificationService: NotificationService, private val playerService: PlayerService,
            private val leaderboardService: LeaderboardService) : Controller<Node> {
    var lockedAchievementsHeaderLabel: Label? = null
    var unlockedAchievementsHeaderLabel: Label? = null
    var gamesPlayedChart: PieChart? = null
    var techBuiltChart: PieChart? = null
    var unitsBuiltChart: PieChart? = null
    var factionsChart: StackedBarChart<*, *>? = null
    var gamesPlayedLabel: Label? = null
    var ratingLabelGlobal: Label? = null
    var ratingLabel1v1: Label? = null
    var avatarImageView: ImageView? = null
    var unlockedAchievementsHeader: Pane? = null
    var lockedAchievementsHeader: Pane? = null
    var achievementsPane: ScrollPane? = null
    var mostRecentAchievementImageView: ImageView? = null
    var mostRecentAchievementDescriptionLabel: Label? = null
    var loadingProgressLabel: Label? = null
    var mostRecentAchievementPane: Pane? = null
    var mostRecentAchievementNameLabel: Label? = null
    var lockedAchievementsContainer: Pane? = null
    var unlockedAchievementsContainer: Pane? = null
    var globalButton: ToggleButton? = null
    var ladder1v1Button: ToggleButton? = null
    var yAxis: NumberAxis? = null
    var xAxis: NumberAxis? = null
    var ratingHistoryChart: LineChart<Int, Int>? = null
    var usernameLabel: Label? = null
    var countryLabel: Label? = null
    var countryImageView: ImageView? = null
    var userInfoRoot: Pane? = null
    var nameHistoryTable: TableView<NameRecord>? = null
    var changeDateColumn: TableColumn<NameRecord, OffsetDateTime>? = null
    var nameColumn: TableColumn<NameRecord, String>? = null
    private var player: Player? = null
    private val achievementItemById: MutableMap<String, AchievementItemController>
    private val achievementDefinitionById: MutableMap<String, AchievementDefinition>
    private var ownerWindow: Window? = null

    override val root: Region?
        get() = userInfoRoot


    init {

        achievementItemById = HashMap()
        achievementDefinitionById = HashMap()
    }

    override fun initialize() {
        loadingProgressLabel!!.managedProperty().bind(loadingProgressLabel!!.visibleProperty())
        achievementsPane!!.managedProperty().bind(achievementsPane!!.visibleProperty())
        mostRecentAchievementPane!!.managedProperty().bind(mostRecentAchievementPane!!.visibleProperty())

        unlockedAchievementsHeader!!.managedProperty().bind(unlockedAchievementsHeader!!.visibleProperty())
        unlockedAchievementsHeader!!.visibleProperty().bind(unlockedAchievementsContainer!!.visibleProperty())
        unlockedAchievementsContainer!!.managedProperty().bind(unlockedAchievementsContainer!!.visibleProperty())
        unlockedAchievementsContainer!!.visibleProperty().bind(Bindings.createBooleanBinding(
                { !unlockedAchievementsContainer!!.children.isEmpty() }, unlockedAchievementsContainer!!.children))

        lockedAchievementsHeader!!.managedProperty().bind(lockedAchievementsHeader!!.visibleProperty())
        lockedAchievementsHeader!!.visibleProperty().bind(lockedAchievementsContainer!!.visibleProperty())
        lockedAchievementsContainer!!.managedProperty().bind(lockedAchievementsContainer!!.visibleProperty())
        lockedAchievementsContainer!!.visibleProperty().bind(Bindings.createBooleanBinding(
                { !lockedAchievementsContainer!!.children.isEmpty() }, lockedAchievementsContainer!!.children))

        lockedAchievementsContainer!!.children.addListener({ observable -> lockedAchievementsHeaderLabel!!.text = i18n.get("achievements.locked", lockedAchievementsContainer!!.children.size) } as InvalidationListener
        )
        unlockedAchievementsContainer!!.children.addListener({ observable -> unlockedAchievementsHeaderLabel!!.text = i18n.get("achievements.unlocked", unlockedAchievementsContainer!!.children.size) } as InvalidationListener
        )

        nameColumn!!.setCellValueFactory { param -> param.value.nameProperty() }
        changeDateColumn!!.setCellValueFactory { param -> param.value.changeDateProperty() }
        changeDateColumn!!.setCellFactory { param -> OffsetDateTimeCell(timeService) }
    }

    private fun setAvailableAchievements(achievementDefinitions: List<AchievementDefinition>) {
        val children = lockedAchievementsContainer!!.children
        Platform.runLater { children.clear() }

        achievementDefinitions.forEach { achievementDefinition ->
            val controller = uiService.loadFxml<AchievementItemController>("theme/achievement_item.fxml")
            controller.setAchievementDefinition(achievementDefinition)
            achievementDefinitionById[achievementDefinition.getId()] = achievementDefinition
            achievementItemById[achievementDefinition.getId()] = controller
            Platform.runLater { children.add(controller.root) }
        }
    }

    fun setPlayer(player: Player) {
        this.player = player

        usernameLabel!!.text = player.username
        countryFlagService.loadCountryFlag(player.country).ifPresent { image -> countryImageView!!.image = image }
        avatarImageView!!.image = IdenticonUtil.createIdenticon(player.id)
        gamesPlayedLabel!!.text = i18n.number(player.numberOfGames)
        ratingLabelGlobal!!.text = i18n.number(RatingUtil.getGlobalRating(player))
        ratingLabel1v1!!.text = i18n.number(RatingUtil.getLeaderboardRating(player))

        updateNameHistory(player)

        val countryCode = CountryCode.getByCode(player.country)
        if (countryCode != null) {
            // Country code is unknown to CountryCode, like A1 or A2 (from GeoIP)
            countryLabel!!.text = countryCode.getName()
        } else {
            countryLabel!!.text = player.country
        }

        globalButton!!.fire()
        globalButton!!.isSelected = true

        loadAchievements()
        eventService.getPlayerEvents(player.id)
                .thenAccept { events ->
                    plotFactionsChart(events)
                    plotUnitsByCategoriesChart(events)
                    plotTechBuiltChart(events)
                    plotGamesPlayedChart()
                }
                .exceptionally { throwable ->
                    notificationService.addImmediateErrorNotification(throwable, "userInfo.statistics.errorLoading")
                    log.warn("Could not load player events", throwable)
                    null
                }
    }

    private fun updateNameHistory(player: Player) {
        playerService.getPlayersByIds(listOf(player.id))
                .thenAccept { players -> nameHistoryTable!!.setItems(players[0].names) }
                .exceptionally { throwable ->
                    notificationService.addImmediateErrorNotification(throwable, "userInfo.nameHistory.errorLoading")
                    null
                }
    }

    private fun loadAchievements() {
        enterAchievementsLoadingState()
        achievementService.achievementDefinitions
                .exceptionally { throwable ->
                    notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLoading")
                    log.warn("Player achievements could not be loaded", throwable)
                    emptyList()
                }
                .thenAccept(Consumer<List<AchievementDefinition>> { this.setAvailableAchievements(it) })
                .thenCompose { aVoid -> achievementService.getPlayerAchievements(player!!.id) }
                .thenAccept { playerAchievements ->
                    updatePlayerAchievements(playerAchievements)
                    enterAchievementsLoadedState()
                }
                .exceptionally { throwable ->
                    notificationService.addImmediateErrorNotification(throwable, "userInfo.achievements.errorLDisplaying")
                    log.warn("Could not display achievement definitions", throwable)
                    null
                }
    }

    private fun plotFactionsChart(playerEvents: Map<String, PlayerEvent>) {
        val aeonPlays = if (playerEvents.containsKey(EVENT_AEON_PLAYS)) playerEvents[EVENT_AEON_PLAYS].getCurrentCount() else 0
        val cybranPlays = if (playerEvents.containsKey(EVENT_CYBRAN_PLAYS)) playerEvents[EVENT_CYBRAN_PLAYS].getCurrentCount() else 0
        val uefPlays = if (playerEvents.containsKey(EVENT_UEF_PLAYS)) playerEvents[EVENT_UEF_PLAYS].getCurrentCount() else 0
        val seraphimPlays = if (playerEvents.containsKey(EVENT_SERAPHIM_PLAYS)) playerEvents[EVENT_SERAPHIM_PLAYS].getCurrentCount() else 0

        val aeonWins = if (playerEvents.containsKey(EVENT_AEON_WINS)) playerEvents[EVENT_AEON_WINS].getCurrentCount() else 0
        val cybranWins = if (playerEvents.containsKey(EVENT_CYBRAN_WINS)) playerEvents[EVENT_CYBRAN_WINS].getCurrentCount() else 0
        val uefWins = if (playerEvents.containsKey(EVENT_UEF_WINS)) playerEvents[EVENT_UEF_WINS].getCurrentCount() else 0
        val seraphimWins = if (playerEvents.containsKey(EVENT_SERAPHIM_WINS)) playerEvents[EVENT_SERAPHIM_WINS].getCurrentCount() else 0

        val winsSeries = XYChart.Series<String, Int>()
        winsSeries.name = i18n.get("userInfo.wins")
        winsSeries.data.add(XYChart.Data("Aeon", aeonWins))
        winsSeries.data.add(XYChart.Data("Cybran", cybranWins))
        winsSeries.data.add(XYChart.Data("UEF", uefWins))
        winsSeries.data.add(XYChart.Data("Seraphim", seraphimWins))

        val lossSeries = XYChart.Series<String, Int>()
        lossSeries.name = i18n.get("userInfo.losses")
        lossSeries.data.add(XYChart.Data("Aeon", aeonPlays - aeonWins))
        lossSeries.data.add(XYChart.Data("Cybran", cybranPlays - cybranWins))
        lossSeries.data.add(XYChart.Data("UEF", uefPlays - uefWins))
        lossSeries.data.add(XYChart.Data("Seraphim", seraphimPlays - seraphimWins))

        Platform.runLater { factionsChart!!.data.addAll(winsSeries, lossSeries) }
    }

    private fun plotUnitsByCategoriesChart(playerEvents: Map<String, PlayerEvent>) {
        val airBuilt = if (playerEvents.containsKey(EVENT_BUILT_AIR_UNITS)) playerEvents[EVENT_BUILT_AIR_UNITS].getCurrentCount() else 0
        val landBuilt = if (playerEvents.containsKey(EVENT_BUILT_LAND_UNITS)) playerEvents[EVENT_BUILT_LAND_UNITS].getCurrentCount() else 0
        val navalBuilt = if (playerEvents.containsKey(EVENT_BUILT_NAVAL_UNITS)) playerEvents[EVENT_BUILT_NAVAL_UNITS].getCurrentCount() else 0

        Platform.runLater {
            unitsBuiltChart!!.data = FXCollections.observableArrayList<Data>(
                    PieChart.Data(i18n.get("stats.air"), airBuilt.toDouble()),
                    PieChart.Data(i18n.get("stats.land"), landBuilt.toDouble()),
                    PieChart.Data(i18n.get("stats.naval"), navalBuilt.toDouble())
            )
        }
    }

    private fun plotTechBuiltChart(playerEvents: Map<String, PlayerEvent>) {
        val tech1Built = if (playerEvents.containsKey(EVENT_BUILT_TECH_1_UNITS)) playerEvents[EVENT_BUILT_TECH_1_UNITS].getCurrentCount() else 0
        val tech2Built = if (playerEvents.containsKey(EVENT_BUILT_TECH_2_UNITS)) playerEvents[EVENT_BUILT_TECH_2_UNITS].getCurrentCount() else 0
        val tech3Built = if (playerEvents.containsKey(EVENT_BUILT_TECH_3_UNITS)) playerEvents[EVENT_BUILT_TECH_3_UNITS].getCurrentCount() else 0

        Platform.runLater {
            techBuiltChart!!.data = FXCollections.observableArrayList<Data>(
                    PieChart.Data(i18n.get("stats.tech1"), tech1Built.toDouble()),
                    PieChart.Data(i18n.get("stats.tech2"), tech2Built.toDouble()),
                    PieChart.Data(i18n.get("stats.tech3"), tech3Built.toDouble())
            )
        }
    }

    private fun plotGamesPlayedChart() {
        val currentPlayer = playerService.currentPlayer.orElseThrow { IllegalStateException("Player must be set") }
        leaderboardService.getEntryForPlayer(currentPlayer.id).thenAccept { leaderboardEntryBean ->
            Platform.runLater {
                val ladderGamesCount = leaderboardEntryBean.gamesPlayed
                val custonGamesCount = currentPlayer.numberOfGames
                Platform.runLater {
                    gamesPlayedChart!!.data = FXCollections.observableArrayList<Data>(
                            PieChart.Data(i18n.get("stats.custom"), custonGamesCount.toDouble()),
                            PieChart.Data(i18n.get("stats.ranked1v1"), ladderGamesCount.toDouble())
                    )
                }
            }
        }.exceptionally { throwable ->
            log.warn("Leaderboard entry could not be read for current player: " + currentPlayer.username, throwable)
            null
        }
    }

    private fun enterAchievementsLoadingState() {
        loadingProgressLabel!!.isVisible = true
        achievementsPane!!.isVisible = false
    }

    private fun updatePlayerAchievements(playerAchievements: List<PlayerAchievement>) {
        var mostRecentPlayerAchievement: PlayerAchievement? = null

        val children = unlockedAchievementsContainer!!.children
        Platform.runLater { children.clear() }

        for (playerAchievement in playerAchievements) {
            val achievementItemController = achievementItemById[playerAchievement.getAchievement().getId()]
            achievementItemController.setPlayerAchievement(playerAchievement)

            if (isUnlocked(playerAchievement)) {
                Platform.runLater { children.add(achievementItemController.root) }
                if (mostRecentPlayerAchievement == null || playerAchievement.getUpdateTime().compareTo(mostRecentPlayerAchievement.getUpdateTime()) > 0) {
                    mostRecentPlayerAchievement = playerAchievement
                }
            }
        }

        if (mostRecentPlayerAchievement == null) {
            mostRecentAchievementPane!!.isVisible = false
        } else {
            mostRecentAchievementPane!!.isVisible = true
            val mostRecentAchievement = achievementDefinitionById[mostRecentPlayerAchievement.getAchievement().getId()]
                    ?: return
            val mostRecentAchievementName = mostRecentAchievement.getName()
            val mostRecentAchievementDescription = mostRecentAchievement.getDescription()

            Platform.runLater {
                mostRecentAchievementNameLabel!!.text = mostRecentAchievementName
                mostRecentAchievementDescriptionLabel!!.text = mostRecentAchievementDescription
                mostRecentAchievementImageView!!.image = achievementService.getImage(mostRecentAchievement, UNLOCKED)
            }
        }
    }

    private fun enterAchievementsLoadedState() {
        loadingProgressLabel!!.isVisible = false
        achievementsPane!!.isVisible = true
    }

    fun ladder1v1ButtonClicked() {
        loadStatistics(KnownFeaturedMod.LADDER_1V1)
    }

    private fun loadStatistics(featuredMod: KnownFeaturedMod): CompletableFuture<Void> {
        return statisticsService.getRatingHistory(featuredMod, player!!.id)
                .thenAccept { ratingHistory -> Platform.runLater { plotPlayerRatingGraph(ratingHistory) } }
                .exceptionally { throwable ->
                    // FIXME display to user
                    log.warn("Statistics could not be loaded", throwable)
                    null
                }
    }

    private fun plotPlayerRatingGraph(dataPoints: List<RatingHistoryDataPoint>) {
        val values = dataPoints.stream()
                .map { datapoint -> Data(dataPoints.indexOf(datapoint), RatingUtil.getRating(datapoint)) }
                .collect<List<XYChart.Data<Int, Int>>, Any>(Collectors.toList())

        xAxis!!.tickLabelFormatter = ratingLabelFormatter(dataPoints)

        val series = XYChart.Series(observableList(values))
        series.name = i18n.get("userInfo.ratingOverTime")
        ratingHistoryChart!!.data.setAll(series)
    }

    private fun ratingLabelFormatter(dataPoints: List<RatingHistoryDataPoint>): StringConverter<Number> {
        return object : StringConverter<Number>() {
            override fun toString(`object`: Number): String {
                val number = `object`.toInt()
                val numberOfDataPoints = dataPoints.size
                val dataPointIndex = if (number >= numberOfDataPoints) numberOfDataPoints - 1 else number
                return if (dataPointIndex >= dataPoints.size || dataPointIndex < 0) {
                    ""
                } else DATE_FORMATTER.format(dataPoints[dataPointIndex].getInstant())
            }

            override fun fromString(string: String): Number? {
                return null
            }
        }
    }

    fun globalButtonClicked() {
        loadStatistics(KnownFeaturedMod.FAF)
    }

    fun show() {
        Assert.checkNullIllegalState(ownerWindow, "ownerWindow must be set")
        val userInfoWindow = Stage(StageStyle.TRANSPARENT)
        userInfoWindow.initModality(Modality.NONE)
        userInfoWindow.initOwner(ownerWindow)

        val scene = uiService.createScene(userInfoWindow, userInfoRoot)
        userInfoWindow.scene = scene
        userInfoWindow.show()
        userInfoWindow.showingProperty().addListener { observable, oldValue, newValue ->
            if (!newValue) {
                userInfoRoot!!.children.clear()
            }
        }
    }

    fun setOwnerWindow(ownerWindow: Window) {
        this.ownerWindow = ownerWindow
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMM")

        private fun isUnlocked(playerAchievement: PlayerAchievement): Boolean {
            return UNLOCKED == AchievementState.valueOf(playerAchievement.getState().name())
        }
    }

}
