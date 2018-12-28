package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("ladder1v1Map")
data class Ladder1v1Map(@Id var id: String? = null) {
    @Relationship("mapVersion")
    var mapVersion: MapVersion? = null
}