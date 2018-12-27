package com.faforever.client.api.dto

import com.faforever.client.game.Faction
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
@Type("gamePlayerStats")
class GamePlayerStats {
    @Id
    private val id: String? = null
    private val ai: Boolean = false
    private val faction: Faction? = null
    private val color: Byte = 0
    private val team: Byte = 0
    private val startSpot: Byte = 0
    private val beforeMean: Float? = null
    private val beforeDeviation: Float? = null
    private val afterMean: Float? = null
    private val afterDeviation: Float? = null
    private val score: Byte = 0
    @Nullable
    private val scoreTime: OffsetDateTime? = null

    @Relationship("replay")
    private val replay: Game? = null

    @Relationship("player")
    private val player: Player? = null
}
