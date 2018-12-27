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
@Type("playerEvent")
class PlayerEvent {

    @Id
    private val id: String? = null
    private val currentCount: Int = 0

    @Relationship("event")
    private val event: Event? = null
}
