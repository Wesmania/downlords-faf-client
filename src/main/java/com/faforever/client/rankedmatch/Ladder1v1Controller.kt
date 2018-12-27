package com.faforever.client.rankedmatch

import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.AbstractViewController
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.game.Faction
import com.faforever.client.game.GameService
import com.faforever.client.i18n.I18n
import com.faforever.client.leaderboard.LeaderboardService
import com.faforever.client.leaderboard.RatingStat
import com.faforever.client.main.event.ShowLadderMapsEvent
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.preferences.PreferenceUpdateListener
import com.faforever.client.preferences.PreferencesService
import com.faforever.client.preferences.event.MissingGamePathEvent
import com.faforever.client.util.RatingUtil
import com.google.common.annotations.VisibleForTesting
import com.google.common.eventbus.EventBus
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.collections.ObservableList
import javafx.css.PseudoClass
import javafx.event.ActionEvent
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToggleButton
import javafx.scene.text.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.lang.invoke.MethodHandles
import java.lang.ref.WeakReference
import java.util.Comparator
import java.util.EnumSet
import java.util.HashMap
import java.util.Random
import java.util.stream.Collectors

import com.faforever.client.game.Faction.AEON
import com.faforever.client.game.Faction.CYBRAN
import com.faforever.client.game.Faction.SERAPHIM
import com.faforever.client.game.Faction.UEF

@Component
@Lazy
class Ladder1v1Controller @Inject
constructor(private val gameService: GameService,
            private val preferencesService: PreferencesService,
            private val playerService: PlayerService,
            private val leaderboardService: LeaderboardService,
            private val i18n: I18n, private val clientProperties: ClientProperties,
            private val eventBus: EventBus) : AbstractViewController<Node>() {

    private val random: Random

    var ratingDistributionXAxis: CategoryAxis? = null
    var ratingDistributionYAxis: NumberAxis? = null
    var ratingDistributionChart: BarChart<String, Int>? = null
    var ratingHintLabel: Label? = null
    var searchingForOpponentLabel: Label? = null
    var ratingLabel: Label? = null
    var searchProgressIndicator: ProgressIndicator? = null
    var ratingProgressIndicator: ProgressIndicator? = null
    var aeonButton: ToggleButton? = null
    var uefButton: ToggleButton? = null
    var cybranButton: ToggleButton? = null
    var seraphimButton: ToggleButton? = null
    var cancelButton: Button? = null
    var playButton: Button? = null
    var ladder1v1Root: ScrollPane? = null
    var gamesPlayedLabel: Label? = null
    var rankingLabel: Label? = null
    var winLossRationLabel: Label? = null
    var rankingOutOfLabel: Label? = null

    private val youLabel: Text

    @VisibleForTesting
    internal var factionsToButtons: HashMap<Faction, ToggleButton>

    // Kept as a field in order to prevent garbage collection
    private var playerRatingListener: InvalidationListener? = null
    private var preferenceUpdateListener: PreferenceUpdateListener? = null

    override val root: Node?
        get() = ladder1v1Root

    init {

        random = Random()

        youLabel = Text(i18n.get("ranked1v1.you"))
        youLabel.id = "1v1-you-text"
    }

    override fun initialize() {
        super.initialize()
        cancelButton!!.managedProperty().bind(cancelButton!!.visibleProperty())
        playButton!!.managedProperty().bind(playButton!!.visibleProperty())
        ratingLabel!!.managedProperty().bind(ratingLabel!!.visibleProperty())
        ratingProgressIndicator!!.managedProperty().bind(ratingProgressIndicator!!.visibleProperty())

        factionsToButtons = HashMap()
        factionsToButtons[AEON] = aeonButton
        factionsToButtons[UEF] = uefButton
        factionsToButtons[CYBRAN] = cybranButton
        factionsToButtons[SERAPHIM] = seraphimButton

        setSearching(false)

        JavaFxUtil.addListener(gameService.searching1v1Property()) { observable, oldValue, newValue -> setSearching(newValue!!) }

        val factions = preferencesService.preferences!!.ladder1v1Prefs.factions
        for (faction in EnumSet.of(AEON, CYBRAN, UEF, SERAPHIM)) {
            factionsToButtons[faction].setSelected(factions.contains(faction))
        }
        playButton!!.isDisable = factionsToButtons.values.stream().noneMatch(Predicate<ToggleButton> { it.isSelected() })

        preferenceUpdateListener = { preferences ->
            if (preferencesService.preferences!!.forgedAlliance.path == null) {
                onCancelButtonClicked()
            }
        }
        preferencesService.addUpdateListener(WeakReference(preferenceUpdateListener))

        JavaFxUtil.addListener(playerService.currentPlayerProperty()) { observable, oldValue, newValue -> Platform.runLater { setCurrentPlayer(newValue) } }
        playerService.currentPlayer.ifPresent(Consumer<Player> { this.setCurrentPlayer(it) })
    }

    @VisibleForTesting
    internal fun setSearching(searching: Boolean) {
        cancelButton!!.isVisible = searching
        playButton!!.isVisible = !searching
        searchProgressIndicator!!.isVisible = searching
        searchingForOpponentLabel!!.isVisible = searching
        setFactionButtonsDisabled(searching)
    }

    private fun setFactionButtonsDisabled(disabled: Boolean) {
        factionsToButtons.values.forEach { button -> button.isDisable = disabled }
    }

    fun onCancelButtonClicked() {
        gameService.stopSearchLadder1v1()
        setSearching(false)
    }

    fun onPlayButtonClicked() {
        if (preferencesService.preferences!!.forgedAlliance.path == null) {
            eventBus.post(MissingGamePathEvent(true))
            return
        }

        setFactionButtonsDisabled(true)

        val factions = preferencesService.preferences!!.ladder1v1Prefs.factions

        val randomFaction = factions[random.nextInt(factions.size)]
        gameService.startSearchLadder1v1(randomFaction)
    }

    fun onFactionButtonClicked() {
        val factions = factionsToButtons.entries.stream()
                .filter { entry -> entry.value.isSelected }
                .map<Faction>(Function<Entry<Faction, ToggleButton>, Faction> { it.key })
                .collect<List<Faction>, Any>(Collectors.toList())

        preferencesService.preferences!!.ladder1v1Prefs.factions.setAll(factions)
        preferencesService.storeInBackground()

        playButton!!.isDisable = factions.isEmpty()
    }

    private fun setCurrentPlayer(player: Player) {
        playerRatingListener = { ratingObservable -> updateRating(player) }

        JavaFxUtil.addListener(player.leaderboardRatingDeviationProperty(), WeakInvalidationListener(playerRatingListener!!))
        JavaFxUtil.addListener(player.leaderboardRatingMeanProperty(), WeakInvalidationListener(playerRatingListener!!))
        updateRating(player)
        updateOtherValues(player)
    }

    private fun updateRating(player: Player) {
        val rating = RatingUtil.getLeaderboardRating(player)
        val beta = clientProperties.getTrueSkill().getBeta()
        val deviation = player.leaderboardRatingDeviation

        if (deviation > beta) {
            val initialStandardDeviation = clientProperties.getTrueSkill().getInitialStandardDeviation()
            ratingProgressIndicator!!.progress = ((initialStandardDeviation - deviation) / beta).toDouble()
            ratingProgressIndicator!!.isVisible = true
            ratingLabel!!.isVisible = false
            ratingHintLabel!!.text = i18n.get("ranked1v1.ratingProgress.stillLearning")
        } else {
            ratingProgressIndicator!!.isVisible = false
            ratingLabel!!.isVisible = true
            ratingLabel!!.text = i18n.number(rating)
            ratingHintLabel!!.isVisible = false
            updateRatingHint(rating)
        }

        leaderboardService.ladder1v1Stats
                .thenAccept { ranked1v1Stats ->
                    ranked1v1Stats.sort(Comparator.comparingInt(ToIntFunction<T> { getRating() }))
                    var totalPlayers = 0
                    for (entry in ranked1v1Stats) {
                        totalPlayers += entry.getTotalCount()
                    }
                    plotRatingDistributions(ranked1v1Stats, player)
                    val rankingOutOfText = i18n.get("ranked1v1.rankingOutOf", totalPlayers)
                    Platform.runLater { rankingOutOfLabel!!.text = rankingOutOfText }
                }
                .exceptionally { throwable ->
                    logger.warn("Could not plot rating distribution", throwable)
                    null
                }
    }

    private fun updateOtherValues(currentPlayer: Player) {
        leaderboardService.getEntryForPlayer(currentPlayer.id).thenAccept { leaderboardEntryBean ->
            Platform.runLater {
                rankingLabel!!.text = i18n.get("ranked1v1.rankingFormat", leaderboardEntryBean.rank)
                gamesPlayedLabel!!.text = String.format("%d", leaderboardEntryBean.gamesPlayed)
                winLossRationLabel!!.text = i18n.get("percentage", leaderboardEntryBean.winLossRatio * 100)
            }
        }.exceptionally { throwable ->
            // Debug instead of warn, since it's fairly common that players don't have a leaderboard entry which causes a 404
            logger.debug("Leaderboard entry could not be read for current player: " + currentPlayer.username, throwable)
            null
        }
    }

    private fun updateRatingHint(rating: Int) {
        // TODO remove/rethink rating hint
        //    if (rating < environment.getProperty("rating.low", int.class)) {
        //      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.low"));
        //    } else if (rating < environment.getProperty("rating.moderate", int.class)) {
        //      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.moderate"));
        //    } else if (rating < environment.getProperty("rating.good", int.class)) {
        //      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.good"));
        //    } else if (rating < environment.getProperty("rating.high", int.class)) {
        //      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.high"));
        //    } else if (rating < environment.getProperty("rating.top", int.class)) {
        //      ratingHintLabel.setText(i18n.valueOf("ranked1v1.ratingHint.top"));
        //    }
    }

    private fun plotRatingDistributions(ratingStats: List<RatingStat>, player: Player) {
        val series = XYChart.Series<String, Int>()
        series.name = i18n.get("ranked1v1.players", LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN)
        val currentPlayerRating = RatingUtil.roundRatingToNextLowest100(RatingUtil.getLeaderboardRating(player).toDouble())

        series.data.addAll(ratingStats.stream()
                .sorted(Comparator.comparingInt(ToIntFunction<T> { getRating() }))
                .map { item ->
                    val rating = item.getRating()
                    val data = XYChart.Data<X, Y>(i18n.number(rating), item.getCountWithEnoughGamesPlayed())
                    if (rating == currentPlayerRating) {
                        data.nodeProperty().addListener { observable, oldValue, newValue ->
                            newValue.pseudoClassStateChanged(NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS, true)
                            addNodeOnTopOfBar(data, youLabel)
                        }
                    }
                    data
                }
                .collect(Collectors.toList<T>()))

        Platform.runLater { ratingDistributionChart!!.data.setAll(series) }
    }

    private fun addNodeOnTopOfBar(data: XYChart.Data<String, Int>, nodeToAdd: Node) {
        val node = data.node
        node.parentProperty().addListener { ov, oldParent, parent ->
            if (parent == null) {
                return@node.parentProperty().addListener
            }
            val parentGroup = parent as Group
            val children = parentGroup.children
            if (!children.contains(nodeToAdd)) {
                children.add(nodeToAdd)
            }
        }

        JavaFxUtil.addListener<Bounds>(node.boundsInParentProperty()) { ov, oldBounds, bounds ->
            nodeToAdd.layoutX = Math.round(bounds.getMinX() + bounds.getWidth() / 2 - nodeToAdd.prefWidth(-1.0) / 2).toDouble()
            nodeToAdd.layoutY = Math.round(bounds.getMinY() - nodeToAdd.prefHeight(-1.0) * 0.5).toDouble()
        }
    }

    fun showLadderMaps(actionEvent: ActionEvent) {
        eventBus.post(ShowLadderMapsEvent())
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
        private val NOTIFICATION_HIGHLIGHTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("highlighted-bar")
    }
}
