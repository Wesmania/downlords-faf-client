package com.faforever.client.achievements

import com.faforever.client.achievements.AchievementService.AchievementState
import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.audio.AudioService
import com.faforever.client.i18n.I18n
import com.faforever.client.notification.NotificationService
import com.faforever.client.notification.TransientNotification
import com.faforever.client.remote.FafService
import com.faforever.client.remote.UpdatedAchievement
import com.faforever.client.remote.UpdatedAchievementsMessage
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.lang.invoke.MethodHandles

@Lazy
@Component
class AchievementUnlockedNotifier @Inject
constructor(private val notificationService: NotificationService, private val i18n: I18n, private val achievementService: AchievementService, private val fafService: FafService, private val audioService: AudioService) {

    private var lastSoundPlayed: Long = 0

    @PostConstruct
    internal fun postConstruct() {
        fafService.addOnMessageListener(UpdatedAchievementsMessage::class.java, { this.onUpdatedAchievementsMessage(it) })
    }

    private fun onUpdatedAchievementsMessage(message: UpdatedAchievementsMessage) {
        message.updatedAchievements.stream()
                .filter { it.getNewlyUnlocked() }
                .forEachOrdered { updatedAchievement ->
                    achievementService.getAchievementDefinition(updatedAchievement.achievementId)
                            .thenAccept { this.notifyAboutUnlockedAchievement(it) }
                            .exceptionally { throwable ->
                                logger.warn("Could not valueOf achievement definition for achievement: {}", updatedAchievement.achievementId, throwable)
                                null
                            }
                }
    }

    private fun notifyAboutUnlockedAchievement(achievementDefinition: AchievementDefinition) {
        if (lastSoundPlayed < System.currentTimeMillis() - 1000) {
            audioService.playAchievementUnlockedSound()
            lastSoundPlayed = System.currentTimeMillis()
        }
        notificationService.addNotification(TransientNotification(
                i18n.get("achievement.unlockedTitle"),
                achievementDefinition.getName(),
                achievementService.getImage(achievementDefinition, AchievementState.UNLOCKED)
        )
        )
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())
    }
}
