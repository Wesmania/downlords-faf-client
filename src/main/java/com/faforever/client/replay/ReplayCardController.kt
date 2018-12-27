package com.faforever.client.replay

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.map.MapBean
import com.faforever.client.map.MapService
import com.faforever.client.map.MapService.PreviewSize
import com.faforever.client.rating.RatingService
import com.faforever.client.util.RatingUtil
import com.faforever.client.util.TimeService
import com.faforever.client.vault.review.Review
import com.faforever.client.vault.review.StarsController
import com.google.common.base.Joiner
import com.jfoenix.controls.JFXRippler
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.beans.WeakInvalidationListener
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject
import java.time.Duration
import java.util.Optional
import java.util.function.Consumer
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReplayCardController @Inject
constructor(private val timeService: TimeService, private val mapService: MapService, private val ratingService: RatingService, private val i18n: I18n) : Controller<Node> {

    var dateLabel: Label? = null
    var mapThumbnailImageView: ImageView? = null
    var gameTitleLabel: Label? = null
    var replayTileRoot: Node? = null
    var timeLabel: Label? = null
    var modLabel: Label? = null
    var durationLabel: Label? = null
    var playerCountLabel: Label? = null
    var ratingLabel: Label? = null
    var qualityLabel: Label? = null
    var numberOfReviewsLabel: Label? = null
    var playerListLabel: Label? = null
    var onMapLabel: Label? = null
    var starsController: StarsController? = null

    private var replay: Replay? = null
    private var onOpenDetailListener: Consumer<Replay>? = null
    private val reviewsChangedListener: InvalidationListener
    private var jfxRippler: JFXRippler? = null

    override val root: Node?
        get() = jfxRippler

    init {
        reviewsChangedListener = { observable -> populateReviews() }
    }

    override fun initialize() {
        jfxRippler = JFXRippler(replayTileRoot)
    }

    fun setReplay(replay: Replay) {
        this.replay = replay

        val optionalMap = Optional.ofNullable(replay.map)
        if (optionalMap.isPresent) {
            val map = optionalMap.get()
            val image = mapService.loadPreview(map, PreviewSize.SMALL)
            mapThumbnailImageView!!.image = image
            onMapLabel!!.text = i18n.get("game.onMapFormat", map.displayName)
        } else {
            onMapLabel!!.text = i18n.get("game.onUnknownMap")
        }

        gameTitleLabel!!.text = replay.title
        dateLabel!!.text = timeService.asDate(replay.startTime)
        timeLabel!!.text = timeService.asShortTime(replay.startTime)
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

        durationLabel!!.text = Optional.ofNullable<Temporal>(replay.endTime)
                .map { endTime -> timeService.shortDuration(Duration.between(replay.startTime, endTime)) }
                .orElse(i18n.get("notAvailable"))

        val players = replay.teams.values.stream()
                .map { team -> Joiner.on(i18n.get("textSeparator")).join(team) }
                .collect<String, *>(Collectors.joining(i18n.get("vsSeparator")))
        playerListLabel!!.text = players

        val reviews = replay.reviews
        JavaFxUtil.addListener(reviews, WeakInvalidationListener(reviewsChangedListener))
        reviewsChangedListener.invalidated(reviews)
    }

    private fun populateReviews() {
        val reviews = replay!!.reviews
        Platform.runLater {
            numberOfReviewsLabel!!.text = i18n.number(reviews.size)
            starsController!!.value = reviews.stream().mapToInt(ToIntFunction<Review> { it.getScore() }).average().orElse(0.0).toFloat()
        }
    }

    fun setOnOpenDetailListener(onOpenDetailListener: Consumer<Replay>) {
        this.onOpenDetailListener = onOpenDetailListener
    }

    fun onShowReplayDetail() {
        onOpenDetailListener!!.accept(replay)
    }
}
