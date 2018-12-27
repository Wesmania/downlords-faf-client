package com.faforever.client.vault.review


import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import com.faforever.client.theme.UiService
import com.google.common.base.Strings
import com.google.common.collect.Iterables
import com.google.common.collect.Lists
import javafx.application.Platform
import javafx.beans.InvalidationListener
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import java.util.IntSummaryStatistics
import java.util.Optional
import java.util.function.Consumer
import java.util.stream.Collectors

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReviewsController(private val i18n: I18n, private val uiService: UiService, private val playerService: PlayerService) : Controller<Pane> {

    override var root: Pane? = null
    var scoreLabel: Label? = null
    var fiveStarsBar: Pane? = null
    var fourStarsBar: Pane? = null
    var threeStarsBar: Pane? = null
    var twoStarsBar: Pane? = null
    var oneStarBar: Pane? = null
    var ratingsGrid: GridPane? = null
    var totalReviewsLabel: Label? = null
    var createReviewButton: Button? = null
    var averageStarsController: StarsController? = null
    /**
     * Not named `ownReviewController` because `ownReview` clashes with [.ownReview].
     */
    var ownReviewPaneController: ReviewController? = null
    var ownReviewLabel: Label? = null
    var otherReviewsContainer: Pane? = null
    var reviewsPagination: Pane? = null
    var pageLeftButton: Button? = null
    var pageRightButton: Button? = null

    private var onSendReviewListener: Consumer<Review>? = null
    private var ownReviewRoot: Pane? = null
    private var onDeleteReviewListener: Consumer<Review>? = null
    private var reviews: ObservableList<Review>? = null
    private val onReviewsChangedListener: InvalidationListener
    private var ownReview: Optional<Review>? = null
    private var reviewPages: List<List<Review>>? = null
    private var currentReviewPage: Int = 0

    init {
        onReviewsChangedListener = { observable -> Platform.runLater { this.onReviewsChanged() } }
        ownReview = Optional.empty()
    }

    override fun initialize() {
        ownReviewRoot = ownReviewPaneController!!.root
        JavaFxUtil.setAnchors(ownReviewRoot, 0.0)
        ownReviewRoot!!.managedProperty().bind(ownReviewRoot!!.visibleProperty())
        ownReviewRoot!!.isVisible = false
        ownReviewPaneController!!.setOnDeleteReviewListener { this.onDeleteReview(it) }
        ownReviewPaneController!!.setOnCancelListener { this.onCancelReview() }
        ownReviewPaneController!!.setOnSendReviewListener { review -> onSendReviewListener!!.accept(review) }

        // Prevent flickering
        setReviews(FXCollections.emptyObservableList())
        createReviewButton!!.managedProperty().bind(createReviewButton!!.visibleProperty())
        createReviewButton!!.isVisible = false

        ownReviewLabel!!.managedProperty().bind(ownReviewLabel!!.visibleProperty())
        ownReviewLabel!!.isVisible = false

        pageLeftButton!!.managedProperty().bind(pageLeftButton!!.visibleProperty())
        reviewsPagination!!.managedProperty().bind(reviewsPagination!!.visibleProperty())
        pageRightButton!!.managedProperty().bind(pageRightButton!!.visibleProperty())
    }

    private fun onDeleteReview(review: Review) {
        Optional.ofNullable(this.onDeleteReviewListener).ifPresent { listener -> listener.accept(review) }
    }

    fun onCreateReviewButtonClicked() {
        ownReviewRoot!!.isVisible = true
        createReviewButton!!.isVisible = false
        ownReviewPaneController!!.setReview(Optional.empty())
    }

    private fun onCancelReview() {
        setOwnReview(this.ownReview)
    }

    fun setOnSendReviewListener(onSendReviewListener: Consumer<Review>) {
        this.onSendReviewListener = onSendReviewListener
    }

    fun setReviews(reviews: ObservableList<Review>) {
        this.reviews = reviews

        val currentPlayer = playerService.currentPlayer
                .orElseThrow { IllegalStateException("No current player available") }

        JavaFxUtil.addListener(reviews, onReviewsChangedListener)
        val onlyOtherNonEmptyReviews = reviews
                .filtered { review -> review.player.id != currentPlayer.id && !Strings.isNullOrEmpty(review.text) }

        reviewPages = Lists.newArrayList(Iterables.partition(onlyOtherNonEmptyReviews, REVIEWS_PER_PAGE))
        currentReviewPage = Math.max(0, Math.min(0, reviewPages!!.size - 1))
        reviewsPagination!!.isVisible = !reviewPages!!.isEmpty()
        displayReviewsPage(0)

        // Prevent flickering
        if (Platform.isFxApplicationThread()) {
            onReviewsChanged()
        } else {
            Platform.runLater { this.onReviewsChanged() }
        }
    }

    fun setCanWriteReview(canWriteReview: Boolean) {
        createReviewButton!!.isVisible = canWriteReview
    }

    private fun displayReviewsPage(page: Int) {
        if (page >= reviewPages!!.size) {
            return
        }
        pageLeftButton!!.isVisible = page > 0
        pageRightButton!!.isVisible = page < reviewPages!!.size - 1

        val reviewsPage = reviewPages!![currentReviewPage]
        val reviewNodes = reviewsPage.stream()
                .map { review ->
                    val controller = uiService.loadFxml<ReviewController>("theme/vault/review/review.fxml")
                    controller.setReview(Optional.of(review))
                    controller.root
                }
                .collect<List<Pane>, Any>(Collectors.toList())

        Platform.runLater { otherReviewsContainer!!.children.setAll(reviewNodes) }
    }

    private fun onReviewsChanged() {
        JavaFxUtil.assertApplicationThread()

        val ratingOccurrences = reviews!!.stream()
                .collect<Map<Int, Long>, Any>(Collectors.groupingBy<Review, Int, Any, Long>(Function<Review, Int> { it.getScore() }, Collectors.counting()))

        val fiveStars = (ratingOccurrences as java.util.Map<Int, Long>).getOrDefault(5, 0L)
        val fourStars = (ratingOccurrences as java.util.Map<Int, Long>).getOrDefault(4, 0L)
        val threeStars = (ratingOccurrences as java.util.Map<Int, Long>).getOrDefault(3, 0L)
        val twoStars = (ratingOccurrences as java.util.Map<Int, Long>).getOrDefault(2, 0L)
        val oneStars = (ratingOccurrences as java.util.Map<Int, Long>).getOrDefault(1, 0L)

        val totalReviews = Math.max(reviews!!.size, 1)
        val fiveStarsPercentage = fiveStars as Float / totalReviews
        val fourStarsPercentage = fourStars as Float / totalReviews
        val threeStarsPercentage = threeStars as Float / totalReviews
        val twoStarsPercentage = twoStars as Float / totalReviews
        val oneStarPercentage = oneStars as Float / totalReviews

        // So that the bars' parents have their sizes
        root!!.applyCss()
        root!!.layout()

        totalReviewsLabel!!.text = i18n.get("reviews.totalReviewers", reviews!!.size)
        fiveStarsBar!!.prefWidthProperty().bind((fiveStarsBar!!.parent as Pane).widthProperty().multiply(fiveStarsPercentage))
        fourStarsBar!!.prefWidthProperty().bind((fourStarsBar!!.parent as Pane).widthProperty().multiply(fourStarsPercentage))
        threeStarsBar!!.prefWidthProperty().bind((threeStarsBar!!.parent as Pane).widthProperty().multiply(threeStarsPercentage))
        twoStarsBar!!.prefWidthProperty().bind((twoStarsBar!!.parent as Pane).widthProperty().multiply(twoStarsPercentage))
        oneStarBar!!.prefWidthProperty().bind((oneStarBar!!.parent as Pane).widthProperty().multiply(oneStarPercentage))

        val statistics = reviews!!.stream().mapToInt(ToIntFunction<Review> { it.getScore() }).summaryStatistics()
        val average = statistics.average.toFloat()
        scoreLabel!!.text = i18n.rounded(average.toDouble(), 1)
        averageStarsController!!.value = average
    }

    fun setOwnReview(ownReview: Optional<Review>?) {
        this.ownReview = ownReview
        Platform.runLater {
            if (ownReview!!.isPresent) {
                ownReviewPaneController!!.setReview(ownReview)
                ownReviewRoot!!.isVisible = true
                createReviewButton!!.isVisible = false
                ownReviewLabel!!.isVisible = true
            } else {
                ownReviewRoot!!.isVisible = false
                createReviewButton!!.isVisible = true
                ownReviewLabel!!.isVisible = false
            }
        }
    }

    fun setOnDeleteReviewListener(onDeleteReviewListener: Consumer<Review>) {
        this.onDeleteReviewListener = onDeleteReviewListener
    }

    fun onPageLeftButtonClicked() {
        displayReviewsPage(--currentReviewPage)
    }

    fun onPageRightButtonClicked() {
        displayReviewsPage(++currentReviewPage)
    }

    companion object {
        private val REVIEWS_PER_PAGE = 4
    }
}
