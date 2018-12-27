package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type
import lombok.EqualsAndHashCode
import lombok.Getter
import lombok.Setter

import java.time.OffsetDateTime

@Getter
@Setter
@EqualsAndHashCode(of = "id")
@Type("nameRecord")
class NameRecord {
    private val changeTime: OffsetDateTime? = null
    private val name: String? = null
    @Id
    private val id: String? = null
}
