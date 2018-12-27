package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@Type("gameReviewsSummary")
class GameReviewsSummary : ReviewsSummary() {

    @Relationship("game")
    private val game: Game? = null
}
