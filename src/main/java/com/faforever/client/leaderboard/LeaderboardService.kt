package com.faforever.client.leaderboard

import com.faforever.client.game.KnownFeaturedMod
import java.util.concurrent.CompletableFuture

interface LeaderboardService {

    val ladder1v1Stats: CompletableFuture<List<RatingStat>>

    fun getEntryForPlayer(playerId: Int): CompletableFuture<LeaderboardEntry>

    fun getEntries(ratingType: KnownFeaturedMod): CompletableFuture<List<LeaderboardEntry>>

    companion object {
        val MINIMUM_GAMES_PLAYED_TO_BE_SHOWN = 10
    }
}
