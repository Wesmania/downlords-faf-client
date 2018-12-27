package com.faforever.client.vault.review

import com.faforever.client.fx.Controller
import com.faforever.client.fx.JavaFxUtil
import com.faforever.client.i18n.I18n
import com.faforever.client.player.Player
import com.faforever.client.player.PlayerService
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import org.springframework.util.Assert

import java.util.Optional
import java.util.function.Consumer

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ReviewController(private val i18n: I18n, private val playerService: PlayerService) : Controller<Pane> {

    override var root: Pane? = null
    var displayReviewPane: Pane? = null
    var editReviewPane: Pane? = null
    var reviewTextArea: TextArea? = null
    var starsTipLabel: Label? = null
    var usernameLabel: Label? = null
    var selectionStarsController: StarsController? = null
    var displayStarsController: StarsController? = null
    var avatarImageView: ImageView? = null
    var reviewTextLabel: Label? = null
    var deleteButton: Button? = null
    var editButton: Button? = null

    private var onSendReviewListener: Consumer<Review>? = null
    private var onDeleteReviewListener: Consumer<Review>? = null
    private var review: Optional<Review>? = null
    private var onCancelReviewListener: Runnable? = null

    init {
        review = Optional.empty()
    }

    override fun initialize() {
        selectionStarsController!!.valueProperty().addListener { observable, oldValue, newValue -> starsTipLabel!!.text = i18n.get(STARS_TIP_KEYS[newValue.toInt() - 1]) }
        selectionStarsController!!.setSelectable(true)
        selectionStarsController!!.value = 4f

        displayReviewPane!!.managedProperty().bind(displayReviewPane!!.visibleProperty())
        editReviewPane!!.managedProperty().bind(editReviewPane!!.visibleProperty())
        editButton!!.managedProperty().bind(editButton!!.visibleProperty())
        deleteButton!!.managedProperty().bind(deleteButton!!.visibleProperty())

        displayReviewPane!!.isVisible = false
        editButton!!.isVisible = false
        deleteButton!!.isVisible = false
    }

    fun setReview(optionalReview: Optional<Review>) {
        JavaFxUtil.assertApplicationThread()
        this.review = optionalReview
        if (!optionalReview.isPresent) {
            editReviewPane!!.isVisible = true
            displayReviewPane!!.isVisible = false
            return
        }

        val currentPlayer = playerService.currentPlayer
                .orElseThrow { IllegalStateException("No player is available") }
        val definiteReview = optionalReview.get()

        val isReviewOwnedByCurrentUser = currentPlayer == definiteReview.player

        val rating = definiteReview.score!!
        selectionStarsController!!.value = rating.toFloat()
        displayStarsController!!.value = rating.toFloat()
        usernameLabel!!.text = definiteReview.player.username
        reviewTextLabel!!.text = definiteReview.text
        displayReviewPane!!.isVisible = true
        editReviewPane!!.isVisible = false
        editButton!!.isVisible = isReviewOwnedByCurrentUser
        deleteButton!!.isVisible = isReviewOwnedByCurrentUser
    }

    fun onDeleteButtonClicked() {
        Optional.ofNullable(onDeleteReviewListener).ifPresent { listener ->
            Assert.state(review!!.isPresent, "No review has been set")
            listener.accept(review!!.get())
        }
    }

    fun onEditButtonClicked() {
        Assert.state(review!!.isPresent, "No review has been set")

        reviewTextArea!!.text = review!!.get().text
        displayReviewPane!!.isVisible = false
        editReviewPane!!.isVisible = true
    }

    fun onSendReview() {
        val review = this.review!!.orElse(Review())
        review.score = Math.round(selectionStarsController!!.value)
        review.text = reviewTextArea!!.text
        this.onSendReviewListener!!.accept(review)
    }

    internal fun setOnSendReviewListener(onSendReviewListener: Consumer<Review>) {
        this.onSendReviewListener = onSendReviewListener
    }

    fun onCancelButtonClicked() {
        val reviewPresent = review!!.isPresent
        displayReviewPane!!.isVisible = reviewPresent
        editReviewPane!!.isVisible = false

        Optional.ofNullable(onCancelReviewListener).ifPresent { it.run() }
    }

    internal fun setOnCancelListener(onCancelReviewListener: Runnable) {
        this.onCancelReviewListener = onCancelReviewListener
    }

    internal fun setOnDeleteReviewListener(onDeleteReviewListener: Consumer<Review>) {
        this.onDeleteReviewListener = onDeleteReviewListener
    }

    companion object {
        private val STARS_TIP_KEYS = arrayOf("review.starsTip.one", "review.starsTip.two", "review.starsTip.three", "review.starsTip.four", "review.starsTip.five")
    }
}
