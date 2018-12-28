package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Type

import java.time.OffsetDateTime

@Type("nameRecord")
data class NameRecord(@Id var id: String? = null) {
    var changeTime: OffsetDateTime? = null
    var name: String? = null
}
