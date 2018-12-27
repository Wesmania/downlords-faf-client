package com.faforever.client.leaderboard

import lombok.Data

@Data
class RatingStat {
    private val rating: Int = 0
    private val totalCount: Int = 0
    private val countWithEnoughGamesPlayed: Int = 0
}
