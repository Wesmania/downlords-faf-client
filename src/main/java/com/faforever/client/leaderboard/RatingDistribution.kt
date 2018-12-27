package com.faforever.client.leaderboard

class RatingDistribution(var maxRating: Int) : Comparable<RatingDistribution> {
    var players: Int = 0

    fun incrementPlayers() {
        players++
    }

    override fun compareTo(o: RatingDistribution): Int {
        return Integer.compare(maxRating, o.maxRating)
    }
}
