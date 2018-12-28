package com.faforever.client.achievements

import com.faforever.client.achievements.AchievementService.AchievementState
import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.api.dto.AchievementType
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.fx.Controller
import com.faforever.client.i18n.I18n
import com.google.common.base.MoreObjects
import javafx.application.Platform
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.control.ProgressBar
import javafx.scene.effect.ColorAdjust
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import javax.inject.Inject

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
// TODO this class should not use API objects
class AchievementItemController @Inject
constructor(private val i18n: I18n, private val achievementService: AchievementService) : Controller<Node> {
    lateinit var achievementItemRoot: GridPane
    lateinit var nameLabel: Label
    lateinit var descriptionLabel: Label
    lateinit var pointsLabel: Label
    lateinit var progressBar: ProgressBar
    lateinit var progressLabel: Label
    lateinit var imageView: ImageView
    private var achievementDefinition: AchievementDefinition? = null

    override val root: Node
        get() = achievementItemRoot

    override fun initialize() {
        progressBar.managedProperty().bind(progressBar.visibleProperty())
        progressLabel.managedProperty().bind(progressLabel.visibleProperty())
    }

    fun setAchievementDefinition(achievementDefinition: AchievementDefinition) {
        this.achievementDefinition = achievementDefinition

        nameLabel.text = achievementDefinition.name
        descriptionLabel.text = achievementDefinition.description
        pointsLabel.text = i18n.number(achievementDefinition.experiencePoints)
        imageView.image = achievementService.getImage(achievementDefinition, AchievementService.AchievementState.REVEALED)
        progressLabel.text = i18n.get("achievement.stepsFormat", 0, achievementDefinition.totalSteps as Any)
        progressBar.progress = 0.0

        if (AchievementType.STANDARD === achievementDefinition.type) {
            progressBar.isVisible = false
            progressLabel.isVisible = false
        }

        val colorAdjust = ColorAdjust()
        colorAdjust.saturation = -1.0
        imageView.effect = colorAdjust
        imageView.opacity = 0.5
    }

    fun setPlayerAchievement(playerAchievement: PlayerAchievement) {
        var definition = achievementDefinition ?:
            throw IllegalStateException("achievementDefinition needs to be set first")
        if (definition.id != playerAchievement.achievement?.id) {
            throw IllegalStateException("Achievement ID does not match")
        }

        if (AchievementState.UNLOCKED == AchievementState.valueOf(playerAchievement.state.name)) {
            imageView.image = achievementService.getImage(definition, AchievementState.UNLOCKED)
            imageView.opacity = 1.0
            imageView.effect = null
        }

        if (AchievementType.INCREMENTAL === definition.type) {
            val currentSteps = MoreObjects.firstNonNull(playerAchievement.currentSteps, 0)
            val totalSteps = definition.totalSteps
            progressBar.progress = currentSteps.toDouble() / totalSteps!!
            Platform.runLater { progressLabel.text = i18n.get("achievement.stepsFormat", currentSteps, totalSteps) }
        }
    }
}
