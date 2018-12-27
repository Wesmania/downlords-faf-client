package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("player")
class Player {
    @Id
    private val id: String? = null
    private val login: String? = null
    private val userAgent: String? = null

    @Relationship("globalRating")
    private val globalRating: GlobalRating? = null

    @Relationship("ladder1v1Rating")
    private val ladder1v1Rating: Ladder1v1Rating? = null

    @Relationship("names")
    private val names: List<NameRecord>? = null
}
