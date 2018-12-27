package com.faforever.client.leaderboard

import com.faforever.client.FafClientApplication
import com.faforever.client.game.KnownFeaturedMod
import com.faforever.client.remote.FafService
import com.faforever.client.util.RatingUtil
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

import javax.inject.Inject
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors
import java.util.stream.Stream


@Lazy
@Service
@Profile("!" + FafClientApplication.PROFILE_OFFLINE)
class LeaderboardServiceImpl @Inject
constructor(private val fafService: FafService) : LeaderboardService {

    override val ladder1v1Stats: CompletableFuture<List<RatingStat>>
        get() = fafService.ladder1v1Leaderboard.thenApply(Function<List<LeaderboardEntry>, List<RatingStat>> { this.toRatingStats(it) })

    private fun toRatingStats(entries: List<LeaderboardEntry>): List<RatingStat> {
        val totalCount = countByRating(entries.stream())
        val countWithoutFewGames = countByRating(entries.stream()
                .filter { entry -> entry.gamesPlayedProperty().get() >= LeaderboardService.MINIMUM_GAMES_PLAYED_TO_BE_SHOWN })

        return totalCount.entries.stream()
                .map { entry ->
                    RatingStat(
                            entry.key,
                            entry.value.toInt(),
                            (countWithoutFewGames as java.util.Map<Int, Long>).getOrDefault(entry.key, 0L).toInt())
                }
                .collect<List<RatingStat>, Any>(Collectors.toList())
    }

    private fun countByRating(entries: Stream<LeaderboardEntry>): Map<Int, Long> {
        return entries.collect<Map<Int, Long>, Any>(Collectors.groupingBy<LeaderboardEntry, Int, Any, Long>({ leaderboardEntry -> RatingUtil.roundRatingToNextLowest100(leaderboardEntry.getRating()) }, Collectors.counting()))
    }

    override fun getEntryForPlayer(playerId: Int): CompletableFuture<LeaderboardEntry> {
        return fafService.getLadder1v1EntryForPlayer(playerId)
    }

    override fun getEntries(ratingType: KnownFeaturedMod): CompletableFuture<List<LeaderboardEntry>> {
        when (ratingType) {
            KnownFeaturedMod.FAF -> return fafService.globalLeaderboard
            KnownFeaturedMod.LADDER_1V1 -> return fafService.ladder1v1Leaderboard
            else -> throw IllegalArgumentException("Not supported: $ratingType")
        }
    }
}
