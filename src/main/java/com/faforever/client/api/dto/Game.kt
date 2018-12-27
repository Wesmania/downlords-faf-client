package com.faforever.client.api.dto

import com.faforever.client.remote.domain.VictoryCondition
import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("game")
class Game {
    @Id
    private val id: String? = null
    private val name: String? = null
    private val startTime: OffsetDateTime? = null
    private val endTime: OffsetDateTime? = null
    private val validity: Validity? = null
    private val victoryCondition: VictoryCondition? = null

    @Relationship("reviews")
    private val reviews: List<GameReview>? = null

    @Relationship("playerStats")
    private val playerStats: List<GamePlayerStats>? = null

    @Relationship("host")
    private val host: Player? = null

    @Relationship("featuredMod")
    private val featuredMod: FeaturedMod? = null

    @Relationship("mapVersion")
    private val mapVersion: MapVersion? = null

    @Relationship("reviewsSummary")
    private val gameReviewsSummary: GameReviewsSummary? = null
}
