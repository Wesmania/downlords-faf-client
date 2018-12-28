package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

import java.time.Duration

@Type("coopResult")
data class CoopResult(@Id var id: String? = null) {
    var duration: Duration? = null
    var secondaryObjectives: Boolean = false
    var playerCount: Int = 0
    /** This field is not provided by the API but must be enriched instead.  */
    var ranking: Int = 0

    @Relationship("game")
    var game: Game? = null
}
