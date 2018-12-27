package com.faforever.client.rating

import com.faforever.client.replay.Replay

interface RatingService {
    /**
     * Calculates the game quality of the specified replay based in the "before" ratings its player stats.
     */
    fun calculateQuality(replay: Replay): Double
}
