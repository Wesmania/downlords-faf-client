package com.faforever.client.stats

import com.faforever.client.domain.RatingHistoryDataPoint
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.remote.FafService
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.concurrent.CompletableFuture


@Lazy
@Service
class StatisticsService @Inject
constructor(private val fafService: FafService) {

    fun getRatingHistory(featuredMod: KnownFeaturedMod, playerId: Int): CompletableFuture<List<RatingHistoryDataPoint>> {
        return fafService.getRatingHistory(playerId, featuredMod)
    }
}
