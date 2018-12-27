package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.time.Duration

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("coopResult")
class CoopResult {
    @Id
    private val id: String? = null
    private val duration: Duration? = null
    private val secondaryObjectives: Boolean = false
    private val playerCount: Int = 0
    /** This field is not provided by the API but must be enriched instead.  */
    private val ranking: Int = 0

    @Relationship("game")
    private val game: Game? = null
}
