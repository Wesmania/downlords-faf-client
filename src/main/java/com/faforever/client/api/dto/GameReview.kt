package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("gameReview")
class GameReview : Review() {

    @Relationship("game")
    var game: Game? = null
}
