package com.faforever.client.replay

import com.faforever.client.api.dto.Validity
import com.faforever.client.config.ClientProperties
import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.fx.StringCell
import com.faforever.client.game.RatingType
import com.faforever.client.game.TeamCardController
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.rating.RatingService
import com.faforever.client.replay.Replay.ChatMessage
import com.faforever.client.replay.Replay.GameOption
import com.faforever.client.replay.Replay.PlayerStats
import com.faforever.client.theme.UiService
import com.faforever.client.util.Rating
import com.faforever.client.util.RatingUtil
import com.faforever.client.util.TimeService
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.review.ReviewService
import com.faforever.client.vault.review.ReviewsController
import com.faforever.commons.io.Bytes
import javafx.application.Platform
import javafx.collections.ObservableMap
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import lombok.Setter
import lombok.extern.slf4j.Slf4j
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.time.Duration
import java.time.temporal.Temporal
import java.util.ArrayList
import java.util.Optional
import java.util.function.Function
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Slf4j
class ReplayDetailController(private val timeService: TimeService, private val i18n: I18n, private val uiService: UiService, private val replayService: ReplayService,
                             private val ratingService: RatingService, private val mapService: MapService, private val playerService: PlayerService,
                             private val reviewService: ReviewService, private val clientProperties: ClientProperties) : Controller<Node> {
    var replayDetailRoot: Pane? = null
    var titleLabel: Label? = null
    var copyButton: Button? = null
    var dateLabel: Label? = null
    var timeLabel: Label? = null
    var modLabel: Label? = null
    var durationLabel: Label? = null
    var playerCountLabel: Label? = null
    var ratingLabel: Label? = null
    var qualityLabel: Label? = null
    var onMapLabel: Label? = null
    var teamsContainer: Pane? = null
    var reviewsController: ReviewsController? = null
    var chatTable: TableView<ChatMessage>? = null
    var chatGameTimeColumn: TableColumn<ChatMessage, Duration>? = null
    var chatSenderColumn: TableColumn<ChatMessage, String>? = null
    var chatMessageColumn: TableColumn<ChatMessage, String>? = null
    var optionsTable: TableView<GameOption>? = null
    var optionKeyColumn: TableColumn<GameOption, String>? = null
    var optionValueColumn: TableColumn<GameOption, String>? = null
    var downloadMoreInfoButton: Button? = null
    var moreInformationPane: Pane? = null
    var mapThumbnailImageView: ImageView? = null
    var watchButton: Button? = null
    var replayIdField: TextField? = null
    var scrollPane: ScrollPane? = null
    var showRatingChangeButton: Button? = null
    @Setter
    private var onClosure: Runnable? = null
    private var replay: Replay? = null
    private val teamCardControllers = ArrayList<TeamCardController>()
    private var teams: ObservableMap<String, List<PlayerStats>>? = null

    override val root: Node?
        get() = replayDetailRoot

    override fun initialize() {
        JavaFxUtil.fixScrollSpeed(scrollPane!!)

        chatGameTimeColumn!!.setCellValueFactory { param -> param.value.timeProperty() }
        chatGameTimeColumn!!.setCellFactory { param -> StringCell(Function { timeService.asHms(it) }) }

        chatSenderColumn!!.setCellValueFactory { param -> param.value.senderProperty() }
        chatSenderColumn!!.setCellFactory { param -> StringCell(Function { it.toString() }) }

        chatMessageColumn!!.setCellValueFactory { param -> param.value.messageProperty() }
        chatMessageColumn!!.setCellFactory { param -> StringCell(Function { it.toString() }) }

        optionKeyColumn!!.setCellValueFactory { param -> param.value.keyProperty() }
        optionKeyColumn!!.setCellFactory { param -> StringCell(Function { it.toString() }) }

        optionValueColumn!!.setCellValueFactory { param -> param.value.valueProperty() }
        optionValueColumn!!.setCellFactory { param -> StringCell(Function { it.toString() }) }

        downloadMoreInfoButton!!.managedProperty().bind(downloadMoreInfoButton!!.visibleProperty())
        moreInformationPane!!.managedProperty().bind(moreInformationPane!!.visibleProperty())
        moreInformationPane!!.isVisible = false

        reviewsController!!.root!!.setMaxSize(Integer.MAX_VALUE.toDouble(), Integer.MAX_VALUE.toDouble())

        copyButton!!.text = i18n.get("replay.copyUrl")
        onClosure = { (replayDetailRoot!!.parent as Pane).children.remove(replayDetailRoot) }
    }

    fun setReplay(replay: Replay) {
        this.replay = replay

        replayIdField!!.text = i18n.get("game.idFormat", replay.id)
        titleLabel!!.text = replay.title
        dateLabel!!.text = timeService.asDate(replay.startTime)
        timeLabel!!.text = timeService.asShortTime(replay.startTime)

        val optionalMap = Optional.ofNullable(replay.map)
        if (optionalMap.isPresent) {
            val map = optionalMap.get()
            val image = mapService.loadPreview(map, PreviewSize.LARGE)
            mapThumbnailImageView!!.image = image
            onMapLabel!!.text = i18n.get("game.onMapFormat", map.displayName)
        } else {
            onMapLabel!!.text = i18n.get("game.onUnknownMap")
        }

        val endTime = replay.endTime
        if (endTime != null) {
            durationLabel!!.text = timeService.shortDuration(Duration.between(replay.startTime, endTime))
        } else {
            durationLabel!!.isVisible = false
        }

        modLabel!!.text = replay.featuredMod.displayName
        playerCountLabel!!.text = i18n.number(replay.teams.values.stream().mapToInt(ToIntFunction<List<String>> { it.size }).sum())
        qualityLabel!!.text = i18n.get("percentage", ratingService.calculateQuality(replay).toInt() * 100)

        replay.teamPlayerStats.values.stream()
                .flatMapToInt { playerStats ->
                    playerStats.stream()
                            .mapToInt { stats -> RatingUtil.getRating(stats.getBeforeMean(), stats.getBeforeDeviation()) }
                }
                .average()
                .ifPresent { averageRating -> ratingLabel!!.text = i18n.number(averageRating.toInt()) }

        replayService.getSize(replay.id)
                .thenAccept { replaySize ->
                    Platform.runLater {
                        if (replaySize > -1) {
                            val humanReadableSize = Bytes.formatSize(replaySize!!.toLong(), i18n.userSpecificLocale)
                            downloadMoreInfoButton!!.text = i18n.get("game.downloadMoreInfo", humanReadableSize)
                            watchButton!!.text = i18n.get("game.watchButtonFormat", humanReadableSize)
                        } else {
                            downloadMoreInfoButton!!.text = i18n.get("game.replayFileMissing")
                            downloadMoreInfoButton!!.isDisable = true
                            watchButton!!.text = i18n.get("game.replayFileMissing")
                            watchButton!!.isDisable = true
                        }
                    }
                }

        val currentPlayer = playerService.currentPlayer
        Assert.state(currentPlayer.isPresent, "No user is logged in")

        reviewsController!!.setOnSendReviewListener(Consumer<Review> { this.onSendReview(it) })
        reviewsController!!.setOnDeleteReviewListener(Consumer<Review> { this.onDeleteReview(it) })
        reviewsController!!.setReviews(replay.reviews)
        reviewsController!!.setOwnReview(replay.reviews.stream()
                .filter { review -> review.player == currentPlayer.get() }
                .findFirst())

        // These items are initially empty but will be populated in #onDownloadMoreInfoClicked()
        optionsTable!!.setItems(replay.gameOptions)
        chatTable!!.setItems(replay.chatMessages)
        teams = replay.teamPlayerStats
        populateTeamsContainer()
    }

    private fun onDeleteReview(review: Review) {
        reviewService.deleteGameReview(review)
                .thenRun {
                    Platform.runLater {
                        replay!!.reviews.remove(review)
                        reviewsController!!.setOwnReview(Optional.empty())
                    }
                }
                // TODO display error to user
                .exceptionally { throwable ->
                    log.warn("Review could not be saved", throwable)
                    null
                }
    }

    private fun onSendReview(review: Review) {
        val isNew = review.id == null
        val player = playerService.currentPlayer
                .orElseThrow { IllegalStateException("No current player is available") }
        review.player = player
        reviewService.saveGameReview(review, replay!!.id)
                .thenRun {
                    if (isNew) {
                        replay!!.reviews.add(review)
                    }
                    reviewsController!!.setOwnReview(Optional.of(review))
                }
                // TODO display error to user
                .exceptionally { throwable ->
                    log.warn("Review could not be saved", throwable)
                    null
                }
    }

    fun onDownloadMoreInfoClicked() {
        // TODO display loading indicator
        downloadMoreInfoButton!!.isVisible = false
        replayService.downloadReplay(replay!!.id)
                .thenAccept { path ->
                    replayService.enrich(replay, path)
                    chatTable!!.setItems(replay!!.chatMessages)
                    optionsTable!!.setItems(replay!!.gameOptions)
                    moreInformationPane!!.isVisible = true
                }
                .exceptionally { throwable ->
                    log.error("Replay could not be enriched", throwable)
                    null
                }
    }

    private fun populateTeamsContainer() {
        if (replay!!.validity != Validity.VALID) {
            showRatingChangeButton!!.isDisable = true
            showRatingChangeButton!!.text = i18n.get("game.notValid")
        } else if (!replayService.replayChangedRating(replay)) {
            showRatingChangeButton!!.isDisable = true
            showRatingChangeButton!!.text = i18n.get("game.notRatedYet")
        }
        val statsByPlayerId = teams!!.values.stream()
                .flatMap { it.stream() }
                .collect<Map<Int, PlayerStats>, Any>(Collectors.toMap(Function<Any, Any> { getPlayerId() }, Function.identity<Any>()))

        Platform.runLater {
            teams!!.forEach { team, value ->
                val playerIds = value.stream()
                        .map(Function<PlayerStats, Any> { getPlayerId() })
                        .collect<List<Int>, Any>(Collectors.toList<Any>())


                val controller = uiService.loadFxml<TeamCardController>("theme/team_card.fxml")
                teamCardControllers.add(controller)
                playerService.getPlayersByIds(playerIds)
                        .thenAccept { players ->
                            controller.setPlayersInTeam(team, players, { player ->
                                val playerStats = statsByPlayerId[player.id]
                                Rating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation())
                            }, RatingType.EXACT)
                        }

                teamsContainer!!.children.add(controller.root)
            }
        }
    }

    fun onCloseButtonClicked() {
        onClosure!!.run()
    }

    fun onDimmerClicked() {
        onCloseButtonClicked()
    }

    fun onContentPaneClicked(event: MouseEvent) {
        event.consume()
    }

    fun onWatchButtonClicked() {
        replayService.runReplay(replay)
    }


    fun copyLink() {
        val clipboard = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        content.putString(Replay.getReplayUrl(replay!!.id, clientProperties.getVault().getReplayDownloadUrlFormat()))
        clipboard.setContent(content)
    }

    fun showRatingChange() {
        teamCardControllers.forEach { teamCardController -> teamCardController.showRatingChange(teams) }
        showRatingChangeButton!!.isVisible = false
    }
}
