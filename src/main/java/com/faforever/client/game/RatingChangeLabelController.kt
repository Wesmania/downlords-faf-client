package com.faforever.client.game

import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.faforever.client.replay.Replay.PlayerStats
import com.faforever.client.util.RatingUtil
import javafx.css.PseudoClass
import javafx.scene.Node
import javafx.scene.control.Label
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class RatingChangeLabelController @Inject
constructor(private val i18n: I18n) : Controller<Node> {
    var ratingChangLabelRoot: Label? = null

    override val root: Node?
        get() = ratingChangLabelRoot

    override fun initialize() {
        ratingChangLabelRoot!!.isVisible = false
    }

    fun setRatingChange(playerStats: PlayerStats) {
        if (playerStats.getAfterMean() == null || playerStats.getAfterDeviation() == null) {
            return
        }
        val newRating = RatingUtil.getRating(playerStats.getAfterMean(), playerStats.getAfterDeviation())
        val oldRating = RatingUtil.getRating(playerStats.getBeforeMean(), playerStats.getBeforeDeviation())

        val ratingChange = newRating - oldRating
        ratingChangLabelRoot!!.text = i18n.numberWithSign(ratingChange)
        ratingChangLabelRoot!!.pseudoClassStateChanged(if (ratingChange < 0) NEGATIVE else POSITIVE, true)

        ratingChangLabelRoot!!.isVisible = true
    }

    companion object {
        private val POSITIVE = PseudoClass.getPseudoClass("positive")
        private val NEGATIVE = PseudoClass.getPseudoClass("negative")
    }
}
