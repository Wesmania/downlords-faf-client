package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("player")
data class Player(@Id var id: String? = null) {
    var login: String? = null
    var userAgent: String? = null

    @Relationship("globalRating")
    var globalRating: GlobalRating? = null

    @Relationship("ladder1v1Rating")
    var ladder1v1Rating: Ladder1v1Rating? = null

    @Relationship("names")
    var names: List<NameRecord>? = null
}
