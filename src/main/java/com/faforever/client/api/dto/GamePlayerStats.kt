package com.faforever.client.api.dto

import com.faforever.client.game.Faction
import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("gamePlayerStats")
data class GamePlayerStats(@Id var id: String? = null) {
    var ai: Boolean = false
    var faction: Faction? = null
    var color: Byte = 0
    var team: Byte = 0
    var startSpot: Byte = 0
    var beforeMean: Float? = null
    var beforeDeviation: Float? = null
    var afterMean: Float? = null
    var afterDeviation: Float? = null
    var score: Byte = 0
    var scoreTime: OffsetDateTime? = null

    @Relationship("replay")
    var replay: Game? = null

    @Relationship("player")
    var player: Player? = null
}
