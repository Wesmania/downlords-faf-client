package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("event")
class Event {

    @Id
    private val id: String? = null
    private val name: String? = null
    private val imageUrl: String? = null
    private val type: Type? = null

    enum class Type {
        NUMERIC, TIME
    }
}
