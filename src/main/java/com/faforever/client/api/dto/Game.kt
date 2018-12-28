package com.faforever.client.api.dto

import com.faforever.client.remote.domain.VictoryCondition
import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("game")
data class Game(@Id var id: String? = null) {
    var name: String? = null
    var startTime: OffsetDateTime? = null
    var endTime: OffsetDateTime? = null
    var validity: Validity? = null
    var victoryCondition: VictoryCondition? = null

    @Relationship("reviews")
    var reviews: List<GameReview>? = null

    @Relationship("playerStats")
    var playerStats: List<GamePlayerStats>? = null

    @Relationship("host")
    var host: Player? = null

    @Relationship("featuredMod")
    var featuredMod: FeaturedMod? = null

    @Relationship("mapVersion")
    var mapVersion: MapVersion? = null

    @Relationship("reviewsSummary")
    var gameReviewsSummary: GameReviewsSummary? = null
}
