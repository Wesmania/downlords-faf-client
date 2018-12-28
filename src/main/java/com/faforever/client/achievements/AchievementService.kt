package com.faforever.client.achievements

import com.faforever.client.api.dto.AchievementDefinition
import com.faforever.client.api.dto.PlayerAchievement
import com.faforever.client.config.CacheNames
import com.faforever.client.player.PlayerService
import com.faforever.client.remote.AssetService
import com.faforever.client.remote.FafService
import com.faforever.client.remote.UpdatedAchievementsMessage
import com.google.common.annotations.VisibleForTesting
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.scene.image.Image
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.inject.Inject
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture

import com.github.nocatch.NoCatch.noCatch


@Lazy
@Service
class AchievementService @Inject
constructor(private val fafService: FafService, private val playerService: PlayerService, private val assetService: AssetService) {
    @VisibleForTesting
    internal val playerAchievements: ObservableList<PlayerAchievement> = FXCollections.observableArrayList()
    private val readOnlyPlayerAchievements: ObservableList<PlayerAchievement> = FXCollections.unmodifiableObservableList(playerAchievements)
    val achievementDefinitions: CompletableFuture<List<AchievementDefinition>>
        get() = fafService.achievementDefinitions

    fun getPlayerAchievements(playerId: Int): CompletableFuture<List<PlayerAchievement>> {
        val currentPlayerId = playerService.currentPlayer.orElseThrow { IllegalStateException("Player has to be set") }.id
        return if (currentPlayerId == playerId) {
            if (readOnlyPlayerAchievements.isEmpty()) {

                reloadAchievements()
            } else CompletableFuture.completedFuture(readOnlyPlayerAchievements as List<PlayerAchievement>) /* FIXME - why do we need the typecast? */
        } else fafService.getPlayerAchievements(playerId)

    }


    fun getAchievementDefinition(achievementId: String): CompletableFuture<AchievementDefinition> {
        return fafService.getAchievementDefinition(achievementId)
    }


    @Cacheable(CacheNames.ACHIEVEMENT_IMAGES)
    fun getImage(achievementDefinition: AchievementDefinition, achievementState: AchievementState): Image? {
        val url: URL
        when (achievementState) {
            AchievementService.AchievementState.REVEALED -> url = noCatch<URL> { URL(achievementDefinition.revealedIconUrl) }
            AchievementService.AchievementState.UNLOCKED -> url = noCatch<URL> { URL(achievementDefinition.unlockedIconUrl) }
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
        return assetService.loadAndCacheImage(url, Paths.get("achievements").resolve(achievementState.name.toLowerCase()),
                null, ACHIEVEMENT_IMAGE_SIZE, ACHIEVEMENT_IMAGE_SIZE)
    }

    private fun reloadAchievements(): CompletableFuture<List<PlayerAchievement>> {
        val achievementsLoadedFuture = CompletableFuture<List<PlayerAchievement>>()
        val playerId = playerService.currentPlayer.orElseThrow { IllegalStateException("Player has to be set") }.id
        fafService.getPlayerAchievements(playerId).thenAccept { achievements ->
            playerAchievements.setAll(achievements)
            achievementsLoadedFuture.complete(readOnlyPlayerAchievements)
        }
        return achievementsLoadedFuture
    }

    @PostConstruct
    internal fun postConstruct() {
        fafService.addOnMessageListener(UpdatedAchievementsMessage::class.java) { _ -> reloadAchievements() }
    }

    enum class AchievementState {
        HIDDEN, REVEALED, UNLOCKED
    }

    companion object {

        private val ACHIEVEMENT_IMAGE_SIZE = 128
    }

}
