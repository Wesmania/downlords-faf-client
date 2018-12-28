package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("playerEvent")
data class PlayerEvent(@Id var id: String? = null) {
    var currentCount: Int = 0

    @Relationship("event")
    var event: Event? = null
}
