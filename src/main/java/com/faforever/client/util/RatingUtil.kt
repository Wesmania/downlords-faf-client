package com.faforever.client.util

import com.faforever.client.domain.RatingHistoryDataPoint
import com.faforever.client.player.Player

object RatingUtil {

    fun roundRatingToNextLowest100(rating: Double): Int {
        val ratingToBeRounded = if (rating < 0) rating - 100 else rating
        return (ratingToBeRounded / 100).toInt() * 100
    }

    fun getRoundedGlobalRating(player: Player): Int {
        return getRoundedRating(getGlobalRating(player))
    }

    fun getRoundedRating(rating: Int): Int {
        return (rating + 50) / 100 * 100
    }

    fun getGlobalRating(playerInfo: Player): Int {
        return getRating(playerInfo.globalRatingMean.toDouble(), playerInfo.globalRatingDeviation.toDouble())
    }

    fun getRating(ratingMean: Double, ratingDeviation: Double): Int {
        return (ratingMean - 3f * ratingDeviation).toInt()
    }

    fun getLeaderboardRating(player: Player): Int {
        return getRating(player.leaderboardRatingMean.toDouble(), player.leaderboardRatingDeviation.toDouble())
    }

    fun getGlobalRating(player: com.faforever.client.remote.domain.Player): Int {
        return getRating(player.globalRating!![0].toDouble(), player.globalRating!![1].toDouble())
    }

    fun getLeaderboardRating(player: com.faforever.client.remote.domain.Player): Int {
        return getRating(player.ladderRating!![0].toDouble(), player.ladderRating!![1].toDouble())
    }

    fun getRating(datapoint: RatingHistoryDataPoint): Int {
        return getRating(datapoint.getMean(), datapoint.getDeviation())
    }

    fun getRating(rating: Rating): Int {
        return getRating(rating.getMean(), rating.getDeviation())
    }
}// Utility class
