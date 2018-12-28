package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("gameReviewsSummary")
class GameReviewsSummary : ReviewsSummary() {

    @Relationship("game")
    var game: Game? = null
}
