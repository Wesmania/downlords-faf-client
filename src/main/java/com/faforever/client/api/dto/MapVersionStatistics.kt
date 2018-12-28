package com.faforever.client.api.dto

import com.github.jasminb.jsonapi.annotations.Id
import com.github.jasminb.jsonapi.annotations.Relationship
import com.github.jasminb.jsonapi.annotations.Type

@Type("mapVersionStatistics")
data class MapVersionStatistics(@Id var id: String? = null) {

    var downloads: Int = 0
    var draws: Int = 0
    var plays: Int = 0

    @Relationship("mapVersion")
    var mapVersion: MapVersion? = null
}
